package com.hesham0_0.marassel.domain.repository

import com.hesham0_0.marassel.domain.model.UserEntity
import kotlinx.coroutines.flow.Flow


/**
 * Repository for the user's chat profile — the display name and metadata
 * chosen during onboarding, stored locally in DataStore.
 *
 * This is intentionally separate from [AuthRepository]:
 * - Auth = who you ARE (Firebase UID, email, Google account)
 * - Profile = how you APPEAR in chat (chosen display name, initials)
 *
 * The profile is keyed by Firebase Auth UID so each authenticated user
 * has their own profile entry in DataStore.
 *
 * Profile lifecycle:
 * ──────────────────
 * 1. User signs in → [AuthRepository.observeAuthState] emits [AuthUser]
 * 2. App checks [hasProfile(uid)] → false on first sign-in
 * 3. App routes to UsernameScreen prefilled with [AuthUser.suggestedUsername]
 * 4. User confirms/edits name → [saveProfile] called
 * 5. App routes to ChatRoom — [observeProfile] emits the saved profile
 * 6. On sign-out → profile remains in DataStore for when they sign back in
 */
interface UserRepository {
    suspend fun hasProfile(uid: String): Boolean
    suspend fun saveProfile(user: UserEntity): Result<Unit>
    suspend fun getProfile(uid: String): Result<UserEntity?>
    fun observeProfile(uid: String): Flow<UserEntity?>
    suspend fun clearProfile(uid: String): Result<Unit>
}