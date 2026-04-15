package com.bounswe2026group8.emergencyhub.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Shared helpers for preparing image uploads.
 *
 * Converts content URIs to [MultipartBody.Part] instances that Retrofit
 * can send to any multipart upload endpoint.
 */
object ImageUploadHelper {

    /**
     * Converts a list of content [Uri]s into Retrofit multipart parts.
     *
     * @param contentResolver the activity/context content resolver
     * @param uris            content URIs selected by the user
     * @return list of multipart parts ready for a Retrofit call
     */
    fun prepareParts(contentResolver: ContentResolver, uris: List<Uri>): List<MultipartBody.Part> {
        val parts = mutableListOf<MultipartBody.Part>()
        for (uri in uris) {
            val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: continue
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val fileName = getFileName(contentResolver, uri) ?: "image.jpg"
            val requestBody = bytes.toRequestBody(mimeType.toMediaType())
            parts.add(MultipartBody.Part.createFormData("images", fileName, requestBody))
        }
        return parts
    }

    /**
     * Resolves the display name of a content URI.
     */
    fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(idx)
            }
        }
        return name
    }
}
