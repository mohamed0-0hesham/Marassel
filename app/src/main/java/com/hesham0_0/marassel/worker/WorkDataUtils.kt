package com.hesham0_0.marassel.worker

import android.net.Uri
import androidx.work.Data
import androidx.work.workDataOf
import com.hesham0_0.marassel.domain.model.MessageEntity
import com.hesham0_0.marassel.domain.model.MessageStatus
import com.hesham0_0.marassel.domain.model.MessageType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WorkDataUtils {
    fun buildSendMessageInputData(
        message: MessageEntity,
        mediaUrls: List<String> = emptyList(),
    ): Data = workDataOf(
        WorkerKeys.KEY_LOCAL_ID to message.localId,
        WorkerKeys.KEY_SENDER_UID to message.senderUid,
        WorkerKeys.KEY_SENDER_NAME to message.senderName,
        WorkerKeys.KEY_CONTENT to (message.text ?: ""),
        WorkerKeys.KEY_MEDIA_URLS to Json.encodeToString(
            // Merge entity URLs with any additionally supplied URLs
            (listOfNotNull(message.mediaUrl) + mediaUrls).distinct()
        ),
        WorkerKeys.KEY_MEDIA_TYPE to (message.mediaType ?: ""),
        WorkerKeys.KEY_TIMESTAMP to message.timestamp,
        WorkerKeys.KEY_MESSAGE_TYPE to message.type.name,
    )

    fun buildUploadMediaInputData(
        localId: String,
        mediaUri: Uri,
        mimeType: String,
    ): Data = workDataOf(
        WorkerKeys.KEY_LOCAL_ID to localId,
        WorkerKeys.KEY_MEDIA_URI to mediaUri.toString(),
        WorkerKeys.KEY_MEDIA_TYPE to mimeType,
    )

    fun Data.toSendMessageParams(): SendMessageParams? {
        val localId = getString(WorkerKeys.KEY_LOCAL_ID)?.takeIf { it.isNotBlank() } ?: return null
        val senderUid =
            getString(WorkerKeys.KEY_SENDER_UID)?.takeIf { it.isNotBlank() } ?: return null
        val senderName =
            getString(WorkerKeys.KEY_SENDER_NAME)?.takeIf { it.isNotBlank() } ?: return null
        val content = getString(WorkerKeys.KEY_CONTENT) ?: ""
        val mediaType = getString(WorkerKeys.KEY_MEDIA_TYPE)?.takeIf { it.isNotBlank() }
        val timestamp = getLong(WorkerKeys.KEY_TIMESTAMP, 0L)
        val messageType = MessageType.fromString(getString(WorkerKeys.KEY_MESSAGE_TYPE))
        val mediaUrls = extractMediaUrls()

        return SendMessageParams(
            localId = localId,
            senderUid = senderUid,
            senderName = senderName,
            content = content,
            mediaUrls = mediaUrls,
            mediaType = mediaType,
            timestamp = timestamp,
            messageType = messageType,
        )
    }

    fun SendMessageParams.toMessageEntity(): MessageEntity = MessageEntity(
        localId = localId,
        firebaseKey = null,
        senderUid = senderUid,
        senderName = senderName,
        text = content.takeIf { it.isNotBlank() },
        mediaUrl = mediaUrls.firstOrNull(),
        mediaType = mediaType,
        timestamp = timestamp,
        status = MessageStatus.PENDING,
        type = messageType,
    )

    fun Data.toUploadMediaParams(): UploadMediaParams? {
        val localId = getString(WorkerKeys.KEY_LOCAL_ID)?.takeIf { it.isNotBlank() } ?: return null
        val uriString =
            getString(WorkerKeys.KEY_MEDIA_URI)?.takeIf { it.isNotBlank() } ?: return null
        val mimeType = getString(WorkerKeys.KEY_MEDIA_TYPE) ?: ""
        return UploadMediaParams(
            localId = localId,
            mediaUri = Uri.parse(uriString),
            mimeType = mimeType,
        )
    }

    fun Data.extractMediaUrls(): List<String> {
        val json = getString(WorkerKeys.KEY_MEDIA_URLS) ?: return emptyList()
        if (json.isBlank()) return emptyList()
        return runCatching {
            Json.decodeFromString<List<String>>(json)
        }.getOrDefault(emptyList())
    }

    fun buildUploadSuccessOutput(
        localId: String,
        downloadUrl: String,
    ): Data = workDataOf(
        WorkerKeys.KEY_LOCAL_ID to localId,
        WorkerKeys.KEY_MEDIA_URLS to Json.encodeToString(listOf(downloadUrl)),
    )
}

data class SendMessageParams(
    val localId: String,
    val senderUid: String,
    val senderName: String,
    val content: String,
    val mediaUrls: List<String>,
    val mediaType: String?,
    val timestamp: Long,
    val messageType: MessageType,
)

data class UploadMediaParams(
    val localId: String,
    val mediaUri: Uri,
    val mimeType: String,
)