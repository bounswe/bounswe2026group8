package com.bounswe2026group8.emergencyhub.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the backend API.
 * Covers auth, help-requests, help-offers, and forum endpoints.
 */
interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────

    @POST("/register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

    @POST("/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("/logout")
    suspend fun logout(): Response<LogoutResponse>

    @GET("/me")
    suspend fun getMe(): Response<MeResponse>

    @PATCH("/me")
    suspend fun updateMe(@Body body: UpdateMeRequest): Response<MeResponse>

    // ── Help Requests ────────────────────────────────────────────────────

    @GET("/help-requests/")
    suspend fun getHelpRequests(
        @Query("hub_id") hubId: Int? = null,
        @Query("category") category: String? = null
    ): Response<List<HelpRequestItem>>

    @POST("/help-requests/")
    suspend fun createHelpRequest(
        @Body body: CreateHelpRequest
    ): Response<HelpRequestDetail>

    @GET("/help-requests/{id}/")
    suspend fun getHelpRequestDetail(
        @Path("id") id: Int
    ): Response<HelpRequestDetail>

    // ── Help Request Comments ────────────────────────────────────────────

    @GET("/help-requests/{id}/comments/")
    suspend fun getHelpRequestComments(
        @Path("id") requestId: Int
    ): Response<List<HelpRequestComment>>

    @POST("/help-requests/{id}/comments/")
    suspend fun createHelpRequestComment(
        @Path("id") requestId: Int,
        @Body body: CreateCommentRequest
    ): Response<HelpRequestComment>

    // ── Help Offers ──────────────────────────────────────────────────────

    @GET("/help-offers/")
    suspend fun getHelpOffers(
        @Query("hub_id") hubId: Int? = null,
        @Query("category") category: String? = null
    ): Response<List<HelpOfferItem>>

    @POST("/help-offers/")
    suspend fun createHelpOffer(
        @Body body: CreateHelpOffer
    ): Response<HelpOfferItem>

    /** Returns Response<ResponseBody?> to avoid Gson parsing the empty 204 body. */
    @DELETE("/help-offers/{id}/")
    suspend fun deleteHelpOffer(
        @Path("id") id: Int
    ): Response<ResponseBody?>

    // ── Hubs ─────────────────────────────────────────────────────────────

    @GET("/hubs/")
    suspend fun getHubs(): Response<List<Hub>>

    // ── Forum ────────────────────────────────────────────────────────────

    @GET("/forum/posts/")
    suspend fun getPosts(
        @Query("forum_type") forumType: String,
        @Query("hub") hub: Int? = null
    ): Response<List<Post>>

    @POST("/forum/posts/")
    suspend fun createPost(@Body body: CreatePostRequest): Response<Post>

    @GET("/forum/posts/{id}/")
    suspend fun getPost(@Path("id") id: Int): Response<Post>

    @GET("/forum/posts/{postId}/comments/")
    suspend fun getComments(@Path("postId") postId: Int): Response<List<Comment>>

    @POST("/forum/posts/{postId}/comments/")
    suspend fun createComment(
        @Path("postId") postId: Int,
        @Body body: CreateCommentRequest
    ): Response<Comment>

    @DELETE("/forum/comments/{id}/")
    suspend fun deleteComment(@Path("id") id: Int): Response<ResponseBody>

    @DELETE("/forum/posts/{id}/")
    suspend fun deletePost(@Path("id") id: Int): Response<ResponseBody>

    @POST("/forum/posts/{postId}/vote/")
    suspend fun vote(
        @Path("postId") postId: Int,
        @Body body: VoteRequest
    ): Response<VoteResponse>

    @POST("/forum/posts/{postId}/repost/")
    suspend fun repost(
        @Path("postId") postId: Int,
        @Body body: RepostRequest
    ): Response<Post>

    @POST("/forum/posts/{postId}/report/")
    suspend fun reportPost(
        @Path("postId") postId: Int,
        @Body body: ReportRequest
    ): Response<ResponseBody>

    @Multipart
    @POST("/forum/upload/")
    suspend fun uploadImages(
        @Part images: List<MultipartBody.Part>
    ): Response<UploadImagesResponse>

    @POST("/accounts/fcm-token/")
    suspend fun updateFcmToken(@Body body: FcmTokenRequest): Response<Void>
}
