package com.bounswe2026group8.emergencyhub.api

import com.google.gson.annotations.SerializedName

/**
 * Wire format for a mesh message (post or comment) shared with the backend.
 * Mirrors `mesh.MeshOfflineMessage` on the server.
 *
 * Same row covers two roles:
 *  - **post**:    parentPostId == null, postType in {"NEED_HELP","OFFER_HELP"}, title set
 *  - **comment**: parentPostId == <post id>, postType == null, title == null
 */
data class MeshMessageDto(
    val id: String,
    @SerializedName("author_device_id") val authorDeviceId: String,
    @SerializedName("author_display_name") val authorDisplayName: String?,
    val body: String,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("received_at") val receivedAt: Long,
    @SerializedName("ttl_hours") val ttlHours: Int,
    @SerializedName("hop_count") val hopCount: Int,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("loc_accuracy_meters") val locAccuracyMeters: Float?,
    @SerializedName("loc_captured_at") val locCapturedAt: Long?,
    val title: String?,
    @SerializedName("post_type") val postType: String?,
    @SerializedName("parent_post_id") val parentPostId: String?,
    /** Set only on responses (server populates auto_now_add). */
    @SerializedName("uploaded_at") val uploadedAt: String? = null
)

data class MeshSyncRequest(
    val messages: List<MeshMessageDto>
)

data class MeshSyncResponse(
    val accepted: List<String>
)
