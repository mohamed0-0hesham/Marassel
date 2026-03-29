package com.hesham0_0.marassel.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.hesham0_0.marassel.ui.chat.MimeTypeResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class CachedMediaResult(
    val uri: Uri,
    val size: Long,
    val mimeType: String
)

@Singleton
class MediaFileCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mimeTypeResolver: MimeTypeResolver
) {
    suspend fun cacheMediaFile(uri: Uri): CachedMediaResult? = withContext(Dispatchers.IO) {
        try {
            val resolvedMimeType = mimeTypeResolver.resolve(uri, "image/jpeg")
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(resolvedMimeType) ?: "tmp"
            val cachedFile = File(context.cacheDir, "upload_${UUID.randomUUID()}.$extension")

            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Could not open input stream for $uri")
            
            input.use { inStream ->
                cachedFile.outputStream().use { outStream ->
                    inStream.copyTo(outStream)
                }
            }

            val finalSize = if (cachedFile.exists() && cachedFile.length() > 0) cachedFile.length() else 1L
            CachedMediaResult(Uri.fromFile(cachedFile), finalSize, resolvedMimeType)
        } catch (e: Exception) {
            Log.e("MediaFileCache", "Failed to copy media file to cache", e)
            null
        }
    }
}
