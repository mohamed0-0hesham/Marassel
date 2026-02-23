package com.hesham0_0.marassel.domain.usecase.message

import com.hesham0_0.marassel.domain.model.MessageType
import com.hesham0_0.marassel.domain.repository.AuthRepository
import com.hesham0_0.marassel.domain.repository.MessageRepository
import javax.inject.Inject

class DeleteMessageUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val messageRepository: MessageRepository,
) {

    suspend operator fun invoke(
        localId: String,
        firebaseKey: String?,
        senderUid: String,
        type: MessageType,
    ): DeleteResult {

        val currentUser = authRepository.getCurrentUser()
            ?: return DeleteResult.NotAuthenticated

        if (senderUid != currentUser.uid) {
            return DeleteResult.NotOwner(
                messageLocalId = localId,
                ownerUid = senderUid,
                currentUserUid = currentUser.uid,
            )
        }

        if (firebaseKey == null) {
            return DeleteResult.UnconfirmedMessage(localId = localId)
        }

        messageRepository.deleteMessage(
            firebaseKey = firebaseKey,
            localId = localId,
            type = type
        ).fold(
            onSuccess = { return DeleteResult.Success(localId = localId) },
            onFailure = { return DeleteResult.StorageError(it) },
        )

        @Suppress("UNREACHABLE_CODE")
        return DeleteResult.StorageError(IllegalStateException("Unreachable"))
    }
}

sealed class DeleteResult {

    data class Success(val localId: String) : DeleteResult()

    data object NotAuthenticated : DeleteResult()

    data class NotOwner(
        val messageLocalId: String,
        val ownerUid: String,
        val currentUserUid: String,
    ) : DeleteResult()

    data class UnconfirmedMessage(val localId: String) : DeleteResult()

    data class StorageError(val cause: Throwable) : DeleteResult()

    val isSuccess: Boolean get() = this is Success
}