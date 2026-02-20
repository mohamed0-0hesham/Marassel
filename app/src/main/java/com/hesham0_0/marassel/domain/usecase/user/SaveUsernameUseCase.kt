package com.hesham0_0.marassel.domain.usecase.user

import com.hesham0_0.marassel.domain.model.UserEntity
import com.hesham0_0.marassel.domain.repository.AuthRepository
import com.hesham0_0.marassel.domain.repository.UserRepository
import javax.inject.Inject

class SaveUsernameUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val validator: UsernameValidator = UsernameValidator,
) {
    suspend operator fun invoke(username: String): SaveUsernameResult {

        // Step 1 — Validate the username
        val validationResult = validator.validate(username)
        if (!validationResult.isValid) {
            return SaveUsernameResult.ValidationFailed(validationResult)
        }

        // Step 2 — Get current auth user (must be signed in)
        val authUser = authRepository.getCurrentUser()
            ?: return SaveUsernameResult.NotAuthenticated

        // Step 3 — Build the UserEntity
        val userEntity = UserEntity.create(
            uid      = authUser.uid,
            username = username.trim(),
            email    = authUser.email,
            photoUrl = authUser.photoUrl,
        )

        // Step 4 — Persist the profile
        return userRepository.saveProfile(userEntity).fold(
            onSuccess = { SaveUsernameResult.Success(userEntity) },
            onFailure = { SaveUsernameResult.StorageError(it) },
        )
    }
}

sealed class SaveUsernameResult {
    data class Success(val user: UserEntity) : SaveUsernameResult()

    data class ValidationFailed(
        val reason: UsernameValidator.ValidationResult,
    ) : SaveUsernameResult()

    data object NotAuthenticated : SaveUsernameResult()

    data class StorageError(val cause: Throwable) : SaveUsernameResult()

    val isSuccess: Boolean get() = this is Success
}