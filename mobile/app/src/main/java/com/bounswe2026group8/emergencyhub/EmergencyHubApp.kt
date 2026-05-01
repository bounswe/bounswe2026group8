package com.bounswe2026group8.emergencyhub

import android.app.Application
import com.bounswe2026group8.emergencyhub.util.LocaleManager

/**
 * Application class to initialize app-wide settings.
 * Applies the saved locale on app startup.
 */
class EmergencyHubApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Apply saved locale on app startup
        LocaleManager.applySavedLocale(this)
    }
}