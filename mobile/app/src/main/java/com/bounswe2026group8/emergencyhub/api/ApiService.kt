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

    @POST("/api/register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

    @POST("/api/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("/api/logout")
    suspend fun logout(): Response<LogoutResponse>

    @GET("/api/me")
    suspend fun getMe(): Response<MeResponse>

    @PATCH("/api/me")
    suspend fun updateMe(@Body body: UpdateMeRequest): Response<MeResponse>

    // ── Help Requests ────────────────────────────────────────────────────

    @GET("/api/help-requests/")
    suspend fun getHelpRequests(
        @Query("hub_id") hubId: Int? = null,
        @Query("category") category: String? = null
    ): Response<List<HelpRequestItem>>

    @POST("/api/help-requests/")
    suspend fun createHelpRequest(
        @Body body: CreateHelpRequest
    ): Response<HelpRequestDetail>

    @GET("/api/help-requests/{id}/")
    suspend fun getHelpRequestDetail(
        @Path("id") id: Int
    ): Response<HelpRequestDetail>

    @DELETE("/help-requests/{id}/")
    suspend fun deleteHelpRequest(
        @Path("id") id: Int
    ): Response<ResponseBody?>

    // ── Help Request Comments ────────────────────────────────────────────

    @GET("/api/help-requests/{id}/comments/")
    suspend fun getHelpRequestComments(
        @Path("id") requestId: Int
    ): Response<List<HelpRequestComment>>

    @POST("/api/help-requests/{id}/comments/")
    suspend fun createHelpRequestComment(
        @Path("id") requestId: Int,
        @Body body: CreateCommentRequest
    ): Response<HelpRequestComment>

    @DELETE("/help-requests/comments/{id}/")
    suspend fun deleteHelpRequestComment(
        @Path("id") commentId: Int
    ): Response<ResponseBody?>

    // ── Help Offers ──────────────────────────────────────────────────────

    @GET("/api/help-offers/")
    suspend fun getHelpOffers(
        @Query("hub_id") hubId: Int? = null,
        @Query("category") category: String? = null
    ): Response<List<HelpOfferItem>>

    @POST("/api/help-offers/")
    suspend fun createHelpOffer(
        @Body body: CreateHelpOffer
    ): Response<HelpOfferItem>

    /** Returns Response<ResponseBody?> to avoid Gson parsing the empty 204 body. */
    @DELETE("/api/help-offers/{id}/")
    suspend fun deleteHelpOffer(
        @Path("id") id: Int
    ): Response<ResponseBody?>

    // ── Hubs ─────────────────────────────────────────────────────────────

    @GET("/api/hubs/")
    suspend fun getHubs(): Response<List<Hub>>

    // ── Forum ────────────────────────────────────────────────────────────

    @GET("/api/forum/posts/")
    suspend fun getPosts(
        @Query("forum_type") forumType: String,
        @Query("hub") hub: Int? = null
    ): Response<List<Post>>

    @POST("/api/forum/posts/")
    suspend fun createPost(@Body body: CreatePostRequest): Response<Post>

    @GET("/api/forum/posts/{id}/")
    suspend fun getPost(@Path("id") id: Int): Response<Post>

    @GET("/api/forum/posts/{postId}/comments/")
    suspend fun getComments(@Path("postId") postId: Int): Response<List<Comment>>

    @POST("/api/forum/posts/{postId}/comments/")
    suspend fun createComment(
        @Path("postId") postId: Int,
        @Body body: CreateCommentRequest
    ): Response<Comment>

    @DELETE("/api/forum/comments/{id}/")
    suspend fun deleteComment(@Path("id") id: Int): Response<ResponseBody>

    @DELETE("/api/forum/posts/{id}/")
    suspend fun deletePost(@Path("id") id: Int): Response<ResponseBody>

    @POST("/api/forum/posts/{postId}/vote/")
    suspend fun vote(
        @Path("postId") postId: Int,
        @Body body: VoteRequest
    ): Response<VoteResponse>

    @POST("/api/forum/posts/{postId}/repost/")
    suspend fun repost(
        @Path("postId") postId: Int,
        @Body body: RepostRequest
    ): Response<Post>

    @POST("/api/forum/posts/{postId}/report/")
    suspend fun reportPost(
        @Path("postId") postId: Int,
        @Body body: ReportRequest
    ): Response<ResponseBody>

    @Multipart
    @POST("/api/forum/upload/")
    suspend fun uploadImages(
        @Part images: List<MultipartBody.Part>
    ): Response<UploadImagesResponse>

    @Multipart
    @POST("/api/help-requests/upload/")
    suspend fun uploadHelpRequestImages(
        @Part images: List<MultipartBody.Part>
    ): Response<UploadImagesResponse>

    @POST("/api/accounts/fcm-token/")
    suspend fun updateFcmToken(@Body body: FcmTokenRequest): Response<Void>

    // ── Profile ─────────────────────────────────────────────────────────────────

    @GET("/profile")
    suspend fun getProfile(): Response<ProfileData>

    @PATCH("/profile")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): Response<ProfileData>

    // ── Resources ───────────────────────────────────────────────────────────────

    @GET("/resources")
    suspend fun getResources(): Response<List<ResourceData>>

    @POST("/resources")
    suspend fun createResource(@Body body: ResourceCreateRequest): Response<ResourceData>

    @DELETE("/resources/{id}")
    suspend fun deleteResource(@Path("id") id: Int): Response<Unit>

    // ── Expertise Fields ────────────────────────────────────────────────────────

    @GET("/expertise")
    suspend fun getExpertiseFields(): Response<List<ExpertiseFieldData>>

    @POST("/expertise")
    suspend fun createExpertiseField(@Body body: ExpertiseFieldCreateRequest): Response<ExpertiseFieldData>

    @DELETE("/expertise/{id}")
    suspend fun deleteExpertiseField(@Path("id") id: Int): Response<Unit>
}
