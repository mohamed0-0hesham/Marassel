package com.hesham0_0.marassel.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.hesham0_0.marassel.domain.model.AuthProvider
import com.hesham0_0.marassel.domain.model.AuthUser
import com.hesham0_0.marassel.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
) : AuthRepository {

    override fun observeAuthState(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toDomain())
        }
        auth.addAuthStateListener(listener)

        trySend(auth.currentUser?.toDomain())
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun getCurrentUser(): AuthUser? =
        auth.currentUser?.toDomain()

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
    ): Result<AuthUser> = runCatching {
        val result = auth
            .createUserWithEmailAndPassword(email, password)
            .await()
        result.user?.toDomain()
            ?: error("Sign-up succeeded but user is null")
    }

    override suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<AuthUser> = runCatching {
        val result = auth
            .signInWithEmailAndPassword(email, password)
            .await()
        result.user?.toDomain()
            ?: error("Sign-in succeeded but user is null")
    }

    override suspend fun signInWithGoogle(
        googleIdToken: String,
    ): Result<AuthUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val result = auth
            .signInWithCredential(credential)
            .await()
        result.user?.toDomain()
            ?: error("Google sign-in succeeded but user is null")
    }

    override suspend fun sendPasswordResetEmail(
        email: String,
    ): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        auth.signOut()
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private fun FirebaseUser.toDomain(): AuthUser {
        val provider = providerData
            .map { it.providerId }
            .firstOrNull { it == "google.com" }
            ?.let { AuthProvider.GOOGLE }
            ?: AuthProvider.EMAIL_PASSWORD

        return AuthUser(
            uid              = uid,
            email            = email,
            displayName      = displayName,
            photoUrl         = photoUrl?.toString(),
            isEmailVerified  = isEmailVerified,
            provider         = provider,
        )
    }
}