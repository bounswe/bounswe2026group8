package com.bounswe2026group8.emergencyhub.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit interface for the backend auth API.
 * Matches the same 4 endpoints used by the React frontend.
 */
interface ApiService {

    @POST("/register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

    @POST("/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("/logout")
    suspend fun logout(): Response<LogoutResponse>

    @GET("/me")
    suspend fun getMe(): Response<MeResponse>
}
