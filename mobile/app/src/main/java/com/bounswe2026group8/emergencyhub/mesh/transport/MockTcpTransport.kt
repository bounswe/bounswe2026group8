package com.bounswe2026group8.emergencyhub.mesh.transport

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket

/**
 * TCP-based mock transport for testing mesh sync between two emulators.
 *
 * Setup (one-time, from terminal):
 *   Emulator A (advertiser): runs a TCP server on PORT inside the emulator.
 *   On host machine:  adb -s emulator-5554 forward tcp:9998 tcp:9998
 *   Emulator B (discoverer): connects to 10.0.2.2:9998 which routes
 *   through the host to Emulator A.
 *
 * @param localDeviceId  unique ID for this device (use a UUID stored in SharedPreferences)
 * @param displayName    optional name shown to other devices
 */
class MockTcpTransport(
    private val localDeviceId: String,
    private val displayName: String?
) : MeshTransport {

    companion object {
        private const val TAG = "MockTcpTransport"
        const val PORT = 9998
        // 10.0.2.2 is the Android emulator's alias for the host machine
        const val HOST_ALIAS = "10.0.2.2"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var serverJob: Job? = null
    private var discoveryJob: Job? = null

    private var deviceFoundCallback: ((DeviceInfo) -> Unit)? = null
    private var payloadReceivedCallback: ((String, ByteArray) -> Unit)? = null
    private var deviceDisconnectedCallback: ((String) -> Unit)? = null

    // Track connected peers so we can send to them
    private val connectedPeers = mutableMapOf<String, DataOutputStream>()
    // One mutex per peer to serialize writes and prevent stream corruption
    private val peerWriteLocks = mutableMapOf<String, Mutex>()

    override fun onDeviceFound(callback: (DeviceInfo) -> Unit) {
        deviceFoundCallback = callback
    }

    override fun onPayloadReceived(callback: (deviceId: String, data: ByteArray) -> Unit) {
        payloadReceivedCallback = callback
    }

    override fun onDeviceDisconnected(callback: (deviceId: String) -> Unit) {
        deviceDisconnectedCallback = callback
    }

    override fun getConnectedPeerIds(): Set<String> = synchronized(connectedPeers) {
        connectedPeers.keys.toSet()
    }

    /**
     * Advertise by starting a TCP server.
     * Other emulators connect to us via host port forwarding.
     */
    override fun startAdvertising() {
        serverJob = scope.launch {
            try {
                val server = ServerSocket(PORT)
                Log.d(TAG, "Advertising: listening on port $PORT")
                while (true) {
                    val client = server.accept()
                    Log.d(TAG, "Peer connected from ${client.inetAddress}")
                    handleConnection(client)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
            }
        }
    }

    /**
     * Discover by connecting to the advertiser via the host alias.
     */
    override fun startDiscovery() {
        discoveryJob = scope.launch {
            try {
                Log.d(TAG, "Discovering: connecting to $HOST_ALIAS:$PORT")
                val socket = Socket(HOST_ALIAS, PORT)
                Log.d(TAG, "Connected to advertiser")
                handleConnection(socket)
            } catch (e: Exception) {
                Log.e(TAG, "Discovery connection failed", e)
            }
        }
    }

    override fun stop() {
        serverJob?.cancel()
        discoveryJob?.cancel()
        synchronized(connectedPeers) {
            connectedPeers.values.forEach { runCatching { it.close() } }
            connectedPeers.clear()
            peerWriteLocks.clear()
        }
        Log.d(TAG, "Stopped")
    }

    override fun sendPayload(deviceId: String, data: ByteArray) {
        scope.launch {
            try {
                val output: DataOutputStream
                val mutex: Mutex
                synchronized(connectedPeers) {
                    output = connectedPeers[deviceId] ?: run {
                        Log.w(TAG, "No connection to device $deviceId")
                        return@launch
                    }
                    mutex = peerWriteLocks.getOrPut(deviceId) { Mutex() }
                }
                // Serialize all writes to this peer's stream
                mutex.withLock {
                    output.writeInt(data.size)
                    output.write(data)
                    output.flush()
                }
                Log.d(TAG, "Sent ${data.size} bytes to $deviceId")
            } catch (e: Exception) {
                Log.e(TAG, "Send failed to $deviceId", e)
            }
        }
    }

    /**
     * Handle a TCP connection (works for both server-accepted and client-initiated).
     * 1. Exchange device IDs (handshake)
     * 2. Listen for incoming payloads
     */
    private fun handleConnection(socket: Socket) {
        scope.launch {
            var peerId: String? = null
            try {
                val input = DataInputStream(socket.getInputStream())
                val output = DataOutputStream(socket.getOutputStream())

                // --- Handshake: send our device ID and display name ---
                val handshake = "$localDeviceId|${displayName.orEmpty()}"
                output.writeInt(handshake.length)
                output.write(handshake.toByteArray())
                output.flush()

                // --- Read peer's handshake ---
                val peerHandshakeLen = input.readInt()
                val peerHandshakeBytes = ByteArray(peerHandshakeLen)
                input.readFully(peerHandshakeBytes)
                val peerHandshake = String(peerHandshakeBytes)
                val parts = peerHandshake.split("|", limit = 2)
                peerId = parts[0]
                val peerName = parts.getOrNull(1)?.ifEmpty { null }

                Log.d(TAG, "Handshake complete with peer: $peerId ($peerName)")

                synchronized(connectedPeers) {
                    connectedPeers[peerId!!] = output
                    peerWriteLocks[peerId!!] = Mutex()
                }
                deviceFoundCallback?.invoke(DeviceInfo(peerId!!, peerName))

                // --- Read loop: receive length-prefixed payloads ---
                while (true) {
                    val length = input.readInt()
                    val payload = ByteArray(length)
                    input.readFully(payload)
                    Log.d(TAG, "Received $length bytes from $peerId")
                    payloadReceivedCallback?.invoke(peerId!!, payload)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Connection closed for $peerId: ${e.message}")
                if (peerId != null) {
                    synchronized(connectedPeers) {
                        connectedPeers.remove(peerId)
                        peerWriteLocks.remove(peerId)
                    }
                    deviceDisconnectedCallback?.invoke(peerId!!)
                }
            }
        }
    }
}
