package com.bounswe2026group8.emergencyhub.mesh

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.bounswe2026group8.emergencyhub.api.MeshMessageDto
import com.bounswe2026group8.emergencyhub.api.MeshSyncRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bounswe2026group8.emergencyhub.mesh.db.MeshMessage
import com.bounswe2026group8.emergencyhub.offline.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pushes locally-stored mesh posts and comments (anything with
 * `syncedToServer = false`) up to the backend so they appear in the
 * "Offline Messages" archive once the user has internet again.
 *
 * The upload is **idempotent on both sides**:
 *   - mobile only sends rows where `syncedToServer = false`
 *   - server upserts by `id`, so if peer A and peer B both gossiped the same
 *     message and both come online, the second upload is a no-op
 *
 * Successful ids returned by the server are marked `syncedToServer = true`
 * locally so we never re-upload them.
 *
 * This class is a one-shot trigger — call [uploadIfOnline] from any screen
 * that wants the local mesh inventory pushed. The caller is responsible for
 * timing (e.g., onResume, network-available callback).
 */
object MeshServerSyncManager {

    private const val TAG = "MeshServerSync"
    private const val BATCH_SIZE = 100  // safety cap per request

    /**
     * If the device has internet AND the user is logged in, upload any local
     * messages not yet synced. Quietly no-op otherwise.
     *
     * Returns the number of newly accepted messages, or 0 if skipped/failed.
     */
    suspend fun uploadIfOnline(context: Context): Int = withContext(Dispatchers.IO) {
        if (!hasInternet(context)) {
            Log.d(TAG, "skip: no internet")
            return@withContext 0
        }
        val token = TokenManager(context).getToken()
        if (token.isNullOrBlank()) {
            Log.d(TAG, "skip: no auth token")
            return@withContext 0
        }

        val dao = AppDatabase.getDatabase(context).meshMessageDao()
        val unsynced = dao.getUnsyncedMessages()
        if (unsynced.isEmpty()) {
            Log.d(TAG, "skip: nothing to upload")
            return@withContext 0
        }

        val api = RetrofitClient.getService(context)
        var totalAccepted = 0
        for (batch in unsynced.chunked(BATCH_SIZE)) {
            try {
                val request = MeshSyncRequest(batch.map { it.toDto() })
                val response = api.syncMeshMessages(request)
                if (!response.isSuccessful) {
                    Log.w(TAG, "upload failed: HTTP ${response.code()}")
                    break
                }
                val acceptedIds = response.body()?.accepted.orEmpty()
                // Mark the entire batch synced — both newly-accepted ids AND ids
                // the server already had (those are dedup'd as success too — we
                // never need to upload them again).
                for (msg in batch) {
                    dao.markSynced(msg.id)
                }
                totalAccepted += acceptedIds.size
                Log.d(TAG, "uploaded ${batch.size} (accepted ${acceptedIds.size} new)")
            } catch (e: Exception) {
                Log.w(TAG, "upload threw: ${e.message}")
                break
            }
        }
        totalAccepted
    }

    private fun hasInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun MeshMessage.toDto(): MeshMessageDto = MeshMessageDto(
        id = id,
        authorDeviceId = authorDeviceId,
        authorDisplayName = authorDisplayName,
        body = body,
        createdAt = createdAt,
        receivedAt = receivedAt,
        ttlHours = ttlHours,
        hopCount = hopCount,
        latitude = latitude,
        longitude = longitude,
        locAccuracyMeters = locAccuracyMeters,
        locCapturedAt = locCapturedAt,
        title = title,
        postType = postType,
        parentPostId = parentPostId
    )
}
