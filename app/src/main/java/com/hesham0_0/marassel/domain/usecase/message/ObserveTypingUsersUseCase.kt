package com.hesham0_0.marassel.domain.usecase.message

import com.hesham0_0.marassel.domain.repository.AuthRepository
import com.hesham0_0.marassel.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveTypingUsersUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val messageRepository: MessageRepository,
) {
    operator fun invoke(): Flow<Map<String, String>> {
        return messageRepository.observeTypingUsers().map { typingMap ->
            // Filter out the current user from the typing map
            val currentUserUid = authRepository.getCurrentUser()?.uid
            if (currentUserUid != null) {
                typingMap.filterKeys { it != currentUserUid }
            } else {
                typingMap
            }
        }
    }
}
