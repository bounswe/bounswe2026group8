package com.bounswe2026group8.emergencyhub.mesh.voice

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.mesh.ui.MeshVoiceSettingsActivity
import com.bounswe2026group8.emergencyhub.util.LocaleManager

/**
 * Binds Vosk-based voice dictation to a single (EditText, ImageButton) pair.
 *
 * UI states the mic button can show:
 *   1. Hidden        — feature off OR model not installed for active language
 *      AND there is no model installed for any other language.
 *      Practically: see [refresh] for the precise rules below.
 *   2. Download icon — feature on, but no model installed for active language.
 *      Tap → confirmation dialog → opens MeshVoiceSettingsActivity.
 *   3. Mic icon      — feature on, model installed. Tap → start dictation.
 *   4. Stop icon     — actively dictating. Tap → stop and commit final text.
 *
 * The "active language" is taken from [LocaleManager.getLanguage] — i.e.
 * whatever the user picked for the rest of the app, not the OS locale. This
 * matches their mental model: if they set the app to Turkish they expect
 * voice input to also be Turkish.
 *
 * One binding owns one [VoskRecognizer]. Multiple bindings on the same
 * screen (e.g. title + body in MeshCreatePostActivity) each get their own
 * recognizer; only one is active at a time because we cancel the active
 * one whenever a different binding's mic is tapped.
 */
class MeshVoiceBinding private constructor(
    private val activity: AppCompatActivity,
    private val editText: EditText,
    private val micButton: ImageButton,
) : DefaultLifecycleObserver {

    private var recognizer: VoskRecognizer? = null
    private var listening = false
    private var baseTextBeforeListening: String = ""
    private var pendingMicTap: Boolean = false

    private val recordPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingMicTap) {
            pendingMicTap = false
            startListeningInternal()
        } else if (!granted) {
            pendingMicTap = false
            Toast.makeText(
                activity, R.string.mesh_voice_record_permission_needed, Toast.LENGTH_SHORT
            ).show()
        }
    }

    init {
        activity.lifecycle.addObserver(this)
        micButton.setOnClickListener { onMicTap() }
        refresh()
    }

    override fun onPause(owner: LifecycleOwner) {
        // A SpeechService that lives across onPause keeps the mic open in
        // the background — bad for battery and creepy. Kill it on every
        // pause; the user can re-tap when they come back.
        if (listening) finishListening(commitFinal = false)
        recognizer?.destroy()
        recognizer = null
    }

    override fun onDestroy(owner: LifecycleOwner) {
        recognizer?.destroy()
        recognizer = null
    }

    /**
     * Re-evaluate visibility/icon based on current settings + disk state.
     * Cheap to call after `onResume` or after the user comes back from
     * MeshVoiceSettingsActivity.
     */
    fun refresh() {
        val ctx = activity
        val enabled = MeshVoicePrefs.isEnabled(ctx)
        if (!enabled) {
            micButton.visibility = View.GONE
            return
        }
        val language = activeLanguage(ctx)
        if (language == null) {
            // App is in a language Vosk doesn't list — hide rather than
            // show a confusing button that can't ever be useful.
            micButton.visibility = View.GONE
            return
        }
        micButton.visibility = View.VISIBLE

        if (listening) {
            micButton.setImageResource(R.drawable.ic_mesh_mic_active)
            micButton.contentDescription = ctx.getString(R.string.mesh_voice_stop_cd)
        } else if (VoskModelManager.isInstalled(ctx, language)) {
            micButton.setImageResource(R.drawable.ic_mesh_mic)
            micButton.contentDescription = ctx.getString(R.string.mesh_voice_start_cd)
        } else {
            micButton.setImageResource(R.drawable.ic_mesh_mic_download)
            micButton.contentDescription = ctx.getString(R.string.mesh_voice_download_cd)
        }
    }

    private fun onMicTap() {
        val ctx = activity
        val language = activeLanguage(ctx)
        if (!MeshVoicePrefs.isEnabled(ctx) || language == null) {
            // Defensive: the button should already be hidden in this state
            // but a stale tap could still race here.
            refresh()
            return
        }

        if (!VoskModelManager.isInstalled(ctx, language)) {
            promptDownload(language)
            return
        }

        if (listening) {
            finishListening(commitFinal = true)
            return
        }

        // Need RECORD_AUDIO before we can hand the mic to Vosk's SpeechService.
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pendingMicTap = true
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        startListeningInternal()
    }

    private fun startListeningInternal() {
        val ctx = activity
        val language = activeLanguage(ctx) ?: return

        // Only one binding may listen at a time.
        ActiveBindingRegistry.cancelOthers(this)

        val rec = recognizer ?: VoskRecognizer(ctx, language).also { recognizer = it }

        baseTextBeforeListening = editText.text?.toString()?.trimEnd().orEmpty()
        listening = true
        ActiveBindingRegistry.setActive(this)
        refresh()

        editText.requestFocus()

        val started = rec.start(object : VoskRecognizer.Callbacks {
            override fun onPartial(text: String) {
                activity.runOnUiThread { applyTranscript(text, isFinal = false) }
            }

            override fun onFinal(text: String) {
                activity.runOnUiThread {
                    applyTranscript(text, isFinal = true)
                    finishListening(commitFinal = false)
                }
            }

            override fun onError(message: String) {
                activity.runOnUiThread {
                    finishListening(commitFinal = false)
                    val msgRes = when (message) {
                        "model_not_installed" -> R.string.mesh_voice_model_missing
                        "model_load_failed" -> R.string.mesh_voice_model_load_failed
                        else -> R.string.mesh_voice_recognizer_error
                    }
                    Toast.makeText(activity, msgRes, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onTimeout() {
                activity.runOnUiThread { finishListening(commitFinal = false) }
            }
        })

        if (!started) {
            // start() already toasted via onError; just reset the UI state.
            finishListening(commitFinal = false)
        }
    }

    private fun applyTranscript(transcript: String, isFinal: Boolean) {
        if (transcript.isBlank()) return
        val merged = mergeText(baseTextBeforeListening, transcript)
        editText.setText(merged)
        editText.setSelection(merged.length)
        if (isFinal) {
            // Make the just-finalised text the new "base" so subsequent
            // partials don't overwrite it.
            baseTextBeforeListening = merged.trimEnd()
        }
    }

    private fun finishListening(commitFinal: Boolean) {
        if (listening) {
            if (commitFinal) recognizer?.stop() else recognizer?.cancel()
        }
        listening = false
        if (ActiveBindingRegistry.isActive(this)) ActiveBindingRegistry.clear()
        refresh()
    }

    internal fun cancelFromOtherBinding() {
        if (listening) finishListening(commitFinal = false)
    }

    private fun promptDownload(language: VoskModelManager.Language) {
        val ctx = activity
        val displayName = languageDisplayName(ctx, language)
        AlertDialog.Builder(ctx)
            .setTitle(R.string.mesh_voice_download_prompt_title)
            .setMessage(
                ctx.getString(
                    R.string.mesh_voice_download_prompt_body,
                    displayName,
                    language.approxMb
                )
            )
            .setPositiveButton(R.string.mesh_voice_download_prompt_open_settings) { _, _ ->
                ctx.startActivity(Intent(ctx, MeshVoiceSettingsActivity::class.java))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun mergeText(existing: String, transcript: String): String {
        if (existing.isBlank()) return transcript
        val sep = if (existing.last().isWhitespace()) "" else " "
        return existing + sep + transcript
    }

    companion object {
        /**
         * Convenience constructor. Returns the binding so the caller can
         * keep a reference and call [refresh] from `onResume`.
         */
        fun bind(
            activity: AppCompatActivity,
            editText: EditText,
            micButton: ImageButton,
        ): MeshVoiceBinding = MeshVoiceBinding(activity, editText, micButton)

        /**
         * Map the user's app-locale code to a VoskModelManager.Language, or
         * null if we don't ship a model for that locale.
         */
        fun activeLanguage(context: Context): VoskModelManager.Language? {
            val code = LocaleManager.getLanguage(context)
            return VoskModelManager.Language.fromCode(code)
        }

        fun languageDisplayName(
            context: Context,
            language: VoskModelManager.Language,
        ): String {
            val resId = when (language) {
                VoskModelManager.Language.EN -> R.string.mesh_voice_lang_en
                VoskModelManager.Language.TR -> R.string.mesh_voice_lang_tr
                VoskModelManager.Language.ES -> R.string.mesh_voice_lang_es
                VoskModelManager.Language.ZH -> R.string.mesh_voice_lang_zh
            }
            return context.getString(resId)
        }
    }
}

/**
 * Per-process registry that ensures only one [MeshVoiceBinding] is dictating
 * at a time. Keeps mic resources from getting fought over when, e.g., the
 * user taps the body mic while the title is still listening.
 */
private object ActiveBindingRegistry {
    private var active: MeshVoiceBinding? = null

    fun setActive(b: MeshVoiceBinding) {
        active = b
    }

    fun cancelOthers(b: MeshVoiceBinding) {
        val other = active
        if (other != null && other !== b) {
            other.cancelFromOtherBinding()
        }
    }

    fun isActive(b: MeshVoiceBinding): Boolean = active === b
    fun clear() {
        active = null
    }
}
