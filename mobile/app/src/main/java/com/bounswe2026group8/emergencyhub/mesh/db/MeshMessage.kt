package com.bounswe2026group8.emergencyhub.mesh.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mesh_messages")
data class MeshMessage(
    @PrimaryKey val id: String,              // UUID generated on creation
    val authorDeviceId: String,              // unique device identifier
    val authorDisplayName: String?,          // optional user-chosen name
    val body: String,
    val createdAt: Long,                     // epoch millis - when message was originally created
    val receivedAt: Long,                    // epoch millis - when this device received it
    val ttlHours: Int = 72,                  // message expires after this many hours
    val hopCount: Int = 0,                   // how many devices relayed this before us
    val syncedToServer: Boolean = false      // true once uploaded to backend
)
