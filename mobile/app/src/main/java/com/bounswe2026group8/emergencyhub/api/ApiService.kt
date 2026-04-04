package com.bounswe2026group8.emergencyhub.api

import okhttp3.MultipartBody
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

interface ApiService {

    @POST("/register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

    @POST("/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("/logout")
    suspend fun logout(): Response<LogoutResponse>

    @GET("/me")
    suspend fun getMe(): Response<MeResponse>

    @GET("/hubs/")
    suspend fun getHubs(): Response<List<Hub>>

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
    suspend fun deleteComment(@Path("id") id: Int): Response<Unit>

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

    @Multipart
    @POST("/forum/upload/")
    suspend fun uploadImages(
        @Part images: List<MultipartBody.Part>
    ): Response<UploadImagesResponse>

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
