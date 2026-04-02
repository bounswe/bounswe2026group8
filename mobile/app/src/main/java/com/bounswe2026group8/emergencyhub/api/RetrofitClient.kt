package com.bounswe2026group8.emergencyhub.api

import android.content.Context
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton providing the configured Retrofit [ApiService] instance.
 *
 * Uses OkHttp interceptors to:
 *   1. Automatically attach `Authorization: Bearer <token>` when a token exists
 *   2. Log HTTP traffic for debugging
 *
 * The base URL defaults to 10.0.2.2:8000 which is the Android emulator alias
 * for the host machine's localhost. Change this if testing on a physical device.
 */
object RetrofitClient {

    // For emulator → host-machine localhost
    private const val BASE_URL = "http://10.0.2.2:8000"

    private var apiService: ApiService? = null

    fun getService(context: Context): ApiService {
        if (apiService == null) {
            val tokenManager = TokenManager(context)

            // Interceptor: attach Bearer token to every request if available
            val authInterceptor = Interceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                tokenManager.getToken()?.let { token ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }

            // Logging interceptor for debug builds
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()

            apiService = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
        return apiService!!
    }
}
