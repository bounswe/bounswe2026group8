package com.bounswe2026group8.emergencyhub.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Manages app locale (language) persistence and switching.
 * Uses SharedPreferences to store the selected language code.
 * Uses AppCompatDelegate.setApplicationLocales() to apply the locale app-wide.
 */
object LocaleManager {

    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LANGUAGE = "language"
    
    // Supported language codes
    const val LANGUAGE_EN = "en"
    const val LANGUAGE_TR = "tr"
    const val LANGUAGE_ES = "es"
    const val LANGUAGE_ZH = "zh"
    
    // Default language
    const val DEFAULT_LANGUAGE = LANGUAGE_EN

    // Map language codes to locale tags (for AppCompatDelegate)
    private val languageToLocale = mapOf(
        LANGUAGE_EN to "en",
        LANGUAGE_TR to "tr",
        LANGUAGE_ES to "es",
        LANGUAGE_ZH to "zh"
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Get the currently saved language code.
     * Returns DEFAULT_LANGUAGE if none saved.
     */
    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    /**
     * Save the selected language code to SharedPreferences.
     */
    fun setLanguage(context: Context, languageCode: String) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    /**
     * Apply the saved language to the app using AppCompatDelegate.
     * This should be called on app startup (in Application or MainActivity).
     */
    fun applySavedLocale(context: Context) {
        val languageCode = getLanguage(context)
        applyLocale(context, languageCode)
    }

    /**
     * Apply a specific language to the app.
     * @param languageCode The language code to apply (en, tr, es, zh)
     */
    fun applyLocale(context: Context, languageCode: String) {
        val localeTag = languageToLocale[languageCode] ?: DEFAULT_LANGUAGE
        val localeList = LocaleListCompat.forLanguageTags(localeTag)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * Get display name for a language code.
     */
    fun getDisplayName(languageCode: String): String {
        return when (languageCode) {
            LANGUAGE_EN -> "EN"
            LANGUAGE_TR -> "TR"
            LANGUAGE_ES -> "ES"
            LANGUAGE_ZH -> "ZH"
            else -> "EN"
        }
    }

    /**
     * Get all supported language codes.
     */
    fun getSupportedLanguages(): List<String> {
        return listOf(LANGUAGE_EN, LANGUAGE_TR, LANGUAGE_ES, LANGUAGE_ZH)
    }
}