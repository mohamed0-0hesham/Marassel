package com.hesham0_0.marassel.domain.repository

import com.hesham0_0.marassel.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeAuthState(): Flow<AuthUser?>
    suspend fun getCurrentUser(): AuthUser?
    suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun signInWithGoogle(googleIdToken: String): Result<AuthUser>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
}