package com.hesham0_0.marassel.domain.repository

import com.hesham0_0.marassel.domain.model.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun saveUser(user: UserEntity): Result<Unit>
    suspend fun getUser(): Result<UserEntity?>
    fun observeUser(): Flow<UserEntity?>
    suspend fun clearUser(): Result<Unit>
}