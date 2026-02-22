package com.hesham0_0.marassel.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hesham0_0.marassel.domain.model.UserEntity
import com.hesham0_0.marassel.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserRepository {

    private fun usernameKey(uid: String) =
        stringPreferencesKey("profile_${uid}_username")

    private fun emailKey(uid: String) =
        stringPreferencesKey("profile_${uid}_email")

    private fun photoUrlKey(uid: String) =
        stringPreferencesKey("profile_${uid}_photo_url")

    override suspend fun hasProfile(uid: String): Boolean =
        runCatching {
            dataStore.data
                .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
                .map { prefs -> prefs[usernameKey(uid)]?.isNotBlank() == true }
                .first()
        }.getOrDefault(false)

    override suspend fun saveProfile(user: UserEntity): Result<Unit> =
        runCatching {
            dataStore.edit { prefs ->
                prefs[usernameKey(user.uid)]  = user.username
                // Store nullable fields only if non-null
                user.email?.let    { prefs[emailKey(user.uid)]    = it }
                user.photoUrl?.let { prefs[photoUrlKey(user.uid)] = it }
            }
            Unit
        }

    override suspend fun getProfile(uid: String): Result<UserEntity?> =
        runCatching {
            dataStore.data
                .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
                .map { prefs -> prefs.toUserEntity(uid) }
                .first()
        }

    override fun observeProfile(uid: String): Flow<UserEntity?> =
        dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences()) else throw e
            }
            .map { prefs -> prefs.toUserEntity(uid) }

    override suspend fun clearProfile(uid: String): Result<Unit> =
        runCatching {
            dataStore.edit { prefs ->
                prefs.remove(usernameKey(uid))
                prefs.remove(emailKey(uid))
                prefs.remove(photoUrlKey(uid))
            }
            Unit
        }

    // ── Private helper ────────────────────────────────────────────────────────

    private fun Preferences.toUserEntity(uid: String): UserEntity? {
        val username = this[usernameKey(uid)]?.takeIf { it.isNotBlank() }
            ?: return null
        return UserEntity.create(
            uid      = uid,
            username = username,
            email    = this[emailKey(uid)],
            photoUrl = this[photoUrlKey(uid)],
        )
    }
}