package com.bounswe2026group8.emergencyhub.mesh.voice

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences-backed flags for the *opt-in* offline-messages voice
 * input feature. We deliberately keep this in its own preferences file
 * (`mesh_voice_prefs`) so it's easy to wipe / inspect in isolation.
 *
 * Per-language readiness is NOT tracked here — that's derived from disk
 * presence by [VoskModelManager.isInstalled]. Storing it twice would invite
 * drift (e.g. user manually clears app data and prefs lie about state).
 */
object MeshVoicePrefs {

    private const val PREFS = "mesh_voice_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_ALLOW_METERED = "allow_metered"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Master switch. When false, the mic button is hidden everywhere in the
     * mesh / offline-messages UI and no model download UI is offered.
     * Default: false (the user MUST opt in explicitly).
     */
    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * If false, model downloads are constrained to UNMETERED networks (Wi-Fi).
     * Important: a 40 MB download over cellular in a low-connectivity region
     * is a hostile default for emergency users, hence opt-in.
     */
    fun allowMetered(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ALLOW_METERED, false)

    fun setAllowMetered(context: Context, allow: Boolean) {
        prefs(context).edit().putBoolean(KEY_ALLOW_METERED, allow).apply()
    }
}
