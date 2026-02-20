package com.hesham0_0.marassel.data.repository

import com.hesham0_0.marassel.domain.model.UserEntity
import com.hesham0_0.marassel.domain.repository.UserRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class UserRepositoryImpl @Inject constructor() : UserRepository {
    override fun observeUser(): Flow<UserEntity?> = flowOf(null)
}