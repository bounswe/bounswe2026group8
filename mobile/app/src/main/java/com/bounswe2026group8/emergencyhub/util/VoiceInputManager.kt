package com.bounswe2026group8.emergencyhub.util

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bounswe2026group8.emergencyhub.R
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

class VoiceInputManager(private val activity: AppCompatActivity) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "VoiceInputManager"
    }

    private data class VoiceBinding(
        val editText: EditText,
        val updateUi: (Boolean) -> Unit,
    )

    private val bindings = mutableMapOf<EditText, VoiceBinding>()
    private val permissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val pendingTarget = pendingPermissionTarget
            pendingPermissionTarget = null
            if (granted && pendingTarget != null) {
                startListening(pendingTarget)
            } else if (!granted) {
                toast(R.string.voice_input_permission_denied)
            }
        }

    private var recognizer: SpeechRecognizer? = null
    private var activeBinding: VoiceBinding? = null
    private var pendingPermissionTarget: EditText? = null
    private var baseTextBeforeListening = ""
    private var lastTranscript = ""
    private var ignoreNextClientError = false
    private var usingOnDeviceRecognizer = false
    private var attemptedSystemFallback = false

    init {
        activity.lifecycle.addObserver(this)
    }

    fun bind(vararg editTexts: EditText) {
        editTexts.forEach(::bind)
    }

    fun bind(editText: EditText) {
        val inputLayout = findParentTextInputLayout(editText) ?: return
        val useStartIcon = inputLayout.endIconMode != TextInputLayout.END_ICON_NONE

        val binding = VoiceBinding(
            editText = editText,
            updateUi = { listening ->
                val iconRes = if (listening) R.drawable.ic_voice_stop else R.drawable.ic_voice_mic
                val description = activity.getString(
                    if (listening) R.string.voice_input_stop else R.string.voice_input_start
                )

                if (useStartIcon) {
                    inputLayout.setStartIconDrawable(iconRes)
                    inputLayout.startIconContentDescription = description
                    inputLayout.isStartIconVisible = true
                } else {
                    inputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
                    inputLayout.setEndIconDrawable(iconRes)
                    inputLayout.endIconContentDescription = description
                }
            }
        )

        if (useStartIcon) {
            inputLayout.setStartIconOnClickListener { toggleListening(editText) }
            inputLayout.isStartIconVisible = true
        } else {
            inputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
            inputLayout.setEndIconOnClickListener { toggleListening(editText) }
        }

        bindings[editText] = binding
        binding.updateUi(false)
    }

    fun bind(editText: EditText, button: ImageButton) {
        val binding = VoiceBinding(
            editText = editText,
            updateUi = { listening ->
                val iconRes = if (listening) R.drawable.ic_voice_stop else R.drawable.ic_voice_mic
                val tintRes = if (listening) R.color.accent else R.color.text_muted
                button.setImageDrawable(AppCompatResources.getDrawable(activity, iconRes))
                button.contentDescription = activity.getString(
                    if (listening) R.string.voice_input_stop else R.string.voice_input_start
                )
                button.imageTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(activity, tintRes)
                )
            }
        )

        button.setOnClickListener { toggleListening(editText) }
        bindings[editText] = binding
        binding.updateUi(false)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        destroy()
    }

    fun destroy() {
        bindings.values.forEach { it.updateUi(false) }
        recognizer?.destroy()
        recognizer = null
        activeBinding = null
        pendingPermissionTarget = null
        baseTextBeforeListening = ""
        lastTranscript = ""
        attemptedSystemFallback = false
    }

    private fun toggleListening(editText: EditText) {
        if (activeBinding?.editText == editText) {
            cancelActiveRecognition()
            return
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pendingPermissionTarget = editText
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        startListening(editText)
    }

    private fun startListening(editText: EditText) {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            toast(R.string.voice_input_unavailable)
            return
        }

        cancelActiveRecognition()
        attemptedSystemFallback = false
        startListeningWithMode(
            editText = editText,
            preferOnDevice = shouldPreferOnDevice(),
            preserveSession = false,
        )
    }

    private fun startListeningWithMode(
        editText: EditText,
        preferOnDevice: Boolean,
        preserveSession: Boolean,
    ) {
        val binding = bindings[editText] ?: return
        val speechRecognizer = getOrCreateRecognizer(preferOnDevice)
            ?: if (preferOnDevice && !attemptedSystemFallback) {
                attemptedSystemFallback = true
                startListeningWithMode(editText, preferOnDevice = false, preserveSession = preserveSession)
                return
            } else {
                finishRecognitionSession()
                toast(R.string.voice_input_unavailable)
                return
            }

        if (!preserveSession) {
            activeBinding = binding
            baseTextBeforeListening = editText.text?.toString()?.trimEnd().orEmpty()
            lastTranscript = ""
            binding.updateUi(true)
        }

        editText.requestFocus()
        Log.d(
            TAG,
            "startListening preferOnDevice=$preferOnDevice preserveSession=$preserveSession"
        )

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Only force offline mode when we're intentionally using the on-device recognizer.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOnDevice)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1_500L)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activity.packageName)
        }

        try {
            speechRecognizer.startListening(intent)
        } catch (exception: Exception) {
            Log.e(TAG, "startListening failed preferOnDevice=$preferOnDevice", exception)
            if (preferOnDevice && !attemptedSystemFallback) {
                attemptedSystemFallback = true
                startListeningWithMode(editText, preferOnDevice = false, preserveSession = preserveSession)
            } else {
                finishRecognitionSession()
                toast(R.string.voice_input_start_failed)
            }
        }
    }

    private fun cancelActiveRecognition() {
        if (activeBinding != null) {
            ignoreNextClientError = true
            recognizer?.cancel()
            finishRecognitionSession()
        }
    }

    private fun finishRecognitionSession() {
        activeBinding?.updateUi?.invoke(false)
        activeBinding = null
        baseTextBeforeListening = ""
        lastTranscript = ""
        attemptedSystemFallback = false
    }

    private fun getOrCreateRecognizer(preferOnDevice: Boolean): SpeechRecognizer? {
        recognizer?.let {
            if (usingOnDeviceRecognizer == preferOnDevice) {
                return it
            }
            it.destroy()
            recognizer = null
        }

        val createdRecognizer = try {
            if (preferOnDevice) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(activity)
            } else {
                SpeechRecognizer.createSpeechRecognizer(activity)
            }
        } catch (_: Exception) {
            null
        }

        if (createdRecognizer == null) {
            toast(R.string.voice_input_unavailable)
            return null
        }

        usingOnDeviceRecognizer = preferOnDevice

        createdRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech usingOnDevice=$usingOnDeviceRecognizer")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                Log.w(
                    TAG,
                    "onError code=$error name=${errorName(error)} usingOnDevice=$usingOnDeviceRecognizer"
                )
                if (ignoreNextClientError && error == SpeechRecognizer.ERROR_CLIENT) {
                    ignoreNextClientError = false
                    return
                }

                if (usingOnDeviceRecognizer && !attemptedSystemFallback && shouldFallbackToSystem(error)) {
                    attemptedSystemFallback = true
                    recognizer?.destroy()
                    recognizer = null
                    activeBinding?.editText?.let {
                        startListeningWithMode(it, preferOnDevice = false, preserveSession = true)
                        return
                    }
                }

                val hadTranscript = lastTranscript.isNotBlank()
                finishRecognitionSession()

                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        if (!hadTranscript) toast(R.string.voice_input_no_match)
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> toast(R.string.voice_input_busy)
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> toast(R.string.voice_input_permission_denied)
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> toast(R.string.voice_input_network_error)
                    else -> toast(R.string.voice_input_failed)
                }
            }

            override fun onResults(results: Bundle?) {
                Log.d(TAG, "onResults transcript=${extractTranscript(results)}")
                applyTranscription(results, isFinal = true)
                finishRecognitionSession()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                Log.d(TAG, "onPartialResults transcript=${extractTranscript(partialResults)}")
                applyTranscription(partialResults, isFinal = false)
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        recognizer = createdRecognizer
        return createdRecognizer
    }

    private fun applyTranscription(results: Bundle?, isFinal: Boolean) {
        val transcript = extractTranscript(results)

        if (transcript.isBlank()) return

        lastTranscript = transcript
        val target = activeBinding?.editText ?: return
        val mergedText = mergeText(baseTextBeforeListening, transcript)
        target.setText(mergedText)
        target.setSelection(mergedText.length)

        if (isFinal) {
            lastTranscript = ""
        }
    }

    private fun mergeText(existing: String, transcript: String): String {
        if (existing.isBlank()) return transcript
        val separator = if (existing.last().isWhitespace()) "" else " "
        return existing + separator + transcript
    }

    private fun extractTranscript(results: Bundle?): String {
        return results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
    }

    private fun shouldPreferOnDevice(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(activity)
    }

    private fun shouldFallbackToSystem(error: Int): Boolean {
        return error == SpeechRecognizer.ERROR_CLIENT ||
            error == SpeechRecognizer.ERROR_SERVER ||
            error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED ||
            error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ||
            error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
            error == SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT ||
            error == SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS
    }

    private fun findParentTextInputLayout(editText: EditText): TextInputLayout? {
        var parent = editText.parent
        while (parent != null) {
            if (parent is TextInputLayout) return parent
            parent = parent.parent
        }
        return null
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(activity, messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun errorName(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
            SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
            SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
            SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
            SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "ERROR_TOO_MANY_REQUESTS"
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "ERROR_SERVER_DISCONNECTED"
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "ERROR_LANGUAGE_NOT_SUPPORTED"
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "ERROR_LANGUAGE_UNAVAILABLE"
            SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> "ERROR_CANNOT_CHECK_SUPPORT"
            SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS ->
                "ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS"
            else -> "UNKNOWN_ERROR"
        }
    }
}
