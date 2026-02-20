package com.hesham0_0.marassel.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hesham0_0.marassel.domain.model.MessageStatus
import com.hesham0_0.marassel.domain.repository.MessageRepository
import com.hesham0_0.marassel.worker.WorkDataUtils.extractMediaUrls
import com.hesham0_0.marassel.worker.WorkDataUtils.toMessageEntity
import com.hesham0_0.marassel.worker.WorkDataUtils.toSendMessageParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject


@HiltWorker
class SendMessageWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val messageRepository: MessageRepository,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {

        // Step 1 — Deserialize input
        val sendParams = inputData.toSendMessageParams()
            ?: return Result.failure(
                workDataOf("error" to "Missing or invalid input data for SendMessageWorker")
            )

        val localId = sendParams.localId

        // Step 2 — Merge any media URLs from UploadMediaWorker output.
        //
        // When this worker is chained after UploadMediaWorker, WorkManager
        // merges the upload worker's output Data into this worker's inputData.
        // The KEY_MEDIA_URLS from the upload output overwrites whatever was
        // originally set at enqueue time — which is correct behavior.
        val mergedMediaUrls = inputData.extractMediaUrls()
        val effectiveParams = if (mergedMediaUrls.isNotEmpty()) {
            sendParams.copy(mediaUrls = mergedMediaUrls)
        } else {
            sendParams
        }

        // Step 3 — Build the entity with final media URLs
        val messageEntity = effectiveParams.toMessageEntity().let { entity ->
            entity.copy(mediaUrl = effectiveParams.mediaUrls.firstOrNull())
        }

        // Step 4 — Mark as SENDING in local store (immediate UI feedback)
        messageRepository.updateMessageStatus(
            localId = localId,
            status = MessageStatus.PENDING,  // UI shows spinner until SENT
        )

        // Step 5 — Send to Firebase
        val sendResult = messageRepository.sendMessage(messageEntity)

        return if (sendResult.isSuccess) {
            // Step 6a — Success: update to SENT with the Firebase key
            val firebaseKey = sendResult.getOrNull()
            messageRepository.updateMessageStatus(
                localId = localId,
                status = MessageStatus.SENT,
                firebaseKey = firebaseKey,
            )
            Result.success()
        } else {
            // Step 6b — Failure: retry or give up
            handleSendFailure(
                localId = localId,
                cause = sendResult.exceptionOrNull()
                    ?: IllegalStateException("sendMessage returned failure with no exception"),
                previewText = messageEntity.text ?: messageEntity.previewText,
            )
        }
    }


    override suspend fun getForegroundInfo(): ForegroundInfo {
        val localId = inputData.getString(WorkerKeys.KEY_LOCAL_ID) ?: "unknown"
        return ForegroundInfo(
            WorkerKeys.NOTIFICATION_ID_UPLOAD,
            NotificationHelper.buildUploadIndeterminateNotification(appContext, localId),
        )
    }


    private suspend fun handleSendFailure(
        localId: String,
        cause: Throwable,
        previewText: String,
    ): Result {
        return if (runAttemptCount < MAX_ATTEMPTS - 1) {
            // Still have retries left — WorkManager will reschedule with
            // exponential backoff (configured at enqueue site, CHAT-035)
            Result.retry()
        } else {
            // All retries exhausted — give up and notify the user
            messageRepository.updateMessageStatus(
                localId = localId,
                status = MessageStatus.FAILED,
            )

            val notificationId = WorkerKeys.NOTIFICATION_ID_FAILED + localId.hashCode()
            androidx.core.app.NotificationManagerCompat.from(appContext).notify(
                notificationId,
                NotificationHelper.buildFailedMessageNotification(
                    context = appContext,
                    localId = localId,
                    previewText = previewText,
                ),
            )

            Result.failure(
                workDataOf("error" to (cause.message ?: "Send failed after $MAX_ATTEMPTS attempts"))
            )
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
    }
}