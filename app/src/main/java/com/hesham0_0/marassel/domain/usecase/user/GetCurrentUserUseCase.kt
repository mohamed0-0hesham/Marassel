package com.hesham0_0.marassel.domain.usecase.user

import com.hesham0_0.marassel.domain.model.UserEntity
import com.hesham0_0.marassel.domain.repository.AuthRepository
import com.hesham0_0.marassel.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<UserEntity?> = authRepository
        .observeAuthState()
        .flatMapLatest { authUser ->
            when (authUser) {
                // Not signed in → emit null immediately, no profile to look up
                null -> flowOf(null)

                // Signed in → observe their profile reactively
                // If profile doesn't exist yet (new user), emits null
                else -> userRepository.observeProfile(authUser.uid)
            }
        }
        .distinctUntilChanged()


    suspend fun getOnce(): UserEntity? {
        val authUser = authRepository.getCurrentUser() ?: return null
        return userRepository.getProfile(authUser.uid).getOrNull()
    }


    suspend fun isFullyOnboarded(): Boolean = getOnce() != null
}