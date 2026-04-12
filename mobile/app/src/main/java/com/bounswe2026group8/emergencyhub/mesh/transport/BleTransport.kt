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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID

/**
 * BLE-based mesh transport for real device communication.
 *
 * Uses BLE for both discovery AND data transfer via GATT.
 * Each device runs both a GATT server (to receive) and acts as a GATT client (to send).
 *
 * Data is chunked into 20-byte MTU-safe packets with a simple reassembly protocol:
 *   [2 bytes total length][payload chunks...]
 *
 * For larger payloads, this will be swapped to Wi-Fi Direct in a future phase.
 */
@SuppressLint("MissingPermission") // Permissions are checked in MeshActivity before starting
class BleTransport(
    private val context: Context,
    private val localDeviceId: String,
    private val displayName: String?
) : MeshTransport {

    companion object {
        private const val TAG = "BleTransport"

        // Custom UUIDs for the mesh service
        val SERVICE_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        val WRITE_CHAR_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567891")
        val NOTIFY_CHAR_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567892")
        val DEVICE_ID_CHAR_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567893")

        // BLE chunk size (conservative for broad compatibility)
        private const val CHUNK_SIZE = 20

        // Scan duty cycle
        private const val SCAN_DURATION_MS = 15_000L
        private const val SCAN_PAUSE_MS = 45_000L
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val scope = CoroutineScope(Dispatchers.IO)
    private var scanJob: Job? = null
    private var gattServer: BluetoothGattServer? = null

    private var deviceFoundCallback: ((DeviceInfo) -> Unit)? = null
    private var payloadReceivedCallback: ((String, ByteArray) -> Unit)? = null
    private var deviceDisconnectedCallback: ((String) -> Unit)? = null

    // Connected GATT clients (we write TO these to send data)
    private val connectedGatts = mutableMapOf<String, BluetoothGatt>()
    private val gattWriteLocks = mutableMapOf<String, Mutex>()

    // Map BLE device addresses to mesh device IDs
    private val addressToDeviceId = mutableMapOf<String, String>()
    private val deviceIdToAddress = mutableMapOf<String, String>()

    // Track discovered devices to avoid duplicate callbacks
    private val discoveredAddresses = mutableSetOf<String>()

    // Reassembly buffer for incoming chunked messages
    private val incomingBuffers = mutableMapOf<String, ByteArrayOutputStream>()
    private val expectedLengths = mutableMapOf<String, Int>()

    // Advertise callback
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "BLE advertising started")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE advertising failed: error $errorCode")
        }
    }

    // Scan callback
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val address = result.device.address
            if (address in discoveredAddresses) return
            discoveredAddresses.add(address)

            Log.d(TAG, "Found BLE device: $address, RSSI: ${result.rssi}")
            // Connect as GATT client to read their device ID
            connectToDevice(result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE scan failed: error $errorCode")
        }
    }

    // GATT server callback (receives data from peers)
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                val meshId = addressToDeviceId[device.address]
                Log.d(TAG, "GATT server: device disconnected: ${device.address} ($meshId)")
                if (meshId != null) {
                    deviceDisconnectedCallback?.invoke(meshId)
                    synchronized(connectedGatts) {
                        connectedGatts.remove(meshId)
                        gattWriteLocks.remove(meshId)
                    }
                    addressToDeviceId.remove(device.address)
                    deviceIdToAddress.remove(meshId)
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
                val address = device.address
                handleIncomingChunk(address, value)
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

    override fun onDeviceFound(callback: (DeviceInfo) -> Unit) {
        deviceFoundCallback = callback
    }

    override fun onPayloadReceived(callback: (deviceId: String, data: ByteArray) -> Unit) {
        payloadReceivedCallback = callback
    }

    override fun onDeviceDisconnected(callback: (deviceId: String) -> Unit) {
        deviceDisconnectedCallback = callback
    }

    override fun getConnectedPeerIds(): Set<String> = synchronized(connectedGatts) {
        connectedGatts.keys.toSet()
    }

    /**
     * Start advertising our mesh service UUID and start the GATT server.
     */
    override fun startAdvertising() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth not available or not enabled")
            return
        }

        // Start GATT server
        startGattServer()

        // Start BLE advertising
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0) // Advertise indefinitely
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        bluetoothAdapter.bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
            ?: Log.e(TAG, "BLE advertiser not available")

        Log.d(TAG, "Started advertising as $localDeviceId")
    }

    /**
     * Start scanning for nearby devices advertising our service UUID.
     * Uses duty cycling: scan for 15s, pause for 45s, repeat.
     */
    override fun startDiscovery() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth not available or not enabled")
            return
        }

        // Also start GATT server so peers can write to us
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

        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (_: Exception) { }

        synchronized(connectedGatts) {
            connectedGatts.values.forEach { it.close() }
            connectedGatts.clear()
            gattWriteLocks.clear()
        }

        gattServer?.close()
        gattServer = null

        discoveredAddresses.clear()
        addressToDeviceId.clear()
        deviceIdToAddress.clear()
        incomingBuffers.clear()
        expectedLengths.clear()

        Log.d(TAG, "Stopped")
    }

    /**
     * Send a payload to a connected peer by writing chunks to their GATT server's write characteristic.
     */
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
                    writeCharacteristic(gatt, writeChar, header.toByteArray())

                    // Send payload in chunks
                    var offset = 0
                    while (offset < data.size) {
                        val end = minOf(offset + CHUNK_SIZE, data.size)
                        val chunk = data.copyOfRange(offset, end)
                        writeCharacteristic(gatt, writeChar, chunk)
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

        // Characteristic for receiving data (peers write to this)
        val writeChar = BluetoothGattCharacteristic(
            WRITE_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        // Characteristic for reading device ID
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
     * Connect to a discovered BLE device as a GATT client.
     * Reads their device ID, then registers them as a connected peer.
     */
    private fun connectToDevice(device: BluetoothDevice) {
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "GATT client connected to ${device.address}")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "GATT client disconnected from ${device.address}")
                    val meshId = addressToDeviceId[device.address]
                    if (meshId != null) {
                        synchronized(connectedGatts) {
                            connectedGatts.remove(meshId)
                            gattWriteLocks.remove(meshId)
                        }
                        addressToDeviceId.remove(device.address)
                        deviceIdToAddress.remove(meshId)
                        deviceDisconnectedCallback?.invoke(meshId)
                    }
                    gatt.close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "Service discovery failed: $status")
                    gatt.close()
                    return
                }

                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    Log.w(TAG, "Mesh service not found on ${device.address}")
                    gatt.close()
                    return
                }

                // Read the device ID characteristic
                val deviceIdChar = service.getCharacteristic(DEVICE_ID_CHAR_UUID)
                if (deviceIdChar != null) {
                    gatt.readCharacteristic(deviceIdChar)
                } else {
                    Log.w(TAG, "Device ID characteristic not found")
                    gatt.close()
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status != BluetoothGatt.GATT_SUCCESS || characteristic.uuid != DEVICE_ID_CHAR_UUID) {
                    gatt.close()
                    return
                }

                val idString = String(characteristic.value)
                val parts = idString.split("|", limit = 2)
                val peerId = parts[0]
                val peerName = parts.getOrNull(1)?.ifEmpty { null }

                // Don't connect to ourselves
                if (peerId == localDeviceId) {
                    gatt.close()
                    return
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

            // Handle incoming data written to our characteristic via notification
            @Deprecated("Deprecated in API 33")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                if (characteristic.uuid == WRITE_CHAR_UUID) {
                    val address = device.address
                    handleIncomingChunk(address, characteristic.value)
                }
            }
        }

        device.connectGatt(context, false, gattCallback)
    }

    /**
     * Handle an incoming chunk of data. Reassembles chunked payloads.
     */
    private fun handleIncomingChunk(address: String, chunk: ByteArray) {
        val meshId = addressToDeviceId[address] ?: address

        if (address !in expectedLengths) {
            // First chunk is the 4-byte length header
            if (chunk.size >= 4) {
                val length = DataInputStream(ByteArrayInputStream(chunk)).readInt()
                expectedLengths[address] = length
                incomingBuffers[address] = ByteArrayOutputStream()
                // If header came with extra data, process it
                if (chunk.size > 4) {
                    incomingBuffers[address]?.write(chunk, 4, chunk.size - 4)
                }
            }
        } else {
            incomingBuffers[address]?.write(chunk)
        }

        // Check if we've received the full payload
        val expected = expectedLengths[address] ?: return
        val buffer = incomingBuffers[address] ?: return

        if (buffer.size() >= expected) {
            val payload = buffer.toByteArray().copyOf(expected)
            Log.d(TAG, "Received complete payload: $expected bytes from $meshId")
            payloadReceivedCallback?.invoke(meshId, payload)

            // Clean up
            expectedLengths.remove(address)
            incomingBuffers.remove(address)
        }
    }

    /**
     * Write a chunk to a GATT characteristic with a small delay for BLE stability.
     */
    private suspend fun writeCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        characteristic.value = value
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        gatt.writeCharacteristic(characteristic)
        // BLE needs a small delay between writes
        delay(30)
    }
}
