package com.bounswe2026group8.emergencyhub.api

import android.content.Context
import com.bounswe2026group8.emergencyhub.BuildConfig
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton providing the configured Retrofit [ApiService] instance.
 *
 * BASE_URL is set per build type via BuildConfig:
 *   - debug:   http://10.0.2.2:8000  (Android emulator → localhost)
 *   - release: https://emergencyhub.duckdns.org
 */
object RetrofitClient {

    private val BASE_URL = BuildConfig.BASE_URL

    /**
     * Resolves an image URL so it is loadable from the mobile client.
     * - Relative paths (e.g. "/media/uploads/abc.png") get the base URL prepended.
     * - All other URLs (external) are returned as-is.
     */
    fun resolveImageUrl(url: String): String {
        if (url.startsWith("/")) return "$BASE_URL$url"
        return url
    }

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

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .apply {
                    if (BuildConfig.DEBUG) {
                        addInterceptor(HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        })
                    }
                }
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
