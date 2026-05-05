package com.bounswe2026group8.emergencyhub.mesh.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row covers two roles:
 *  - **post**:    parentPostId == null, postType in {"NEED_HELP","OFFER_HELP"}, title set
 *  - **comment**: parentPostId == <post id>, postType == null, title == null
 *
 * Keeping both in one table means the gossip protocol stays unchanged — comments
 * participate in inventory diffs alongside posts.
 */
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
    val syncedToServer: Boolean = false,     // true once uploaded to backend
    // Optional location attached at send time. All four are null when the
    // author did not opt in to sharing location for this message.
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locAccuracyMeters: Float? = null,
    val locCapturedAt: Long? = null,         // epoch millis of the underlying GPS fix
    // Forum fields
    val title: String? = null,               // post title; null for comments
    val postType: String? = null,            // "NEED_HELP" / "OFFER_HELP"; null for comments
    val parentPostId: String? = null         // null for top-level posts; post.id for comments
)
