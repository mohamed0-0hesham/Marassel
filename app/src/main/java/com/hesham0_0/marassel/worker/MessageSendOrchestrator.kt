package com.hesham0_0.marassel.worker

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.hesham0_0.marassel.domain.model.MessageEntity
import com.hesham0_0.marassel.worker.WorkDataUtils.buildSendMessageInputData
import com.hesham0_0.marassel.worker.WorkDataUtils.buildUploadMediaInputData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageSendOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    private val sendConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun enqueueTextMessage(message: MessageEntity): OneTimeWorkRequest {
        val sendRequest = buildSendRequest(message)

        workManager.enqueueUniqueWork(
            message.localId,
            ExistingWorkPolicy.REPLACE,
            sendRequest,
        )

        return sendRequest
    }

    fun enqueueMediaMessage(
        message: MessageEntity,
        mediaUri: Uri,
        mimeType: String,
    ): Pair<OneTimeWorkRequest, OneTimeWorkRequest> {
        val uploadRequest = buildUploadRequest(
            localId  = message.localId,
            mediaUri = mediaUri,
            mimeType = mimeType,
        )
        val sendRequest = buildSendRequest(message)

        workManager
            .beginUniqueWork(
                message.localId,
                ExistingWorkPolicy.REPLACE,
                uploadRequest,
            )
            .then(sendRequest)
            .enqueue()

        return uploadRequest to sendRequest
    }

    fun retryMessage(
        message: MessageEntity,
        mediaUri: Uri? = null,
        mimeType: String? = null,
    ): List<OneTimeWorkRequest> {
        val uploadAlreadySucceeded = !message.mediaUrl.isNullOrBlank() && message.mediaUrl!!.startsWith("http")

        return when {
            // Text message or upload already done — only need to resend
            !message.isMedia || uploadAlreadySucceeded -> {
                val sendRequest = buildSendRequest(message)
                workManager.enqueueUniqueWork(
                    message.localId,
                    ExistingWorkPolicy.REPLACE,
                    sendRequest,
                )
                listOf(sendRequest)
            }

            // Media message with failed upload — re-run full chain
            mediaUri != null && mimeType != null -> {
                val (uploadRequest, sendRequest) = enqueueMediaMessage(message, mediaUri, mimeType)
                listOf(uploadRequest, sendRequest)
            }

            // Media message but no URI available — can only re-enqueue send
            // (will fail again if URL is missing, but avoids a crash)
            else -> {
                val sendRequest = buildSendRequest(message)
                workManager.enqueueUniqueWork(
                    message.localId,
                    ExistingWorkPolicy.REPLACE,
                    sendRequest,
                )
                listOf(sendRequest)
            }
        }
    }

    private fun buildSendRequest(message: MessageEntity): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<SendMessageWorker>()
            .setInputData(buildSendMessageInputData(message))
            .setConstraints(sendConstraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS,
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(WorkerKeys.TAG_SEND_MESSAGE)
            .addTag(message.localId)
            .build()

    private fun buildUploadRequest(
        localId: String,
        mediaUri: Uri,
        mimeType: String,
    ): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<UploadMediaWorker>()
            .setInputData(buildUploadMediaInputData(localId, mediaUri, mimeType))
            .setConstraints(sendConstraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS,
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(WorkerKeys.TAG_UPLOAD_MEDIA)
            .addTag(localId)
            .build()

    companion object {
        private const val BACKOFF_DELAY_SECONDS = 10L
    }
}