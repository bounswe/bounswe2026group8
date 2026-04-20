package com.bounswe2026group8.emergencyhub.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.ui.DashboardActivity
import com.bounswe2026group8.emergencyhub.ui.ForumActivity
import com.bounswe2026group8.emergencyhub.ui.HelpRequestDetailActivity
import com.bounswe2026group8.emergencyhub.ui.HelpRequestListActivity
import com.bounswe2026group8.emergencyhub.ui.PostDetailActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles incoming FCM messages for both forum posts and help requests.
 *
 * Dispatches based on the `type` field in the data payload:
 *   - "help_request"       → [handleHelpRequestNotification] → opens [HelpRequestDetailActivity]
 *   - "no_expert_available"→ [handleNoExpertNotification]    → opens [HelpRequestDetailActivity]
 *   - anything else        → [handlePostNotification]        → opens [PostDetailActivity] (forum)
 *
 * Because the backend sends data-only messages (no `notification` key),
 * [onMessageReceived] is called in both foreground and background states.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token will be sent to backend on next login via DashboardActivity.sendFcmTokenToBackend()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Dispatch to the correct handler based on notification type.
        when (message.data["type"]) {
            "help_request"        -> handleHelpRequestNotification(message.data)
            "no_expert_available" -> handleNoExpertNotification(message.data)
            else                  -> handlePostNotification(message.data)
        }
    }

    // ── Help Request Notifications ────────────────────────────────────────────

    /**
     * Shows a notification for a new help request and deep-links to its detail screen on tap.
     *
     * Respects the user's "pref_help_request_notifications" SharedPreference —
     * if the user has disabled help request notifications, this returns immediately.
     */
    private fun handleHelpRequestNotification(data: Map<String, String>) {
        val requestId = data["request_id"]?.toIntOrNull() ?: return
        val title = data["title"] ?: "New Help Request"
        val body = data["body"] ?: ""

        createHelpRequestNotificationChannel()

        val detailIntent = Intent(this, HelpRequestDetailActivity::class.java).apply {
            putExtra(HelpRequestDetailActivity.EXTRA_REQUEST_ID, requestId)
        }

        val pendingIntent = TaskStackBuilder.create(this)
            .addNextIntentWithParentStack(Intent(this, DashboardActivity::class.java))
            .addNextIntent(Intent(this, HelpRequestListActivity::class.java))
            .addNextIntent(detailIntent)
            .getPendingIntent(requestId, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, HELP_REQUEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // Offset by 100_000 to avoid ID collision with forum post notification IDs.
        manager.notify(requestId + 100_000, notification)
    }

    private fun createHelpRequestNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                HELP_REQUEST_CHANNEL_ID,
                "Help Requests",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications for new help requests in your hub"
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    /**
     * Notifies the requester that no matching expert was found for their help request.
     * Deep-links to the same [HelpRequestDetailActivity] so they can see their request.
     */
    private fun handleNoExpertNotification(data: Map<String, String>) {
        val requestId = data["request_id"]?.toIntOrNull() ?: return
        val title = data["title"] ?: "No specialist available"
        val body = data["body"] ?: ""

        createHelpRequestNotificationChannel()

        val detailIntent = Intent(this, HelpRequestDetailActivity::class.java).apply {
            putExtra(HelpRequestDetailActivity.EXTRA_REQUEST_ID, requestId)
        }

        val pendingIntent = TaskStackBuilder.create(this)
            .addNextIntentWithParentStack(Intent(this, DashboardActivity::class.java))
            .addNextIntent(Intent(this, HelpRequestListActivity::class.java))
            .addNextIntent(detailIntent)
            .getPendingIntent(requestId + 200_000, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, HELP_REQUEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(requestId + 200_000, notification)
    }

    // ── Forum Post Notifications ──────────────────────────────────────────────

    /**
     * Shows a notification for an urgent forum post and deep-links to [PostDetailActivity].
     */
    private fun handlePostNotification(data: Map<String, String>) {
        val postId = data["post_id"]?.toIntOrNull() ?: return
        val title = data["title"] ?: "Urgent Post"
        val body = data["body"] ?: ""

        createPostNotificationChannel()

        val detailIntent = Intent(this, PostDetailActivity::class.java).apply {
            putExtra("post_id", postId)
        }

        val pendingIntent = TaskStackBuilder.create(this)
            .addNextIntentWithParentStack(Intent(this, DashboardActivity::class.java))
            .addNextIntent(Intent(this, ForumActivity::class.java))
            .addNextIntent(detailIntent)
            .getPendingIntent(postId, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, POST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(postId, notification)
    }

    private fun createPostNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                POST_CHANNEL_ID,
                "Urgent Posts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications for urgent forum posts"
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        private const val POST_CHANNEL_ID = "urgent_posts"
        private const val HELP_REQUEST_CHANNEL_ID = "help_requests"
    }
}
