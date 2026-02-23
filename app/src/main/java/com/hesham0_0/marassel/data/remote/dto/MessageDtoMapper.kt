package com.hesham0_0.marassel.data.remote.dto

import com.hesham0_0.marassel.domain.model.MessageEntity
import com.hesham0_0.marassel.domain.model.MessageStatus
import com.hesham0_0.marassel.domain.model.MessageType

object MessageDtoMapper {

    private const val UNKNOWN_UID  = "unknown_uid"
    private const val UNKNOWN_NAME = "Unknown"

    fun toEntity(dto: MessageDto, firebaseKey: String): MessageEntity {
        require(firebaseKey.isNotBlank()) {
            "firebaseKey must not be blank — every Firebase message must have a push key"
        }

        return MessageEntity(
            localId     = dto.localId?.takeIf { it.isNotBlank() } ?: firebaseKey,
            firebaseKey = firebaseKey,
            senderUid   = dto.senderUid?.takeIf { it.isNotBlank() } ?: UNKNOWN_UID,
            senderName  = dto.senderName?.takeIf { it.isNotBlank() } ?: UNKNOWN_NAME,
            text        = dto.text?.takeIf { it.isNotBlank() },
            mediaUrl    = dto.mediaUrl?.takeIf { it.isNotBlank() },
            mediaType   = dto.mediaType?.takeIf { it.isNotBlank() },
            timestamp   = dto.timestamp ?: 0L,
            status      = MessageStatus.SENT,
            type        = MessageType.fromString(dto.type),
            replyToId   = dto.replyToId?.takeIf { it.isNotBlank() },
        )
    }

    fun toEntityList(pairs: List<Pair<String, MessageDto>>): List<MessageEntity> =
        pairs.mapNotNull { (key, dto) ->
            runCatching { toEntity(dto, key) }.getOrNull()
        }

    fun toDto(entity: MessageEntity): MessageDto = MessageDto(
        senderUid  = entity.senderUid,
        senderName = entity.senderName,
        text       = entity.text,
        mediaUrl   = entity.mediaUrl,
        mediaType  = entity.mediaType,
        timestamp  = entity.timestamp,
        type       = entity.type.name,
        localId    = entity.localId,
        replyToId  = entity.replyToId,
    )

    fun isValid(entity: MessageEntity): Boolean {
        if (entity.senderUid == UNKNOWN_UID) return false
        if (entity.timestamp == 0L)          return false
        return when (entity.type) {
            MessageType.TEXT          -> !entity.text.isNullOrBlank()
            MessageType.IMAGE,
            MessageType.VIDEO         -> !entity.mediaUrl.isNullOrBlank()
        }
    }
}