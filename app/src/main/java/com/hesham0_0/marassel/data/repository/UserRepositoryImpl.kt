package com.hesham0_0.marassel.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hesham0_0.marassel.data.repository.UserRepositoryImpl.Keys.PREF_DEVICE_ID
import com.hesham0_0.marassel.data.repository.UserRepositoryImpl.Keys.PREF_USERNAME
import kotlinx.coroutines.flow.first

import com.hesham0_0.marassel.domain.model.UserEntity
import com.hesham0_0.marassel.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserRepository {

    private companion object Keys {
        val PREF_USERNAME  = stringPreferencesKey("user_username")
        val PREF_DEVICE_ID = stringPreferencesKey("user_device_id")
    }


    private val userFlow: Flow<UserEntity?> = dataStore.data
        .catch { cause ->
            // Only recover from I/O errors — surface everything else
            if (cause is IOException) {
                emit(emptyPreferences())
            } else {
                throw cause
            }
        }
        .map { preferences ->
            preferences.toUserEntity()
        }

    override suspend fun saveUser(user: UserEntity): Result<Unit> =
        runCatching {
            dataStore.edit { preferences ->
                preferences[PREF_USERNAME]  = user.username
                preferences[PREF_DEVICE_ID] = user.deviceId
            }
            Unit
        }

    override suspend fun getUser(): Result<UserEntity?> =
        runCatching {
            dataStore.data
                .catch { cause ->
                    if (cause is IOException) emit(emptyPreferences())
                    else throw cause
                }
                .map { it.toUserEntity() }
                .first()
        }

    override fun observeUser(): Flow<UserEntity?> = userFlow

    override suspend fun clearUser(): Result<Unit> =
        runCatching {
            dataStore.edit { preferences ->
                preferences.remove(PREF_USERNAME)
                preferences.remove(PREF_DEVICE_ID)
            }
            Unit
        }


    private fun Preferences.toUserEntity(): UserEntity? {
        val username = this[PREF_USERNAME]  ?: return null
        val deviceId = this[PREF_DEVICE_ID] ?: return null

        // Guard against blank values that could slip through
        if (username.isBlank() || deviceId.isBlank()) return null

        return UserEntity.create(
            username = username,
            deviceId = deviceId,
        )
    }
}

// ── Import for first() ────────────────────────────────────────────────────────
// Needs to be a top-level import — placed here to keep the class clean
private suspend fun <T> Flow<T>.firstValue(): T = first()
