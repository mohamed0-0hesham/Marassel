package com.hesham0_0.marassel.ui.chat

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the MIME type of content URI using the ContentResolver.
 *
 * Kept separate from the ViewModel so it can be mocked in tests and
 * keeps Android framework dependencies out of the ViewModel constructor.
 */
@Singleton
class MimeTypeResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Returns the MIME type for a content:// URI, or a safe fallback.
     *
     * @param uri    Content URI from the media picker
     * @param fallback Default if resolution fails (defaults to image/jpeg)
     */
    fun resolve(uri: Uri, fallback: String = "image/jpeg"): String =
        context.contentResolver.getType(uri) ?: fallback

    /**
     * Returns the file size in bytes for a content URI.
     * Returns 0 if the size cannot be determined.
     */
    fun resolveSize(uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                fd.statSize.coerceAtLeast(0L)
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}