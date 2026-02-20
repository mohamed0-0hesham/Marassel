package com.hesham0_0.marassel.domain.usecase.message

import com.hesham0_0.marassel.domain.model.MessageEntity
import com.hesham0_0.marassel.domain.repository.AuthRepository
import com.hesham0_0.marassel.domain.repository.MessageRepository
import com.hesham0_0.marassel.domain.repository.UserRepository
import javax.inject.Inject


class SendMessageUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
) {

    suspend operator fun invoke(text: String): SendMessageResult {
        val validation = MessageValidator.validateText(text)
        if (!validation.isValid) {
            return SendMessageResult.ValidationFailed(validation)
        }

        return send { uid, displayName ->
            MessageEntity.createTextMessage(
                senderUid  = uid,
                senderName = displayName,
                text       = text,
            )
        }
    }

    suspend fun sendMedia(
        mimeType: String,
        fileSizeBytes: Long,
    ): SendMessageResult {
        val validation = MessageValidator.validateMedia(mimeType, fileSizeBytes)
        if (!validation.isValid) {
            return SendMessageResult.ValidationFailed(validation)
        }

        return send { uid, displayName ->
            MessageEntity.createMediaMessage(
                senderUid  = uid,
                senderName = displayName,
                mediaType  = mimeType,
            )
        }
    }

    private suspend fun send(
        build: suspend (uid: String, displayName: String) -> MessageEntity,
    ): SendMessageResult {

        // Step 1 — Resolve auth user
        val authUser = authRepository.getCurrentUser()
            ?: return SendMessageResult.NotAuthenticated

        // Step 2 — Resolve chat profile (need display name)
        val profile = userRepository.getProfile(authUser.uid)
            .getOrNull()
            ?: return SendMessageResult.NotOnboarded

        // Step 3 — Build the message entity
        val message = build(authUser.uid, profile.username)

        // Step 4 — Persist locally for immediate UI feedback
        return messageRepository.saveMessageLocally(message).fold(
            onSuccess = { SendMessageResult.Success(message) },
            onFailure = { SendMessageResult.StorageError(it) },
        )
    }
}

sealed class SendMessageResult {

    data class Success(val message: MessageEntity) : SendMessageResult()

    data class ValidationFailed(
        val reason: MessageValidator.ValidationResult,
    ) : SendMessageResult()

    data object NotAuthenticated : SendMessageResult()

    data object NotOnboarded : SendMessageResult()

    data class StorageError(val cause: Throwable) : SendMessageResult()

    val isSuccess: Boolean get() = this is Success
}