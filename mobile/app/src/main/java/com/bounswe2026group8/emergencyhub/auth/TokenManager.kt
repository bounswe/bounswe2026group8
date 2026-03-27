package com.bounswe2026group8.emergencyhub.auth

import android.content.Context
import android.content.SharedPreferences
import com.bounswe2026group8.emergencyhub.api.UserData
import com.google.gson.Gson

/**
 * Manages JWT token and cached user data via SharedPreferences.
 *
 * After login the access token is stored so the OkHttp interceptor can
 * attach it automatically. User data is cached for quick access on the
 * Dashboard without requiring a /me call every time.
 */
class TokenManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("emergency_hub_prefs", Context.MODE_PRIVATE)

    private val gson = Gson()

    companion object {
        private const val KEY_TOKEN = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_USER = "user_data"
    }

    // ── Token ────────────────────────────────────────────────────────────────────

    fun saveToken(accessToken: String, refreshToken: String? = null) {
        prefs.edit().apply {
            putString(KEY_TOKEN, accessToken)
            refreshToken?.let { putString(KEY_REFRESH, it) }
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    // ── User data cache ──────────────────────────────────────────────────────────

    fun saveUser(user: UserData) {
        prefs.edit().putString(KEY_USER, gson.toJson(user)).apply()
    }

    fun getUser(): UserData? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, UserData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // ── Clear (logout) ───────────────────────────────────────────────────────────

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = getToken() != null
}
