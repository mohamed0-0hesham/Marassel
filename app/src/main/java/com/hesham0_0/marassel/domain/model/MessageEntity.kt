package com.hesham0_0.marassel.domain.model

import java.util.UUID

data class MessageEntity(
    val localId: String,
    val firebaseKey: String?,
    val senderUid: String,
    val senderName: String,
    val text: String?,
    val mediaUrl: String?,
    val mediaType: String?,
    val timestamp: Long,
    val status: MessageStatus,
    val type: MessageType,
    val replyToId: String? = null,
) {

    val previewText: String
        get() = when (type) {
            MessageType.TEXT -> text ?: ""
            MessageType.IMAGE -> "📷 Image"
            MessageType.VIDEO -> "🎥 Video"
        }

    val isMedia: Boolean
        get() = type == MessageType.IMAGE || type == MessageType.VIDEO

    companion object {

        fun createTextMessage(
            senderUid: String,
            senderName: String,
            text: String,
            timestamp: Long = System.currentTimeMillis(),
        ): MessageEntity = MessageEntity(
            localId = UUID.randomUUID().toString(),
            firebaseKey = null,
            senderUid = senderUid,
            senderName = senderName,
            text = text.trim(),
            mediaUrl = null,
            mediaType = null,
            timestamp = timestamp,
            status = MessageStatus.PENDING,
            type = MessageType.TEXT,
        )

        fun createMediaMessage(
            senderUid: String,
            senderName: String,
            mediaType: String,
            localMediaUri: String? = null,
            timestamp: Long = System.currentTimeMillis(),
        ): MessageEntity {
            val type = when {
                mediaType.startsWith("image") -> MessageType.IMAGE
                mediaType.startsWith("video") -> MessageType.VIDEO
                else -> MessageType.IMAGE // fallback
            }
            return MessageEntity(
                localId = UUID.randomUUID().toString(),
                firebaseKey = null,
                senderUid = senderUid,
                senderName = senderName,
                text = null,
                mediaUrl = localMediaUri,
                mediaType = mediaType,
                timestamp = timestamp,
                status = MessageStatus.PENDING,
                type = type,
            )
        }
    }
}

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO;

    companion object {

        fun fromString(value: String?): MessageType =
            entries.find { it.name == value } ?: TEXT
    }
}