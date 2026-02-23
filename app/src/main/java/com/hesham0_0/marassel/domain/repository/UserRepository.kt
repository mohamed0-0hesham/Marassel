package com.hesham0_0.marassel.domain.repository

import com.hesham0_0.marassel.domain.model.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun hasProfile(uid: String): Boolean
    suspend fun saveProfile(user: UserEntity): Result<Unit>
    suspend fun getProfile(uid: String): Result<UserEntity?>
    fun observeProfile(uid: String): Flow<UserEntity?>
    suspend fun clearProfile(uid: String): Result<Unit>
}