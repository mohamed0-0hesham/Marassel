package com.hesham0_0.marassel.domain.usecase.message

import com.hesham0_0.marassel.domain.model.MessageEntity
import com.hesham0_0.marassel.domain.repository.AuthRepository
import com.hesham0_0.marassel.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
) {

    operator fun invoke(): Flow<List<MessageUiItem>> =
        messageRepository
            .observeMessages()
            .map { messages -> transform(messages) }
            .distinctUntilChanged()

    private suspend fun transform(messages: List<MessageEntity>): List<MessageUiItem> {
        if (messages.isEmpty()) return emptyList()

        val currentUid = authRepository.getCurrentUser()?.uid

        return messages.mapIndexed { index, message ->
            val prev = messages.getOrNull(index - 1)
            val next = messages.getOrNull(index + 1)

            val isOwnMessage   = message.senderUid == currentUid
            val showSenderInfo = prev == null || prev.senderUid != message.senderUid
            val isLastInBurst  = next == null || next.senderUid != message.senderUid
            val showTimestamp  = prev == null || !isSameDay(prev.timestamp, message.timestamp)

            MessageUiItem(
                message  = message,
                meta     = MessageUiMeta(
                    isOwnMessage   = isOwnMessage,
                    showSenderInfo = showSenderInfo,
                    isLastInBurst  = isLastInBurst,
                    showTimestamp  = showTimestamp,
                ),
            )
        }
    }

    private fun isSameDay(timestampA: Long, timestampB: Long): Boolean {
        val calA = java.util.Calendar.getInstance().apply { timeInMillis = timestampA }
        val calB = java.util.Calendar.getInstance().apply { timeInMillis = timestampB }
        return calA.get(java.util.Calendar.YEAR)         == calB.get(java.util.Calendar.YEAR)
                && calA.get(java.util.Calendar.DAY_OF_YEAR)  == calB.get(java.util.Calendar.DAY_OF_YEAR)
    }
}

data class MessageUiItem(
    val message: MessageEntity,
    val meta: MessageUiMeta,
)

data class MessageUiMeta(
    val isOwnMessage: Boolean,
    val showSenderInfo: Boolean,
    val isLastInBurst: Boolean,
    val showTimestamp: Boolean,
)