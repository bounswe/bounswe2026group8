package com.bounswe2026group8.emergencyhub.mesh.voice

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.IOException

/**
 * Thin Kotlin wrapper around Vosk's `Model` + `SpeechService`. Owned 1:1 by
 * the activity that's currently dictating; created on first [start], freed
 * on [destroy] (or activity `onPause`). We deliberately do NOT memoize
 * [Model] across activities — it's ~50 MB of mmapped data and we'd rather
 * pay the ~100ms re-init than hold it forever.
 *
 * Vosk emits JSON; we extract the `text` field for finals and `partial` for
 * partials and forward them as plain Strings to the caller.
 */
class VoskRecognizer(
    private val context: Context,
    private val language: VoskModelManager.Language,
) {

    interface Callbacks {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String)
        fun onTimeout()
    }

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var callbacks: Callbacks? = null

    /** Lazily loads the model from disk. Returns false if not installed. */
    fun start(callbacks: Callbacks): Boolean {
        if (!VoskModelManager.isInstalled(context, language)) {
            Log.w(TAG, "start() called for ${language.code} but model not installed")
            callbacks.onError("model_not_installed")
            return false
        }
        stop()
        this.callbacks = callbacks
        return try {
            val dir = VoskModelManager.modelDir(context, language)
            val loadedModel = Model(dir.absolutePath)
            model = loadedModel
            // Vosk's Recognizer wants the sample rate it was trained on. The
            // small models are all 16 kHz; SpeechService records at the same
            // rate by default.
            val recognizer = Recognizer(loadedModel, 16_000.0f)
            val service = SpeechService(recognizer, 16_000.0f)
            service.startListening(VoskListener(callbacks))
            speechService = service
            true
        } catch (t: IOException) {
            Log.w(TAG, "Failed to load Vosk model from disk", t)
            callbacks.onError("model_load_failed")
            destroy()
            false
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to start Vosk recognizer", t)
            callbacks.onError("recognizer_start_failed")
            destroy()
            false
        }
    }

    fun stop() {
        speechService?.stop()
    }

    fun cancel() {
        speechService?.cancel()
    }

    fun destroy() {
        try {
            speechService?.shutdown()
        } catch (_: Throwable) {
            // Vosk has been known to throw on double-shutdown; we don't care.
        }
        speechService = null
        try {
            model?.close()
        } catch (_: Throwable) {
        }
        model = null
        callbacks = null
    }

    private class VoskListener(private val cb: Callbacks) : RecognitionListener {
        override fun onPartialResult(hypothesis: String?) {
            // Vosk emits e.g. {"partial":"hello world"}
            val text = extract(hypothesis, "partial")
            if (text.isNotBlank()) cb.onPartial(text)
        }

        override fun onResult(hypothesis: String?) {
            // Vosk emits e.g. {"text":"hello world"}
            val text = extract(hypothesis, "text")
            if (text.isNotBlank()) cb.onFinal(text)
        }

        override fun onFinalResult(hypothesis: String?) {
            val text = extract(hypothesis, "text")
            if (text.isNotBlank()) cb.onFinal(text)
        }

        override fun onError(exception: Exception?) {
            cb.onError(exception?.message ?: "vosk_error")
        }

        override fun onTimeout() {
            cb.onTimeout()
        }

        private fun extract(json: String?, key: String): String {
            if (json.isNullOrBlank()) return ""
            return try {
                JSONObject(json).optString(key, "").trim()
            } catch (_: Throwable) {
                ""
            }
        }
    }

    companion object {
        private const val TAG = "VoskRecognizer"
    }
}
