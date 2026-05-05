package com.bounswe2026group8.emergencyhub.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the backend API.
 * Covers auth, help-requests, help-offers, and forum endpoints.
 *
 * All paths are relative (no leading slash) so Retrofit resolves them against
 * BASE_URL correctly:
 *   - debug:   http://10.0.2.2:8000/         → Django at root
 *   - release: https://emergencyhub.duckdns.org/api/ → nginx strips /api/ → Django
 */
interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────

    @POST("register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

    @POST("login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("logout")
    suspend fun logout(): Response<LogoutResponse>

    @GET("me")
    suspend fun getMe(): Response<MeResponse>

    @PATCH("me")
    suspend fun updateMe(@Body body: UpdateMeRequest): Response<MeResponse>

    // ── Help Requests ────────────────────────────────────────────────────

    @GET("help-requests/")
    suspend fun getHelpRequests(
        @Query("hub_id") hubId: Int? = null,
        @Query("category") category: String? = null
    ): Response<List<HelpRequestItem>>

    @POST("help-requests/")
    suspend fun createHelpRequest(
        @Body body: CreateHelpRequest
    ): Response<HelpRequestDetail>

    @GET("help-requests/{id}/")
    suspend fun getHelpRequestDetail(
        @Path("id") id: Int
    ): Response<HelpRequestDetail>

    @DELETE("help-requests/{id}/")
    suspend fun deleteHelpRequest(
        @Path("id") id: Int
    ): Response<ResponseBody?>

    @PATCH("help-requests/{id}/status/")
    suspend fun updateHelpRequestStatus(
        @Path("id") id: Int,
        @Body body: UpdateHelpRequestStatusRequest
    ): Response<HelpRequestDetail>

    // ── Help Request Comments ────────────────────────────────────────────

    @GET("help-requests/{id}/comments/")
    suspend fun getHelpRequestComments(
        @Path("id") requestId: Int
    ): Response<List<HelpRequestComment>>

    @POST("help-requests/{id}/comments/")
    suspend fun createHelpRequestComment(
        @Path("id") requestId: Int,
        @Body body: CreateCommentRequest
    ): Response<HelpRequestComment>

    @DELETE("help-requests/comments/{id}/")
    suspend fun deleteHelpRequestComment(
        @Path("id") commentId: Int
    ): Response<ResponseBody?>

    // ── Help Offers ──────────────────────────────────────────────────────

    @GET("help-offers/")
    suspend fun getHelpOffers(
        @Query("hub_id") hubId: Int? = null,
        @Query("category") category: String? = null
    ): Response<List<HelpOfferItem>>

    @POST("help-offers/")
    suspend fun createHelpOffer(
        @Body body: CreateHelpOffer
    ): Response<HelpOfferItem>

    /** Returns Response<ResponseBody?> to avoid Gson parsing the empty 204 body. */
    @DELETE("help-offers/{id}/")
    suspend fun deleteHelpOffer(
        @Path("id") id: Int
    ): Response<ResponseBody?>

    // ── Hubs ─────────────────────────────────────────────────────────────

    @GET("hubs/")
    suspend fun getHubs(): Response<List<Hub>>

    // ── Forum ────────────────────────────────────────────────────────────

    @GET("forum/posts/")
    suspend fun getPosts(
        @Query("forum_type") forumType: String,
        @Query("hub") hub: Int? = null
    ): Response<List<Post>>

    @POST("forum/posts/")
    suspend fun createPost(@Body body: CreatePostRequest): Response<Post>

    @GET("forum/posts/{id}/")
    suspend fun getPost(@Path("id") id: Int): Response<Post>

    @GET("forum/posts/{postId}/comments/")
    suspend fun getComments(@Path("postId") postId: Int): Response<List<Comment>>

    @POST("forum/posts/{postId}/comments/")
    suspend fun createComment(
        @Path("postId") postId: Int,
        @Body body: CreateCommentRequest
    ): Response<Comment>

    @DELETE("forum/comments/{id}/")
    suspend fun deleteComment(@Path("id") id: Int): Response<ResponseBody?>

    @DELETE("forum/posts/{id}/")
    suspend fun deletePost(@Path("id") id: Int): Response<ResponseBody?>

    @POST("forum/posts/{postId}/vote/")
    suspend fun vote(
        @Path("postId") postId: Int,
        @Body body: VoteRequest
    ): Response<VoteResponse>

    @POST("forum/posts/{postId}/repost/")
    suspend fun repost(
        @Path("postId") postId: Int,
        @Body body: RepostRequest
    ): Response<Post>

    @POST("forum/posts/{postId}/report/")
    suspend fun reportPost(
        @Path("postId") postId: Int,
        @Body body: ReportRequest
    ): Response<ResponseBody>

    @Multipart
    @POST("forum/upload/")
    suspend fun uploadImages(
        @Part images: List<MultipartBody.Part>
    ): Response<UploadImagesResponse>

    @Multipart
    @POST("help-requests/upload/")
    suspend fun uploadHelpRequestImages(
        @Part images: List<MultipartBody.Part>
    ): Response<UploadImagesResponse>

    @POST("accounts/fcm-token/")
    suspend fun updateFcmToken(@Body body: FcmTokenRequest): Response<Void>

    // ── Profile ─────────────────────────────────────────────────────────────────

    @GET("profile")
    suspend fun getProfile(): Response<ProfileData>

    @PATCH("profile")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): Response<ProfileData>

    // ── Resources ───────────────────────────────────────────────────────────────

    @GET("resources")
    suspend fun getResources(): Response<List<ResourceData>>

    @POST("resources")
    suspend fun createResource(@Body body: ResourceCreateRequest): Response<ResourceData>

    @DELETE("resources/{id}")
    suspend fun deleteResource(@Path("id") id: Int): Response<Unit>

    // ── Expertise Fields ────────────────────────────────────────────────────────

    @GET("expertise")
    suspend fun getExpertiseFields(): Response<List<ExpertiseFieldData>>

    @POST("expertise")
    suspend fun createExpertiseField(@Body body: ExpertiseFieldCreateRequest): Response<ExpertiseFieldData>

    @DELETE("expertise/{id}")
    suspend fun deleteExpertiseField(@Path("id") id: Int): Response<Unit>

    // ── Staff: Admin (user management) ─────────────────────────────────────────

    @GET("staff/users/")
    suspend fun listStaffUsers(
        @Query("search") search: String? = null,
        @Query("staff_role") staffRole: String? = null,
        @Query("is_active") isActive: Boolean? = null,
    ): Response<List<StaffUserListItem>>

    @PATCH("staff/users/{id}/staff-role/")
    suspend fun updateStaffRole(
        @Path("id") userId: Int,
        @Body body: StaffRoleUpdateRequest,
    ): Response<StaffUserListItem>

    @PATCH("staff/users/{id}/status/")
    suspend fun updateAccountStatus(
        @Path("id") userId: Int,
        @Body body: AccountStatusUpdateRequest,
    ): Response<StaffUserListItem>

    // ── Staff: Forum moderation (mod / admin) ──────────────────────────────────

    @GET("forum/moderation/posts/")
    suspend fun listForumModerationPosts(
        @Query("status") status: String? = null,
    ): Response<List<ForumModerationPost>>

    @PATCH("forum/posts/{id}/moderation/")
    suspend fun moderateForumPost(
        @Path("id") postId: Int,
        @Body body: ForumModerationActionRequest,
    ): Response<ForumModerationPost>

    // ── Staff: Expertise verification (verifier / admin) ───────────────────────

    @GET("staff/expertise-verifications/")
    suspend fun listExpertiseVerifications(
        @Query("status") status: String? = null,
    ): Response<List<ExpertiseVerificationItem>>

    @PATCH("staff/expertise-verifications/{id}/decision/")
    suspend fun decideExpertiseVerification(
        @Path("id") expertiseId: Int,
        @Body body: ExpertiseDecisionRequest,
    ): Response<ExpertiseVerificationItem>

    // ── Staff: Hubs (admin) ────────────────────────────────────────────────────

    @GET("staff/hubs/")
    suspend fun listStaffHubs(): Response<List<Hub>>

    @POST("staff/hubs/")
    suspend fun createStaffHub(@Body body: HubCreateRequest): Response<Hub>

    @PATCH("staff/hubs/{id}/")
    suspend fun updateStaffHub(
        @Path("id") hubId: Int,
        @Body body: HubUpdateRequest,
    ): Response<Hub>

    /**
     * Hub deletion requires `confirm: true` per the backend safeguard, so we
     * route through @HTTP to send a JSON body with DELETE.
     */
    @HTTP(method = "DELETE", path = "staff/hubs/{id}/", hasBody = true)
    suspend fun deleteStaffHub(
        @Path("id") hubId: Int,
        @Body body: HubDeleteRequest,
    ): Response<ResponseBody?>

    // ── Staff: Audit log (admin) ───────────────────────────────────────────────

    @GET("staff/audit-logs/")
    suspend fun listAuditLogs(
        @Query("action") action: String? = null,
        @Query("target_type") targetType: String? = null,
    ): Response<List<AuditLogItem>>

    // ── Staff: Help moderation (mod / admin) ───────────────────────────────────

    @GET("help-requests/moderation/")
    suspend fun listHelpRequestModeration(): Response<List<HelpRequestModerationItem>>

    @GET("help-offers/moderation/")
    suspend fun listHelpOfferModeration(): Response<List<HelpOfferModerationItem>>

    /** DELETE with body so moderators can attach an audit-log reason. */
    @HTTP(method = "DELETE", path = "help-requests/{id}/", hasBody = true)
    suspend fun moderationDeleteHelpRequest(
        @Path("id") id: Int,
        @Body body: ModerationDeleteRequest,
    ): Response<ResponseBody?>

    @HTTP(method = "DELETE", path = "help-offers/{id}/", hasBody = true)
    suspend fun moderationDeleteHelpOffer(
        @Path("id") id: Int,
        @Body body: ModerationDeleteRequest,
    ): Response<ResponseBody?>
}
