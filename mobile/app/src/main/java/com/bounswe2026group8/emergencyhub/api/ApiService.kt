package com.bounswe2026group8.emergencyhub.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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
}
