package com.bounswe2026group8.emergencyhub.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the backend API.
 * Covers auth endpoints and help-requests endpoints.
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

    // ── Comments ─────────────────────────────────────────────────────────

    @GET("/help-requests/{id}/comments/")
    suspend fun getHelpRequestComments(
        @Path("id") requestId: Int
    ): Response<List<Comment>>

    @POST("/help-requests/{id}/comments/")
    suspend fun createComment(
        @Path("id") requestId: Int,
        @Body body: CreateCommentRequest
    ): Response<Comment>

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
}
