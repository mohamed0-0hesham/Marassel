package com.hesham0_0.marassel.domain.usecase.message

import com.hesham0_0.marassel.domain.model.MessageEntity
import com.hesham0_0.marassel.domain.model.MessageStatus
import com.hesham0_0.marassel.domain.repository.MessageRepository

import javax.inject.Inject

class RetryMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
) {

    suspend operator fun invoke(localId: String): RetryMessageResult {

        // Step 1 — Load all local messages and find the target
        val localMessages = messageRepository.getLocalMessages()
            .getOrElse { return RetryMessageResult.StorageError(it) }

        val message = localMessages.find { it.localId == localId }
            ?: return RetryMessageResult.MessageNotFound(localId)

        // Step 2 — Guard: only FAILED messages can be retried
        if (message.status != MessageStatus.FAILED) {
            return RetryMessageResult.MessageNotFailed(
                localId       = localId,
                currentStatus = message.status,
            )
        }

        // Step 3 — Reset to PENDING so UI shows queued indicator
        messageRepository.updateMessageStatus(
            localId     = localId,
            status      = MessageStatus.PENDING,
            firebaseKey = message.firebaseKey, // preserve any existing key
        ).getOrElse { return RetryMessageResult.StorageError(it) }

        // Step 4 — Return the updated message for WorkManager dispatch
        val resetMessage = message.copy(status = MessageStatus.PENDING)
        return RetryMessageResult.Success(resetMessage)
    }
}

sealed class RetryMessageResult {

    data class Success(val message: MessageEntity) : RetryMessageResult()

    data class MessageNotFound(val localId: String) : RetryMessageResult()

    data class MessageNotFailed(
        val localId: String,
        val currentStatus: MessageStatus,
    ) : RetryMessageResult()

    data class StorageError(val cause: Throwable) : RetryMessageResult()

    val isSuccess: Boolean get() = this is Success
}