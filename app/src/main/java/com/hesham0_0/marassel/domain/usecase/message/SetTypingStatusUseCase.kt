package com.hesham0_0.marassel.domain.usecase.message

import com.hesham0_0.marassel.domain.repository.AuthRepository
import com.hesham0_0.marassel.domain.repository.MessageRepository
import com.hesham0_0.marassel.domain.repository.UserRepository
import javax.inject.Inject

class SetTypingStatusUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
) {
    suspend operator fun invoke(isTyping: Boolean) {
        val user = authRepository.getCurrentUser() ?: return
        val profile = userRepository.getProfile(user.uid).getOrNull() ?: return
        
        messageRepository.setTypingStatus(
            uid = user.uid,
            displayName = profile.username,
            isTyping = isTyping
        )
    }
}
