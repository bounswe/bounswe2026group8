package com.bounswe2026group8.emergencyhub.mesh.voice

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Owns the on-disk catalog of Vosk small acoustic models, one per language.
 *
 * Models live under `context.filesDir/vosk-models/{lang}/` and a `.ready`
 * marker file is written only after the unzip completes successfully — so a
 * crash mid-download leaves the dir in an obviously-broken state and a
 * subsequent `isInstalled` returns false (we'll re-download instead of
 * loading a corrupt model into Vosk).
 *
 * URLs point at Vosk's official "small" mirrors on alphacephei.com. They've
 * been stable since 2019; if we ever need to pin a specific revision we'd
 * mirror them to a GitHub Release and just swap the URL constants.
 */
object VoskModelManager {

    private const val TAG = "VoskModelManager"
    private const val MODELS_DIR = "vosk-models"
    private const val READY_MARKER = ".ready"

    /**
     * Languages exposed in the settings UI. Keyed by the *app* language code
     * we use elsewhere (matches `LocaleManager.LANGUAGE_*`). The display name
     * in the UI comes from string resources — never hardcode it here.
     */
    enum class Language(
        val code: String,
        val downloadUrl: String,
        val approxMb: Int,
    ) {
        EN("en", "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip", 40),
        TR("tr", "https://alphacephei.com/vosk/models/vosk-model-small-tr-0.3.zip", 38),
        ES("es", "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip", 39),
        ZH("zh", "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip", 42);

        companion object {
            fun fromCode(code: String?): Language? =
                entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
        }
    }

    private fun modelsRoot(context: Context): File =
        File(context.filesDir, MODELS_DIR)

    fun modelDir(context: Context, language: Language): File =
        File(modelsRoot(context), language.code)

    fun isInstalled(context: Context, language: Language): Boolean {
        val dir = modelDir(context, language)
        return dir.isDirectory && File(dir, READY_MARKER).exists()
    }

    /**
     * Bytes consumed on disk by an installed model (best-effort recursive sum).
     * Returns 0 if the language isn't installed.
     */
    fun sizeBytes(context: Context, language: Language): Long {
        val dir = modelDir(context, language)
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun delete(context: Context, language: Language): Boolean {
        val dir = modelDir(context, language)
        if (!dir.exists()) return true
        return dir.deleteRecursively()
    }

    /**
     * Downloaded models live under one root we can wipe in a single call —
     * exposed so the settings UI can offer a "delete all" footer button.
     */
    fun deleteAll(context: Context): Boolean {
        val root = modelsRoot(context)
        if (!root.exists()) return true
        return root.deleteRecursively()
    }

    /**
     * Synchronous download + unzip used by [VoskDownloadWorker]. Reports
     * progress as a fraction in `0f..1f` (download phase only — unzip is
     * fast enough that we just snap to 1.0 on completion). Throws on any
     * failure; caller is responsible for rolling back the partial dir.
     */
    fun downloadAndInstall(
        context: Context,
        language: Language,
        onProgress: (fraction: Float, downloadedBytes: Long, totalBytes: Long) -> Unit,
        isCancelled: () -> Boolean = { false },
    ) {
        val targetDir = modelDir(context, language)
        // Always wipe any half-installed remnant before starting fresh — both
        // because the marker scheme means "missing marker == invalid" and to
        // make sure we don't accidentally union two model versions.
        if (targetDir.exists()) targetDir.deleteRecursively()
        targetDir.mkdirs()

        val tmpZip = File(context.cacheDir, "vosk-${language.code}.zip")
        if (tmpZip.exists()) tmpZip.delete()

        val client = OkHttpClient.Builder()
            // Connect aggressively but allow long reads — 40 MB on a slow
            // mobile connection can easily exceed the default 10s read timeout.
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(language.downloadUrl).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code} downloading ${language.downloadUrl}")
                }
                val body = response.body
                    ?: throw IllegalStateException("Empty response body for ${language.downloadUrl}")

                val total = body.contentLength().takeIf { it > 0 } ?: -1L
                FileOutputStream(tmpZip).use { out ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        var lastReportPct = -1
                        while (true) {
                            if (isCancelled()) {
                                throw InterruptedException("Download cancelled by user")
                            }
                            val read = input.read(buffer)
                            if (read <= 0) break
                            out.write(buffer, 0, read)
                            downloaded += read

                            // Throttle progress callbacks to whole-percent ticks
                            // — WorkManager's setProgress isn't free.
                            if (total > 0) {
                                val pct = ((downloaded * 100) / total).toInt()
                                if (pct != lastReportPct) {
                                    lastReportPct = pct
                                    onProgress(pct / 100f, downloaded, total)
                                }
                            } else {
                                onProgress(-1f, downloaded, -1L)
                            }
                        }
                    }
                }
            }

            if (isCancelled()) throw InterruptedException("Cancelled before unzip")
            unzip(tmpZip, targetDir)

            // Vosk .zip files contain a single top-level dir like
            // `vosk-model-small-tr-0.3/`. Flatten it so `Model(targetDir)`
            // works without a second directory hop.
            flattenSingleTopDir(targetDir)

            // Atomic completion marker — only written after the unzip succeeds.
            File(targetDir, READY_MARKER).writeText(language.downloadUrl)
            onProgress(1f, 1L, 1L)
        } catch (t: Throwable) {
            Log.w(TAG, "downloadAndInstall failed for ${language.code}", t)
            // Roll back: the partial dir would otherwise look "installed-ish"
            // to a casual `dir.exists()` check.
            targetDir.deleteRecursively()
            throw t
        } finally {
            tmpZip.delete()
        }
    }

    private fun unzip(zipFile: File, destination: File) {
        ZipInputStream(zipFile.inputStream()).use { zin ->
            while (true) {
                val entry = zin.nextEntry ?: break
                val outFile = File(destination, entry.name)
                // Defense against zip-slip: don't allow `..` to escape the
                // destination dir.
                if (!outFile.canonicalPath.startsWith(destination.canonicalPath)) {
                    throw IllegalStateException("Zip entry escapes destination: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { out ->
                        zin.copyTo(out)
                    }
                }
                zin.closeEntry()
            }
        }
    }

    /**
     * If the unzipped tree contains exactly one top-level directory (the
     * usual Vosk packaging), promote its children to the parent and remove
     * the now-empty wrapper. Idempotent: no-op if the tree is already flat.
     */
    private fun flattenSingleTopDir(dir: File) {
        val children = dir.listFiles()?.toList().orEmpty()
        val nonMarker = children.filter { it.name != READY_MARKER }
        if (nonMarker.size == 1 && nonMarker[0].isDirectory) {
            val wrapper = nonMarker[0]
            wrapper.listFiles()?.forEach { it.renameTo(File(dir, it.name)) }
            wrapper.delete()
        }
    }
}
