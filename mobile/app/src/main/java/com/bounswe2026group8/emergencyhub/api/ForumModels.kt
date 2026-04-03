package com.bounswe2026group8.emergencyhub.api

import com.google.gson.annotations.SerializedName

data class PostAuthor(
    val id: Int,
    @SerializedName("full_name") val fullName: String,
    val email: String,
    val role: String
)

data class RepostOrigin(
    val id: Int,
    val title: String,
    val author: PostAuthor
)

data class Post(
    val id: Int,
    val hub: Int?,
    @SerializedName("hub_name") val hubName: String?,
    @SerializedName("forum_type") val forumType: String,
    val author: PostAuthor,
    val title: String,
    val content: String?,
    @SerializedName("image_urls") val imageUrls: List<String>?,
    val status: String,
    @SerializedName("upvote_count") val upvoteCount: Int,
    @SerializedName("downvote_count") val downvoteCount: Int,
    @SerializedName("comment_count") val commentCount: Int,
    @SerializedName("repost_count") val repostCount: Int,
    @SerializedName("user_vote") val userVote: String?,
    @SerializedName("user_has_reposted") val userHasReposted: Boolean?,
    @SerializedName("reposted_from") val repostedFrom: RepostOrigin?,
    @SerializedName("created_at") val createdAt: String
)

data class CreatePostRequest(
    @SerializedName("forum_type") val forumType: String,
    val title: String,
    val content: String,
    @SerializedName("image_urls") val imageUrls: List<String> = emptyList(),
    val hub: Int? = null
)

data class Comment(
    val id: Int,
    val post: Int,
    val author: PostAuthor,
    val content: String,
    @SerializedName("created_at") val createdAt: String
)

data class CreateCommentRequest(
    val content: String
)

data class VoteRequest(
    @SerializedName("vote_type") val voteType: String
)

data class VoteResponse(
    val detail: String,
    val vote: String?
)

data class RepostRequest(
    val hub: Int? = null
)

data class ReportRequest(
    val reason: String
)

data class UploadImagesResponse(
    val urls: List<String>
)
