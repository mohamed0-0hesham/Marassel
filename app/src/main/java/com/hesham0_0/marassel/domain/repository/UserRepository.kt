package com.hesham0_0.marassel.domain.repository

import com.hesham0_0.marassel.domain.model.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeUser(): Flow<UserEntity?>
}