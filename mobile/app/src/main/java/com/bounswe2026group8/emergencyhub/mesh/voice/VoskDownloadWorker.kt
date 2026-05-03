package com.bounswe2026group8.emergencyhub.mesh.voice

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background download of a single Vosk language model. Lives in WorkManager
 * because:
 *   * the download can outlive the activity that started it (~40 MB on a
 *     slow connection isn't unusual to take 30–60s),
 *   * we want it pause/resume and survive process death,
 *   * we want declarative network constraints (don't burn cellular unless
 *     the user opted in via [MeshVoicePrefs.allowMetered]).
 *
 * Progress is reported via WorkManager's `setProgress` so the settings UI can
 * observe `getWorkInfoByIdLiveData` and update without holding any references
 * to this Worker.
 */
class VoskDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val code = inputData.getString(KEY_LANG) ?: return@withContext Result.failure(
            errorData("missing_lang")
        )
        val language = VoskModelManager.Language.fromCode(code)
            ?: return@withContext Result.failure(errorData("unknown_lang"))

        try {
            VoskModelManager.downloadAndInstall(
                context = applicationContext,
                language = language,
                onProgress = { fraction, downloaded, total ->
                    // setProgress is a suspend on KTX; on the IO dispatcher we
                    // can't suspend from the lambda, but `setProgressAsync`
                    // returns a future that's safe to fire-and-forget — this
                    // method is called dozens of times so we don't await it.
                    setProgressAsync(
                        workDataOf(
                            KEY_PROGRESS_FRACTION to fraction,
                            KEY_PROGRESS_DOWNLOADED to downloaded,
                            KEY_PROGRESS_TOTAL to total,
                            KEY_LANG to code,
                        )
                    )
                },
                isCancelled = { isStopped },
            )
            Result.success(workDataOf(KEY_LANG to code))
        } catch (t: Throwable) {
            // Don't retry forever on a 404 / corrupted zip / disk-full —
            // Result.failure is terminal, the user can re-issue from the UI.
            Result.failure(errorData(t.message ?: t.javaClass.simpleName))
        }
    }

    private fun errorData(msg: String): Data = workDataOf(KEY_ERROR to msg)

    companion object {
        const val KEY_LANG = "lang"
        const val KEY_PROGRESS_FRACTION = "progress_fraction"
        const val KEY_PROGRESS_DOWNLOADED = "progress_downloaded"
        const val KEY_PROGRESS_TOTAL = "progress_total"
        const val KEY_ERROR = "error"

        /** Per-language unique work name so KEEP gives idempotency. */
        fun workName(language: VoskModelManager.Language): String =
            "vosk-download-${language.code}"

        /**
         * Enqueue (or no-op if already running) a download for [language].
         * Returns the work-request UUID so observers can subscribe.
         */
        fun enqueue(
            context: Context,
            language: VoskModelManager.Language,
            allowMetered: Boolean,
        ): java.util.UUID {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (allowMetered) NetworkType.CONNECTED else NetworkType.UNMETERED
                )
                .setRequiresStorageNotLow(true)
                .build()

            val request = OneTimeWorkRequestBuilder<VoskDownloadWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_LANG to language.code))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                workName(language),
                ExistingWorkPolicy.KEEP,
                request,
            )
            return request.id
        }

        fun cancel(context: Context, language: VoskModelManager.Language) {
            WorkManager.getInstance(context).cancelUniqueWork(workName(language))
        }

        /** Find the currently-active or most recent WorkInfo for a language. */
        fun currentWorkInfos(
            context: Context,
            language: VoskModelManager.Language,
        ): List<WorkInfo> = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(workName(language))
            .get()
    }
}
