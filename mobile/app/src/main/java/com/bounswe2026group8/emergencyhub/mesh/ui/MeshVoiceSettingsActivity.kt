package com.bounswe2026group8.emergencyhub.mesh.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.mesh.voice.MeshVoiceBinding
import com.bounswe2026group8.emergencyhub.mesh.voice.MeshVoicePrefs
import com.bounswe2026group8.emergencyhub.mesh.voice.VoskDownloadWorker
import com.bounswe2026group8.emergencyhub.mesh.voice.VoskModelManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.UUID

/**
 * Settings screen the user opens from Profile to:
 *   1. Toggle the offline-messages voice-input feature on/off (master flag).
 *   2. Choose whether downloads can use cellular data.
 *   3. Download / re-download / delete each language model individually.
 *
 * Per-language rows observe their `WorkInfo` via [WorkManager] LiveData so
 * the UI keeps updating even if the activity is destroyed and recreated
 * while the download keeps running in the background.
 */
class MeshVoiceSettingsActivity : AppCompatActivity() {

    private lateinit var languageList: LinearLayout
    private val rowBindings = mutableMapOf<VoskModelManager.Language, RowBinding>()
    private val activeObservers = mutableMapOf<UUID, Observer<WorkInfo?>>()

    private data class RowBinding(
        val root: View,
        val name: TextView,
        val status: TextView,
        val action: MaterialButton,
        val progress: ProgressBar,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mesh_voice_settings)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        val switchEnabled = findViewById<MaterialSwitch>(R.id.switchEnabled)
        switchEnabled.isChecked = MeshVoicePrefs.isEnabled(this)
        switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            MeshVoicePrefs.setEnabled(this, isChecked)
            updateLanguagesCardVisibility(isChecked)
        }

        val switchAllowMetered = findViewById<MaterialSwitch>(R.id.switchAllowMetered)
        switchAllowMetered.isChecked = MeshVoicePrefs.allowMetered(this)
        switchAllowMetered.setOnCheckedChangeListener { _, isChecked ->
            MeshVoicePrefs.setAllowMetered(this, isChecked)
        }

        languageList = findViewById(R.id.languageList)
        VoskModelManager.Language.entries.forEach { addLanguageRow(it) }

        findViewById<MaterialButton>(R.id.btnDeleteAll).setOnClickListener {
            confirmDeleteAll()
        }

        updateLanguagesCardVisibility(switchEnabled.isChecked)
    }

    override fun onResume() {
        super.onResume()
        // Re-bind WorkManager observers + recompute on-disk state. This is
        // the entry path after the user came back from a notification, etc.
        rowBindings.keys.forEach { rebindRow(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        // LiveData removeObservers cleans itself, but explicit is friendly.
        activeObservers.clear()
    }

    private fun updateLanguagesCardVisibility(enabled: Boolean) {
        // Even when the master toggle is off we still let users delete a
        // model they previously downloaded — so we show the card always
        // and just disable the per-row download buttons when off.
        rowBindings.forEach { (lang, row) ->
            val installed = VoskModelManager.isInstalled(this, lang)
            row.action.isEnabled = enabled || installed
        }
    }

    private fun addLanguageRow(language: VoskModelManager.Language) {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.item_mesh_voice_language, languageList, false)
        val binding = RowBinding(
            root = view,
            name = view.findViewById(R.id.txtLangName),
            status = view.findViewById(R.id.txtLangStatus),
            action = view.findViewById(R.id.btnAction),
            progress = view.findViewById(R.id.progressDownload),
        )
        binding.name.text = MeshVoiceBinding.languageDisplayName(this, language)
        rowBindings[language] = binding
        languageList.addView(view)
        rebindRow(language)
    }

    /**
     * Refresh a single row from disk + WorkManager state and re-observe its
     * unique work so the UI updates while a download is in progress.
     */
    private fun rebindRow(language: VoskModelManager.Language) {
        val row = rowBindings[language] ?: return
        val installed = VoskModelManager.isInstalled(this, language)

        // Default to "idle" rendering; the WorkInfo observer below will
        // replace this if a download is actually running.
        renderIdle(language, row, installed)

        val infos = VoskDownloadWorker.currentWorkInfos(this, language)
        val live = infos.firstOrNull { !it.state.isFinished }
        if (live != null) {
            observeWork(language, row, live.id)
        }
    }

    private fun renderIdle(
        language: VoskModelManager.Language,
        row: RowBinding,
        installed: Boolean,
    ) {
        row.progress.visibility = View.GONE
        if (installed) {
            val sizeMb = (VoskModelManager.sizeBytes(this, language) / 1024 / 1024).coerceAtLeast(1)
            row.status.text = getString(R.string.mesh_voice_status_ready, sizeMb)
            row.action.text = getString(R.string.mesh_voice_action_delete)
            row.action.setOnClickListener {
                confirmDeleteOne(language)
            }
        } else {
            row.status.text = getString(R.string.mesh_voice_status_not_downloaded, language.approxMb)
            row.action.text = getString(R.string.mesh_voice_action_download)
            row.action.setOnClickListener {
                if (!MeshVoicePrefs.isEnabled(this)) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.mesh_voice_settings_title)
                        .setMessage(R.string.mesh_voice_enable_first)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                    return@setOnClickListener
                }
                startDownload(language, row)
            }
        }
    }

    private fun startDownload(language: VoskModelManager.Language, row: RowBinding) {
        val displayName = MeshVoiceBinding.languageDisplayName(this, language)
        AlertDialog.Builder(this)
            .setTitle(R.string.mesh_voice_download_prompt_title)
            .setMessage(
                getString(
                    R.string.mesh_voice_download_prompt_body,
                    displayName,
                    language.approxMb
                )
            )
            .setPositiveButton(R.string.mesh_voice_action_download) { _, _ ->
                val id = VoskDownloadWorker.enqueue(
                    this, language, MeshVoicePrefs.allowMetered(this)
                )
                renderDownloading(row, fraction = 0f, downloadedMb = 0, totalMb = language.approxMb)
                row.action.setOnClickListener {
                    VoskDownloadWorker.cancel(this, language)
                }
                row.action.text = getString(R.string.cancel)
                observeWork(language, row, id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun observeWork(
        language: VoskModelManager.Language,
        row: RowBinding,
        workId: UUID,
    ) {
        val live = WorkManager.getInstance(this).getWorkInfoByIdLiveData(workId)
        // Replace any prior observer for this id (rebind on rotate, etc.)
        activeObservers[workId]?.let { live.removeObserver(it) }
        val observer = Observer<WorkInfo?> { info ->
            if (info == null) return@Observer
            when (info.state) {
                WorkInfo.State.ENQUEUED -> {
                    row.progress.visibility = View.VISIBLE
                    row.progress.isIndeterminate = true
                    row.status.text = getString(R.string.mesh_voice_status_queued)
                }
                WorkInfo.State.RUNNING -> {
                    val frac = info.progress.getFloat(VoskDownloadWorker.KEY_PROGRESS_FRACTION, -1f)
                    val dl = info.progress.getLong(VoskDownloadWorker.KEY_PROGRESS_DOWNLOADED, 0L)
                    val total = info.progress.getLong(VoskDownloadWorker.KEY_PROGRESS_TOTAL, -1L)
                    val downloadedMb = (dl / 1024 / 1024).toInt()
                    val totalMb = if (total > 0) (total / 1024 / 1024).toInt() else language.approxMb
                    renderDownloading(row, frac, downloadedMb, totalMb)
                }
                WorkInfo.State.SUCCEEDED -> {
                    rebindRow(language)
                }
                WorkInfo.State.FAILED -> {
                    val err = info.outputData.getString(VoskDownloadWorker.KEY_ERROR) ?: "unknown"
                    row.progress.visibility = View.GONE
                    row.status.text = getString(R.string.mesh_voice_status_failed, err)
                    row.action.text = getString(R.string.mesh_voice_action_retry)
                    row.action.setOnClickListener { startDownload(language, row) }
                }
                WorkInfo.State.CANCELLED -> {
                    row.progress.visibility = View.GONE
                    row.status.text = getString(R.string.mesh_voice_status_cancelled)
                    row.action.text = getString(R.string.mesh_voice_action_download)
                    row.action.setOnClickListener { startDownload(language, row) }
                }
                WorkInfo.State.BLOCKED -> {
                    row.status.text = getString(R.string.mesh_voice_status_waiting_network)
                    row.progress.visibility = View.VISIBLE
                    row.progress.isIndeterminate = true
                }
            }
        }
        activeObservers[workId] = observer
        live.observe(this, observer)
    }

    private fun renderDownloading(
        row: RowBinding,
        fraction: Float,
        downloadedMb: Int,
        totalMb: Int,
    ) {
        row.progress.visibility = View.VISIBLE
        if (fraction in 0f..1f) {
            row.progress.isIndeterminate = false
            row.progress.progress = (fraction * 100).toInt().coerceIn(0, 100)
        } else {
            row.progress.isIndeterminate = true
        }
        row.status.text = getString(
            R.string.mesh_voice_status_downloading, downloadedMb, totalMb
        )
        row.action.text = getString(R.string.cancel)
    }

    private fun confirmDeleteOne(language: VoskModelManager.Language) {
        val name = MeshVoiceBinding.languageDisplayName(this, language)
        AlertDialog.Builder(this)
            .setTitle(R.string.mesh_voice_delete_one_title)
            .setMessage(getString(R.string.mesh_voice_delete_one_body, name))
            .setPositiveButton(R.string.delete) { _, _ ->
                VoskModelManager.delete(this, language)
                rebindRow(language)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDeleteAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.mesh_voice_delete_all_title)
            .setMessage(R.string.mesh_voice_delete_all_body)
            .setPositiveButton(R.string.delete) { _, _ ->
                VoskModelManager.deleteAll(this)
                rowBindings.keys.forEach { rebindRow(it) }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
