package com.bounswe2026group8.emergencyhub.mesh.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID

/**
 * BLE-based mesh transport for real device communication.
 *
 * Architecture: Every device runs a GATT server. When device A discovers
 * device B, A connects as a GATT client to B's server (to send data to B).
 * B's server sees the incoming connection, reads A's address, and connects
 * back as a GATT client to A's server (so B can send data to A).
 *
 * Data is chunked into MTU-safe packets with a simple reassembly protocol:
 *   [4 bytes total length][payload chunks...]
 */
@SuppressLint("MissingPermission")
class BleTransport(
    private val context: Context,
    private val localDeviceId: String,
    private val displayName: String?
) : MeshTransport {

    companion object {
        private const val TAG = "BleTransport"

        val SERVICE_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        val WRITE_CHAR_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567891")
        val DEVICE_ID_CHAR_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567893")

        private const val CHUNK_SIZE = 20
        private const val WRITE_TIMEOUT_MS = 2000L

        private const val SCAN_DURATION_MS = 15_000L
        private const val SCAN_PAUSE_MS = 45_000L
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    // SupervisorJob: don't let a thrown exception in one peer's GATT coroutine
    // (e.g., a write timing out in an unexpected way) cancel the whole scope and
    // silently kill the scan loop and every other peer's sendPayload.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scanJob: Job? = null
    private var gattServer: BluetoothGattServer? = null

    private var deviceFoundCallback: ((DeviceInfo) -> Unit)? = null
    private var payloadReceivedCallback: ((String, ByteArray) -> Unit)? = null
    private var deviceDisconnectedCallback: ((String) -> Unit)? = null

    // Connected GATT clients (we write TO these to send data to the peer's server)
    private val connectedGatts = mutableMapOf<String, BluetoothGatt>()
    private val gattWriteLocks = mutableMapOf<String, Mutex>()

    // Serializes the actual radio write across ALL peers. Android's BLE stack only
    // permits one outstanding GATT operation at a time per radio, and `writeComplete`
    // below is a single shared field — without this, writes to peer B and peer C race
    // and each receives the other's onCharacteristicWrite ack, scrambling chunked
    // payloads. The per-peer mutex above only protects ordering within one peer's
    // payload (header + chunks); this one protects the radio itself.
    private val writeMutex = Mutex()

    // Write completion signal — set when onCharacteristicWrite fires. Only ever
    // touched while holding writeMutex, so the single field is safe.
    private var writeComplete: CompletableDeferred<Boolean>? = null

    // Map BLE addresses to mesh device IDs (bidirectional)
    private val addressToDeviceId = mutableMapOf<String, String>()
    private val deviceIdToAddress = mutableMapOf<String, String>()

    // Track devices we're already connecting to / connected with
    private val connectingAddresses = mutableSetOf<String>()
    private val discoveredAddresses = mutableSetOf<String>()

    // Reassembly buffer for incoming chunked messages (synchronized)
    private val reassemblyLock = Any()
    private val incomingBuffers = mutableMapOf<String, ByteArrayOutputStream>()
    private val expectedLengths = mutableMapOf<String, Int>()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "BLE advertising started")
        }
        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE advertising failed: error $errorCode")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val address = result.device.address
            if (address in discoveredAddresses) return
            discoveredAddresses.add(address)
            Log.d(TAG, "Found BLE device: $address, RSSI: ${result.rssi}")
            connectToDevice(result.device)
        }
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE scan failed: error $errorCode")
        }
    }

    // GATT server callback — receives data written by peers
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "GATT server: peer connected from ${device.address}")
                val address = device.address
                // Always schedule a client connect-back (deduped by connectingAddresses).
                //
                // The earlier "skip if addressToDeviceId[address] != null" condition
                // assumed any cached mapping was still valid — but if the peer
                // reinstalled or restarted with a new device ID, BLE supervision
                // timeout may not have detected the old gatt's death yet, leaving us
                // with a stale mapping. By always connecting back, we re-read the
                // peer's DEVICE_ID characteristic; if it changed, onCharacteristicRead
                // prunes the stale entry. If it's the same, the duplicate-detection
                // path in onCharacteristicRead closes the new gatt as a no-op.
                if (address !in connectingAddresses) {
                    scope.launch {
                        delay(500) // Small delay to let the peer's server stabilize
                        connectToDevice(device)
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "GATT server: peer client disconnected: ${device.address}")
                // Server-side disconnect just means the peer's GATT CLIENT closed its
                // connection to our server. Our OUTBOUND client (in connectedGatts) may
                // still be alive — do NOT call cleanupPeer here, or we'll wipe the live
                // outbound's state. The client-side gattCallback fires its own DISCONNECTED
                // event when our outbound truly dies. Just clear in-flight reassembly
                // buffers since any partial chunk this peer was sending is now lost.
                synchronized(reassemblyLock) {
                    incomingBuffers.remove(device.address)
                    expectedLengths.remove(device.address)
                }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
            if (characteristic.uuid == WRITE_CHAR_UUID && value != null) {
                handleIncomingChunk(device.address, value)
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == DEVICE_ID_CHAR_UUID) {
                val idBytes = "$localDeviceId|${displayName.orEmpty()}".toByteArray()
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, idBytes)
            }
        }
    }

    override fun onDeviceFound(callback: (DeviceInfo) -> Unit) { deviceFoundCallback = callback }
    override fun onPayloadReceived(callback: (deviceId: String, data: ByteArray) -> Unit) { payloadReceivedCallback = callback }
    override fun onDeviceDisconnected(callback: (deviceId: String) -> Unit) { deviceDisconnectedCallback = callback }

    override fun getConnectedPeerIds(): Set<String> = synchronized(connectedGatts) {
        connectedGatts.keys.toSet()
    }

    override fun startAdvertising() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth not available or not enabled")
            return
        }

        startGattServer()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        bluetoothAdapter.bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
            ?: Log.e(TAG, "BLE advertiser not available")

        Log.d(TAG, "Started advertising as $localDeviceId")
    }

    override fun startDiscovery() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth not available or not enabled")
            return
        }

        startGattServer()

        scanJob = scope.launch {
            val scanner = bluetoothAdapter.bluetoothLeScanner ?: run {
                Log.e(TAG, "BLE scanner not available")
                return@launch
            }

            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(SERVICE_UUID))
                .build()

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            while (true) {
                Log.d(TAG, "Starting BLE scan cycle")
                scanner.startScan(listOf(filter), settings, scanCallback)
                delay(SCAN_DURATION_MS)
                scanner.stopScan(scanCallback)
                Log.d(TAG, "Scan cycle paused")
                delay(SCAN_PAUSE_MS)
            }
        }

        Log.d(TAG, "Started discovery as $localDeviceId")
    }

    override fun stop() {
        scanJob?.cancel()
        bluetoothAdapter?.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        try { bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback) } catch (_: Exception) { }

        synchronized(connectedGatts) {
            connectedGatts.values.forEach { it.close() }
            connectedGatts.clear()
            gattWriteLocks.clear()
        }

        gattServer?.close()
        gattServer = null

        connectingAddresses.clear()
        discoveredAddresses.clear()
        addressToDeviceId.clear()
        deviceIdToAddress.clear()
        synchronized(reassemblyLock) {
            incomingBuffers.clear()
            expectedLengths.clear()
        }

        Log.d(TAG, "Stopped")
    }

    override fun sendPayload(deviceId: String, data: ByteArray) {
        scope.launch {
            try {
                val gatt: BluetoothGatt
                val mutex: Mutex
                synchronized(connectedGatts) {
                    gatt = connectedGatts[deviceId] ?: run {
                        Log.w(TAG, "No GATT connection to device $deviceId")
                        return@launch
                    }
                    mutex = gattWriteLocks.getOrPut(deviceId) { Mutex() }
                }

                val service = gatt.getService(SERVICE_UUID) ?: run {
                    Log.w(TAG, "Mesh service not found on $deviceId")
                    return@launch
                }
                val writeChar = service.getCharacteristic(WRITE_CHAR_UUID) ?: run {
                    Log.w(TAG, "Write characteristic not found on $deviceId")
                    return@launch
                }

                mutex.withLock {
                    // Send length header first (4 bytes)
                    val header = ByteArrayOutputStream()
                    DataOutputStream(header).writeInt(data.size)
                    writeAndWait(gatt, writeChar, header.toByteArray())

                    // Send payload in chunks
                    var offset = 0
                    while (offset < data.size) {
                        val end = minOf(offset + CHUNK_SIZE, data.size)
                        val chunk = data.copyOfRange(offset, end)
                        writeAndWait(gatt, writeChar, chunk)
                        offset = end
                    }
                }

                Log.d(TAG, "Sent ${data.size} bytes to $deviceId")
            } catch (e: Exception) {
                Log.e(TAG, "Send failed to $deviceId", e)
            }
        }
    }

    // ---- Private helpers ----

    private fun startGattServer() {
        if (gattServer != null) return

        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)

        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val writeChar = BluetoothGattCharacteristic(
            WRITE_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        val deviceIdChar = BluetoothGattCharacteristic(
            DEVICE_ID_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        service.addCharacteristic(writeChar)
        service.addCharacteristic(deviceIdChar)
        gattServer?.addService(service)

        Log.d(TAG, "GATT server started")
    }

    /**
     * Connect to a peer's GATT server as a client.
     * Reads their device ID, then registers the GATT connection so we can send payloads.
     */
    private fun connectToDevice(device: BluetoothDevice) {
        val address = device.address
        synchronized(connectingAddresses) {
            if (address in connectingAddresses) return
            connectingAddresses.add(address)
        }

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "GATT client connected to ${device.address}")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "GATT client disconnected from ${device.address}")
                    val meshId = addressToDeviceId[device.address]
                    // Only clean up if THIS gatt is the one we currently have stored.
                    // When a duplicate connection (e.g., a connect-back race where both
                    // sides initiated client connections) is closed, the closing gatt is
                    // NOT the live one — cleaning up would wipe the live connection's
                    // state and leave us thinking we're disconnected.
                    val storedGatt = if (meshId != null) {
                        synchronized(connectedGatts) { connectedGatts[meshId] }
                    } else null
                    if (storedGatt === gatt) {
                        cleanupPeer(device.address, meshId)
                    } else {
                        synchronized(connectingAddresses) {
                            connectingAddresses.remove(device.address)
                        }
                    }
                    gatt.close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "Service discovery failed: $status")
                    gatt.close()
                    synchronized(connectingAddresses) { connectingAddresses.remove(address) }
                    return
                }

                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    Log.w(TAG, "Mesh service not found on ${device.address}")
                    gatt.close()
                    synchronized(connectingAddresses) { connectingAddresses.remove(address) }
                    return
                }

                val deviceIdChar = service.getCharacteristic(DEVICE_ID_CHAR_UUID)
                if (deviceIdChar != null) {
                    gatt.readCharacteristic(deviceIdChar)
                } else {
                    Log.w(TAG, "Device ID characteristic not found")
                    gatt.close()
                    synchronized(connectingAddresses) { connectingAddresses.remove(address) }
                }
            }

            @Suppress("DEPRECATION")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status != BluetoothGatt.GATT_SUCCESS || characteristic.uuid != DEVICE_ID_CHAR_UUID) {
                    gatt.close()
                    synchronized(connectingAddresses) { connectingAddresses.remove(address) }
                    return
                }

                val idString = String(characteristic.value)
                val parts = idString.split("|", limit = 2)
                val peerId = parts[0]
                val peerName = parts.getOrNull(1)?.ifEmpty { null }

                if (peerId == localDeviceId) {
                    Log.d(TAG, "Discovered ourselves, ignoring")
                    gatt.close()
                    synchronized(connectingAddresses) { connectingAddresses.remove(address) }
                    return
                }

                // Check if we already have a connection to this peer
                synchronized(connectedGatts) {
                    if (peerId in connectedGatts) {
                        Log.d(TAG, "Already connected to $peerId, closing duplicate")
                        gatt.close()
                        synchronized(connectingAddresses) { connectingAddresses.remove(address) }
                        return
                    }
                }

                // Re-identification: if this BLE address used to map to a different
                // mesh device id, the peer must have reinstalled / cleared data and
                // restarted with a new id. The old id's gatt is dead even if BLE
                // supervision hasn't reported it yet, so prune the stale entry now —
                // otherwise the peer list shows the old id forever.
                val priorMeshId = addressToDeviceId[device.address]
                if (priorMeshId != null && priorMeshId != peerId) {
                    Log.d(
                        TAG,
                        "Address ${device.address} re-identified: $priorMeshId → $peerId; pruning stale"
                    )
                    val staleGatt = synchronized(connectedGatts) {
                        val g = connectedGatts.remove(priorMeshId)
                        gattWriteLocks.remove(priorMeshId)
                        g
                    }
                    deviceIdToAddress.remove(priorMeshId)
                    runCatching { staleGatt?.close() }
                    deviceDisconnectedCallback?.invoke(priorMeshId)
                }

                Log.d(TAG, "Identified peer: $peerId ($peerName) at ${device.address}")

                addressToDeviceId[device.address] = peerId
                deviceIdToAddress[peerId] = device.address

                synchronized(connectedGatts) {
                    connectedGatts[peerId] = gatt
                    gattWriteLocks[peerId] = Mutex()
                }

                deviceFoundCallback?.invoke(DeviceInfo(peerId, peerName))
            }

            @Suppress("DEPRECATION")
            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                writeComplete?.complete(status == BluetoothGatt.GATT_SUCCESS)
            }
        }

        device.connectGatt(context, false, gattCallback)
    }

    private fun cleanupPeer(address: String, meshId: String?) {
        if (meshId != null) {
            synchronized(connectedGatts) {
                connectedGatts.remove(meshId)
                gattWriteLocks.remove(meshId)
            }
            deviceIdToAddress.remove(meshId)
            deviceDisconnectedCallback?.invoke(meshId)
        }
        addressToDeviceId.remove(address)
        // Allow this peer's address to be rediscovered next time the scanner sees it,
        // so a peer that drops out of range and comes back will reconnect.
        discoveredAddresses.remove(address)
        synchronized(connectingAddresses) { connectingAddresses.remove(address) }
        synchronized(reassemblyLock) {
            incomingBuffers.remove(address)
            expectedLengths.remove(address)
        }
    }

    /**
     * Handle an incoming chunk of data. Reassembles chunked payloads.
     * Thread-safe via reassemblyLock.
     */
    private fun handleIncomingChunk(address: String, chunk: ByteArray) {
        val meshId = addressToDeviceId[address] ?: address

        synchronized(reassemblyLock) {
            if (address !in expectedLengths) {
                if (chunk.size >= 4) {
                    val length = DataInputStream(ByteArrayInputStream(chunk)).readInt()
                    expectedLengths[address] = length
                    incomingBuffers[address] = ByteArrayOutputStream()
                    if (chunk.size > 4) {
                        incomingBuffers[address]?.write(chunk, 4, chunk.size - 4)
                    }
                }
            } else {
                incomingBuffers[address]?.write(chunk)
            }

            val expected = expectedLengths[address] ?: return
            val buffer = incomingBuffers[address] ?: return

            if (buffer.size() >= expected) {
                val payload = buffer.toByteArray().copyOf(expected)
                Log.d(TAG, "Received complete payload: $expected bytes from $meshId")

                expectedLengths.remove(address)
                incomingBuffers.remove(address)

                // Invoke callback outside the lock
                scope.launch { payloadReceivedCallback?.invoke(meshId, payload) }
            }
        }
    }

    /**
     * Write a chunk to a GATT characteristic and wait for the write callback.
     * Falls back to a delay if the callback doesn't fire within the timeout.
     */
    private suspend fun writeAndWait(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        writeMutex.withLock {
            val deferred = CompletableDeferred<Boolean>()
            writeComplete = deferred
            @Suppress("DEPRECATION")
            characteristic.value = value
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)

            val result = withTimeoutOrNull(WRITE_TIMEOUT_MS) { deferred.await() }
            if (result == null) {
                Log.w(TAG, "Write timeout, continuing anyway")
            }
        }
    }
}
