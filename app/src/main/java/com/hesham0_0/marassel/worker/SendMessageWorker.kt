package com.hesham0_0.marassel.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat
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
        val notification = NotificationHelper.buildUploadIndeterminateNotification(appContext, localId)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                WorkerKeys.NOTIFICATION_ID_UPLOAD,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                WorkerKeys.NOTIFICATION_ID_UPLOAD,
                notification
            )
        }
    }


    private suspend fun handleSendFailure(
        localId: String,
        cause: Throwable,
        previewText: String,
    ): Result {
        return if (runAttemptCount < MAX_ATTEMPTS - 1) {
            Result.retry()
        } else {
            messageRepository.updateMessageStatus(
                localId = localId,
                status = MessageStatus.FAILED,
            )

            val notificationId = WorkerKeys.NOTIFICATION_ID_FAILED + localId.hashCode()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.NotificationManagerCompat.from(appContext).notify(
                    notificationId,
                    NotificationHelper.buildFailedMessageNotification(
                        context = appContext,
                        localId = localId,
                        previewText = previewText,
                    ),
                )
            }

            NotificationHelper.showFailedMessageSummary(appContext, 1)

            Result.failure(
                workDataOf("error" to (cause.message ?: "Send failed after $MAX_ATTEMPTS attempts"))
            )
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
    }
}