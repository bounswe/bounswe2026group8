package com.bounswe2026group8.emergencyhub.mesh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.mesh.transport.BleTransport
import com.bounswe2026group8.emergencyhub.mesh.transport.MeshTransport
import com.bounswe2026group8.emergencyhub.mesh.transport.MockTcpTransport
import com.bounswe2026group8.emergencyhub.mesh.ui.MeshActivity
import com.bounswe2026group8.emergencyhub.offline.data.AppDatabase

class MeshForegroundService : Service() {

    companion object {
        private const val TAG = "MeshForegroundService"
        private const val CHANNEL_ID = "mesh_service_channel"
        private const val NOTIFICATION_ID = 1001

        const val EXTRA_USE_BLE = "use_ble"
        const val EXTRA_DISPLAY_NAME = "display_name"

        const val ACTION_STOP = "com.bounswe2026group8.emergencyhub.mesh.STOP"
    }

    private var syncManager: MeshSyncManager? = null
    private var transport: MeshTransport? = null

    // Binder so the Activity can access syncManager
    inner class MeshBinder : Binder() {
        fun getSyncManager(): MeshSyncManager? = syncManager
    }

    private val binder = MeshBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopMesh()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val useBle = intent?.getBooleanExtra(EXTRA_USE_BLE, true) ?: true
        val displayName = intent?.getStringExtra(EXTRA_DISPLAY_NAME)

        startForeground(NOTIFICATION_ID, buildNotification())
        startMesh(useBle, displayName)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMesh()
    }

    private fun startMesh(useBle: Boolean, displayName: String?) {
        val deviceId = MeshSyncManager.getDeviceId(this)
        val dao = AppDatabase.getDatabase(this).meshMessageDao()

        transport = if (useBle) {
            BleTransport(this, deviceId, displayName)
        } else {
            MockTcpTransport(deviceId, displayName)
        }

        syncManager = MeshSyncManager(this, transport!!, dao).also {
            if (displayName != null) it.displayName = displayName
            it.start()
        }

        if (useBle) {
            transport!!.startAdvertising()
            transport!!.startDiscovery()
        } else {
            transport!!.startAdvertising()
        }

        Log.d(TAG, "Mesh started: useBle=$useBle, deviceId=$deviceId")
    }

    private fun stopMesh() {
        syncManager?.stop()
        syncManager = null
        transport = null
        Log.d(TAG, "Mesh stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mesh Messaging",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps offline mesh messaging active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MeshActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MeshForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mesh Messaging Active")
            .setContentText("Scanning for nearby devices")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pendingOpen)
            .addAction(0, "Stop", pendingStop)
            .build()
    }
}
