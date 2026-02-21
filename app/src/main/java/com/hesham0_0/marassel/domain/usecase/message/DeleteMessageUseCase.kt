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

        // Step 1 — Resolve current user
        val currentUser = authRepository.getCurrentUser()
            ?: return DeleteResult.NotAuthenticated

        // Step 2 — Ownership check using caller-supplied senderUid
        // We trust the caller's senderUid (it comes from the MessageUiItem
        // that was rendered on screen). A mismatch should not be possible
        // in normal flow — the UI only shows a delete option for own messages.
        if (senderUid != currentUser.uid) {
            return DeleteResult.NotOwner(
                messageLocalId  = localId,
                ownerUid        = senderUid,
                currentUserUid  = currentUser.uid,
            )
        }

        // Step 3 — Handle unconfirmed messages (never reached Firebase)
        if (firebaseKey == null) {
            return DeleteResult.UnconfirmedMessage(localId = localId)
        }

        // Step 4 — Hard delete from Firebase + clear local queue
        messageRepository.deleteMessage(
            firebaseKey = firebaseKey,
            localId     = localId,
            type        = type
        ).fold(
            onSuccess = { return DeleteResult.Success(localId = localId) },
            onFailure = { return DeleteResult.StorageError(it)           },
        )

        // Unreachable — fold always returns above
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