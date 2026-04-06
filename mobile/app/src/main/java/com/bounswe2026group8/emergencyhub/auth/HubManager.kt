package com.bounswe2026group8.emergencyhub.auth

import android.content.Context
import android.content.SharedPreferences
import com.bounswe2026group8.emergencyhub.api.Hub
import com.google.gson.Gson

class HubManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("emergency_hub_selection", Context.MODE_PRIVATE)

    private val gson = Gson()

    companion object {
        private const val KEY_SELECTED_HUB = "selected_hub"
    }

    fun saveSelectedHub(hub: Hub) {
        prefs.edit().putString(KEY_SELECTED_HUB, gson.toJson(hub)).apply()
    }

    fun getSelectedHub(): Hub? {
        val json = prefs.getString(KEY_SELECTED_HUB, null) ?: return null
        return try {
            gson.fromJson(json, Hub::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun clearSelectedHub() {
        prefs.edit().remove(KEY_SELECTED_HUB).apply()
    }
}
