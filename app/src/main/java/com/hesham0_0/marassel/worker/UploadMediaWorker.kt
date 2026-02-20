package com.hesham0_0.marassel.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hesham0_0.marassel.data.remote.FirebaseStorageDataSource
import com.hesham0_0.marassel.data.remote.UploadState
import com.hesham0_0.marassel.domain.model.MessageStatus
import com.hesham0_0.marassel.domain.repository.MessageRepository
import com.hesham0_0.marassel.worker.WorkDataUtils.buildUploadSuccessOutput
import com.hesham0_0.marassel.worker.WorkDataUtils.toUploadMediaParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collectLatest

@HiltWorker
class UploadMediaWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val storageDataSource: FirebaseStorageDataSource,
    private val messageRepository: MessageRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {

        // Step 1 — Deserialize input
        val uploadParams = inputData.toUploadMediaParams()
            ?: return Result.failure(
                workDataOf("error" to "Missing or invalid input data for UploadMediaWorker")
            )

        val localId = uploadParams.localId
        val mediaUri = uploadParams.mediaUri
        val mimeType = uploadParams.mimeType

        // Step 2 — Show foreground notification immediately (indeterminate)
        setForeground(buildForegroundInfo(localId, progress = 0))

        // Step 3 — Update message status to SENDING
        messageRepository.updateMessageStatus(
            localId = localId,
            status = MessageStatus.PENDING,   // stays PENDING until send completes
        )

        // Step 4 — Upload and track progress
        var downloadUrl: String? = null

        try {
            storageDataSource.uploadMedia(
                fileUri = mediaUri,
                localId = localId,
                mimeType = mimeType,
            ).collectLatest { state ->
                when (state) {
                    is UploadState.Progress -> {
                        // Update foreground notification with real progress
                        setForeground(buildForegroundInfo(localId, state.percent))
                        // Report progress to WorkManager observers (UI can read this)
                        setProgress(
                            workDataOf(WorkerKeys.KEY_UPLOAD_PROGRESS to state.percent)
                        )
                    }

                    is UploadState.Success -> {
                        downloadUrl = state.downloadUrl
                    }

                    is UploadState.Error -> {
                        throw state.cause
                    }
                }
            }
        } catch (e: Exception) {
            return handleUploadFailure(localId, e)
        }

        // Step 5 — Validate we got a URL
        val url = downloadUrl
            ?: return handleUploadFailure(
                localId,
                IllegalStateException("Upload completed but no download URL was returned"),
            )

        // Step 6 — Return success with download URL for SendMessageWorker
        return Result.success(
            buildUploadSuccessOutput(
                localId = localId,
                downloadUrl = url,
            )
        )
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val localId = inputData.getString(WorkerKeys.KEY_LOCAL_ID) ?: "unknown"
        return buildForegroundInfo(localId, progress = 0)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildForegroundInfo(localId: String, progress: Int): ForegroundInfo =
        ForegroundInfo(
            WorkerKeys.NOTIFICATION_ID_UPLOAD,
            if (progress == 0) {
                NotificationHelper.buildUploadIndeterminateNotification(appContext, localId)
            } else {
                NotificationHelper.buildUploadProgressNotification(appContext, localId, progress)
            },
        )

    private suspend fun handleUploadFailure(localId: String, cause: Throwable): Result {
        return if (runAttemptCount < MAX_ATTEMPTS - 1) {
            Result.retry()
        } else {
            // Final attempt exhausted — mark message as failed
            messageRepository.updateMessageStatus(
                localId = localId,
                status = MessageStatus.FAILED,
            )
            NotificationHelper.cancelUploadNotification(appContext)
            Result.failure(workDataOf("error" to (cause.message ?: "Upload failed")))
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
    }
}