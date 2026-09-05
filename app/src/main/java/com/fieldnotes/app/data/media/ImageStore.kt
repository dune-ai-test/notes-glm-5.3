package com.fieldnotes.app.data.media

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Copies picked gallery images into the app's private storage so they survive
 * (picked content URIs are one-shot grants).
 */
class ImageStore(private val context: Context) {

    fun imagesDir(): File = File(context.filesDir, "images")

    suspend fun copyFrom(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val dir = imagesDir().apply { mkdirs() }
            val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
            input.use { stream ->
                file.outputStream().use { output -> stream.copyTo(output) }
            }
            if (file.length() > 0) file.absolutePath else null
        } catch (_: Exception) {
            null
        }
    }
}
