package com.hesham0_0.marassel.domain.usecase.message

import com.hesham0_0.marassel.domain.model.MessageEntity
import com.hesham0_0.marassel.domain.repository.MessageRepository

import javax.inject.Inject

class LoadOlderMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
) {

    companion object {
        const val DEFAULT_PAGE_SIZE = 20

        const val MIN_PAGE_SIZE = 1

        const val MAX_PAGE_SIZE = 100
    }

    suspend operator fun invoke(
        beforeTimestamp: Long,
        limit: Int,
    ): LoadOlderResult {

        // Step 1 — Validate inputs
        if (beforeTimestamp <= 0L) {
            return LoadOlderResult.Error(
                IllegalArgumentException(
                    "beforeTimestamp must be > 0, got $beforeTimestamp"
                )
            )
        }

        val clampedLimit = limit.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)

        // Step 2 — Fetch from repository
        val messages = messageRepository.loadOlderMessages(
            beforeTimestamp = beforeTimestamp,
            limit           = clampedLimit,
        ).getOrElse { return LoadOlderResult.Error(it) }

        // Step 3 — Sort ascending (oldest first) for prepend-friendly ordering
        val sorted = messages.sortedBy { it.timestamp }

        // Step 4 — Detect end of history
        // If fewer messages than requested were returned, there are no more.
        // An empty result also means end of history.
        val hasReachedEnd = sorted.size < clampedLimit

        return LoadOlderResult.Success(
            OlderMessagesResult(
                messages     = sorted,
                hasReachedEnd = hasReachedEnd,
                cursor        = sorted.firstOrNull()?.timestamp,
            )
        )
    }
}

sealed class LoadOlderResult {

    data class Success(val data: OlderMessagesResult) : LoadOlderResult()

    data class Error(val cause: Throwable) : LoadOlderResult()

    val isSuccess: Boolean get() = this is Success
}

data class OlderMessagesResult(
    val messages: List<MessageEntity>,
    val hasReachedEnd: Boolean,
    val cursor: Long?,
) {

    val isEmpty: Boolean get() = messages.isEmpty()

    val size: Int get() = messages.size
}