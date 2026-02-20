package com.hesham0_0.marassel.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import com.hesham0_0.marassel.data.remote.FirebaseStorageDataSource.Companion.MAX_FILENAME_LENGTH
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageDataSource @Inject constructor(
    private val firebaseStorage: FirebaseStorage,
) {

    companion object {
        private const val MEDIA_ROOT     = "chat_media"
        private const val DEFAULT_MIME   = "application/octet-stream"
        private const val MAX_FILENAME_LENGTH = 64
    }

    fun uploadMedia(
        fileUri: Uri,
        localId: String,
        mimeType: String,
    ): Flow<UploadState> = callbackFlow {

        // Build storage path: chat_media/{localId}/{filename}_{timestamp}.{ext}
        val storagePath = buildStoragePath(
            localId  = localId,
            fileUri  = fileUri,
            mimeType = mimeType,
        )

        val storageRef = firebaseStorage.getReference(storagePath)
        val metadata   = storageMetadata { contentType = mimeType.ifBlank { DEFAULT_MIME } }
        val uploadTask = storageRef.putFile(fileUri, metadata)

        uploadTask.addOnProgressListener { snapshot ->
            val total       = snapshot.totalByteCount
            val transferred = snapshot.bytesTransferred
            val percent     = if (total > 0L) {
                ((transferred.toDouble() / total.toDouble()) * 100.0)
                    .toInt()
                    .coerceIn(0, 100)
            } else 0

            trySend(UploadState.Progress(percent))
        }

        uploadTask.addOnSuccessListener {
            // Resolve the public download URL then close the flow
            storageRef.downloadUrl
                .addOnSuccessListener { uri ->
                    trySend(UploadState.Success(downloadUrl = uri.toString()))
                    close()
                }
                .addOnFailureListener { exception ->
                    close(exception)
                }
        }

        uploadTask.addOnFailureListener { exception ->
            close(exception)
        }

        awaitClose { uploadTask.cancel() }
    }

    suspend fun deleteMediaForMessage(localId: String): Result<Unit> = runCatching {
        val prefix  = "$MEDIA_ROOT/$localId"
        val listRef = firebaseStorage.getReference(prefix)

        val listResult = listRef.listAll().await()

        // Delete all items under the prefix — individually (Firebase Storage
        // has no batch delete API)
        val errors = listResult.items.mapNotNull { ref ->
            runCatching { ref.delete().await() }.exceptionOrNull()
        }

        if (errors.isNotEmpty()) throw errors.first()
    }

    internal fun buildStoragePath(
        localId: String,
        fileUri: Uri,
        mimeType: String,
    ): String {
        val rawFilename  = fileUri.lastPathSegment ?: "upload"
        val extension    = extractExtension(rawFilename) ?: mimeTypeToExtension(mimeType)
        val baseName     = rawFilename
            .substringBeforeLast(".")
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(MAX_FILENAME_LENGTH)
        val timestamp    = System.currentTimeMillis()
        val finalName    = "${baseName}_${timestamp}${if (extension != null) ".$extension" else ""}"

        return "$MEDIA_ROOT/$localId/$finalName"
    }

    private fun extractExtension(filename: String): String? {
        val dotIndex = filename.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < filename.length - 1) {
            filename.substring(dotIndex + 1).lowercase()
        } else null
    }

    private fun mimeTypeToExtension(mimeType: String): String? = when (mimeType) {
        "image/jpeg"      -> "jpg"
        "image/png"       -> "png"
        "image/gif"       -> "gif"
        "image/webp"      -> "webp"
        "video/mp4"       -> "mp4"
        "video/3gpp"      -> "3gp"
        "video/quicktime" -> "mov"
        "video/webm"      -> "webm"
        else              -> null
    }
}

sealed class UploadState {

    data class Progress(val percent: Int) : UploadState() {
        init {
            require(percent in 0..100) {
                "percent must be in 0..100 but was $percent"
            }
        }
    }

    data class Success(val downloadUrl: String) : UploadState()

    data class Error(val cause: Throwable) : UploadState()

    val isSuccess: Boolean get() = this is Success

    val isError: Boolean get() = this is Error

    val isProgress: Boolean get() = this is Progress
}