package com.bounswe2026group8.emergencyhub.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.ui.PostDetailActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token will be sent to backend on next login
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val postId = message.data["post_id"]?.toIntOrNull() ?: return
        val title = message.data["title"] ?: "Urgent Post"
        val body = message.data["body"] ?: ""

        createNotificationChannel()

        val intent = Intent(this, PostDetailActivity::class.java).apply {
            putExtra("post_id", postId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, postId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Urgent Posts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for urgent forum posts"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "urgent_posts"
    }
}
