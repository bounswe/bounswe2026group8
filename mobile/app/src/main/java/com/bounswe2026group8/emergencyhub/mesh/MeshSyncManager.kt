package com.bounswe2026group8.emergencyhub.mesh

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bounswe2026group8.emergencyhub.mesh.db.MeshMessage
import com.bounswe2026group8.emergencyhub.mesh.db.MeshMessageDao
import com.bounswe2026group8.emergencyhub.mesh.transport.DeviceInfo
import com.bounswe2026group8.emergencyhub.mesh.transport.MeshProtocol
import com.bounswe2026group8.emergencyhub.mesh.transport.MeshTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Orchestrates the mesh sync flow:
 *  1. When a peer is discovered, send our message inventory
 *  2. When we receive their inventory, request the messages we're missing
 *  3. When we receive messages, store them in Room
 *
 * Also handles creating new local messages and cleaning up expired ones.
 */
class MeshSyncManager(
    context: Context,
    private val transport: MeshTransport,
    private val dao: MeshMessageDao
) {
    companion object {
        private const val TAG = "MeshSyncManager"
        private const val PREFS_NAME = "mesh_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_SHARE_LOCATION = "share_location"

        /** Get or create the stable device ID without creating a full SyncManager. */
        fun getDeviceId(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            if (existing != null) return existing
            val id = java.util.UUID.randomUUID().toString().take(8)
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            return id
        }

        /** Read the persisted "share location" toggle without creating a SyncManager. */
        fun isLocationSharingEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHARE_LOCATION, false)

        /** Persist the "share location" toggle. */
        fun setLocationSharingEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SHARE_LOCATION, enabled).apply()
        }
    }

    // SupervisorJob: a single child failing (e.g., a DB query throwing) must not
    // cancel the whole scope. Without this, an exception in deleteExpiredMessages
    // or handleMessages would silently kill all subsequent sync work — BLE would
    // stay connected at the radio layer but no data would flow.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val localDeviceId: String = getOrCreateDeviceId()
    var displayName: String?
        get() = prefs.getString(KEY_DISPLAY_NAME, null)
        set(value) = prefs.edit().putString(KEY_DISPLAY_NAME, value).apply()

    // Listener for UI updates
    var onMessagesUpdated: (() -> Unit)? = null
    var onPeersUpdated: (() -> Unit)? = null

    // Track peers we've already exchanged inventories with to prevent infinite loop
    private val inventorySentToPeers = mutableSetOf<String>()

    // deviceId -> displayName? for currently connected peers (for UI display)
    private val connectedPeers = mutableMapOf<String, String?>()

    fun getConnectedPeers(): List<DeviceInfo> = synchronized(connectedPeers) {
        connectedPeers.map { DeviceInfo(it.key, it.value) }
    }

    private fun getOrCreateDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing
        val id = UUID.randomUUID().toString().take(8)
        prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        return id
    }

    /**
     * Initialize the mesh — register callbacks and clean expired messages.
     * The caller is responsible for starting the transport
     * (calling startAdvertising/startDiscovery on the transport directly).
     */
    fun start() {
        // Clean expired messages first
        scope.launch { deleteExpiredMessages() }

        // Set up callbacks
        transport.onDeviceFound { peer ->
            synchronized(connectedPeers) { connectedPeers[peer.deviceId] = peer.displayName }
            onPeersUpdated?.invoke()
            onPeerFound(peer)
        }
        transport.onPayloadReceived { deviceId, data -> onPayloadReceived(deviceId, data) }
        transport.onDeviceDisconnected { deviceId ->
            synchronized(connectedPeers) { connectedPeers.remove(deviceId) }
            inventorySentToPeers.remove(deviceId)
            onPeersUpdated?.invoke()
            Log.d(TAG, "Peer disconnected: $deviceId")
        }

        Log.d(TAG, "MeshSyncManager initialized for $localDeviceId")
    }

    fun stop() {
        transport.stop()
        inventorySentToPeers.clear()
        synchronized(connectedPeers) { connectedPeers.clear() }
        onPeersUpdated?.invoke()
    }

    /**
     * Create a new top-level post and gossip it to connected peers.
     */
    fun sendPost(
        title: String,
        body: String,
        postType: String,
        latitude: Double? = null,
        longitude: Double? = null,
        locAccuracyMeters: Float? = null,
        locCapturedAt: Long? = null
    ) {
        val message = buildMessage(
            body = body,
            title = title,
            postType = postType,
            parentPostId = null,
            latitude = latitude,
            longitude = longitude,
            locAccuracyMeters = locAccuracyMeters,
            locCapturedAt = locCapturedAt
        )
        persistAndBroadcast(message)
    }

    /**
     * Create a comment on an existing post and gossip it to connected peers.
     */
    fun sendComment(
        parentPostId: String,
        body: String,
        latitude: Double? = null,
        longitude: Double? = null,
        locAccuracyMeters: Float? = null,
        locCapturedAt: Long? = null
    ) {
        val message = buildMessage(
            body = body,
            title = null,
            postType = null,
            parentPostId = parentPostId,
            latitude = latitude,
            longitude = longitude,
            locAccuracyMeters = locAccuracyMeters,
            locCapturedAt = locCapturedAt
        )
        persistAndBroadcast(message)
    }

    private fun buildMessage(
        body: String,
        title: String?,
        postType: String?,
        parentPostId: String?,
        latitude: Double?,
        longitude: Double?,
        locAccuracyMeters: Float?,
        locCapturedAt: Long?
    ): MeshMessage {
        val now = System.currentTimeMillis()
        return MeshMessage(
            id = UUID.randomUUID().toString(),
            authorDeviceId = localDeviceId,
            authorDisplayName = displayName,
            body = body,
            createdAt = now,
            receivedAt = now,
            ttlHours = 72,
            hopCount = 0,
            syncedToServer = false,
            latitude = latitude,
            longitude = longitude,
            locAccuracyMeters = locAccuracyMeters,
            locCapturedAt = locCapturedAt,
            title = title,
            postType = postType,
            parentPostId = parentPostId
        )
    }

    private fun persistAndBroadcast(message: MeshMessage) {
        scope.launch {
            dao.insertMessage(message)
            val kind = if (message.parentPostId == null) "post" else "comment"
            Log.d(TAG, "Created local $kind: ${message.id}")
            onMessagesUpdated?.invoke()

            val peers = transport.getConnectedPeerIds()
            Log.d(TAG, "Connected peers: $peers (count=${peers.size})")
            val payload = MeshProtocol.encodeMessages(listOf(message.copy(hopCount = 1)))
            for (peerId in peers) {
                transport.sendPayload(peerId, payload)
                Log.d(TAG, "Pushed new $kind to peer $peerId")
            }
        }
    }

    /**
     * When we discover a peer, send our inventory so they know what we have.
     */
    private fun onPeerFound(peer: DeviceInfo) {
        Log.d(TAG, "Peer found: ${peer.deviceId} (${peer.displayName})")
        scope.launch {
            val myIds = dao.getAllMessageIds()
            val payload = MeshProtocol.encodeInventory(myIds)
            transport.sendPayload(peer.deviceId, payload)
            Log.d(TAG, "Sent inventory of ${myIds.size} message IDs to ${peer.deviceId}")
        }
    }

    /**
     * Handle incoming payloads based on their type.
     */
    private fun onPayloadReceived(deviceId: String, data: ByteArray) {
        if (data.isEmpty()) return

        when (MeshProtocol.getType(data)) {
            MeshProtocol.TYPE_INVENTORY -> handleInventory(deviceId, data)
            MeshProtocol.TYPE_REQUEST -> handleRequest(deviceId, data)
            MeshProtocol.TYPE_MESSAGES -> handleMessages(data)
            else -> Log.w(TAG, "Unknown payload type: ${data[0]}")
        }
    }

    /**
     * Peer sent their inventory. Figure out what they have that we don't,
     * and request those messages. Send our inventory back only once.
     */
    private fun handleInventory(deviceId: String, data: ByteArray) {
        scope.launch {
            val theirIds = MeshProtocol.decodeInventory(data)
            val myIds = dao.getAllMessageIds().toSet()

            // IDs they have that we don't
            val missing = theirIds.filter { it !in myIds }
            if (missing.isNotEmpty()) {
                val request = MeshProtocol.encodeRequest(missing)
                transport.sendPayload(deviceId, request)
                Log.d(TAG, "Requested ${missing.size} messages from $deviceId")
            }

            // Send our inventory back only once per peer to avoid infinite loop
            if (inventorySentToPeers.add(deviceId)) {
                val ourInventory = MeshProtocol.encodeInventory(myIds.toList())
                transport.sendPayload(deviceId, ourInventory)
                Log.d(TAG, "Sent inventory of ${myIds.size} message IDs back to $deviceId")
            }
        }
    }

    /**
     * Peer wants specific messages from us. Send them, with posts ordered before
     * any comments — so the receiver's batch insert never has to deal with a
     * comment whose parent is later in the same payload.
     */
    private fun handleRequest(deviceId: String, data: ByteArray) {
        scope.launch {
            val requestedIds = MeshProtocol.decodeRequest(data)
            val allMessages = dao.getAllMessages()
            val toSend = allMessages
                .filter { it.id in requestedIds.toSet() }
                .sortedBy { if (it.parentPostId == null) 0 else 1 }

            if (toSend.isNotEmpty()) {
                // Increment hop count before sending
                val withHops = toSend.map { it.copy(hopCount = it.hopCount + 1) }
                val payload = MeshProtocol.encodeMessages(withHops)
                transport.sendPayload(deviceId, payload)
                Log.d(TAG, "Sent ${toSend.size} messages to $deviceId")
            }
        }
    }

    /**
     * Received messages from a peer. Store them locally.
     */
    private fun handleMessages(data: ByteArray) {
        scope.launch {
            val messages = MeshProtocol.decodeMessages(data)
            // Set receivedAt to now for all incoming messages
            val now = System.currentTimeMillis()
            val withTimestamp = messages.map { it.copy(receivedAt = now) }
            dao.insertMessages(withTimestamp)
            Log.d(TAG, "Stored ${messages.size} messages from peer")
            onMessagesUpdated?.invoke()
        }
    }

    /**
     * Delete messages older than their TTL.
     */
    private suspend fun deleteExpiredMessages() {
        val now = System.currentTimeMillis()
        // We need to check each message's TTL individually, but for simplicity
        // we use the default 72h cutoff
        val cutoff = now - (72 * 60 * 60 * 1000L)
        dao.deleteExpired(cutoff)
    }
}
