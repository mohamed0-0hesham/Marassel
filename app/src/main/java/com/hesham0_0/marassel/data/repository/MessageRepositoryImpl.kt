package com.hesham0_0.marassel.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hesham0_0.marassel.data.remote.FirebaseMessageDataSource
import com.hesham0_0.marassel.data.remote.FirebaseStorageDataSource
import com.hesham0_0.marassel.domain.model.MessageEntity
import com.hesham0_0.marassel.domain.model.MessageStatus
import com.hesham0_0.marassel.domain.model.MessageType
import com.hesham0_0.marassel.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseMessageDataSource,
    private val firebaseStorageDataSource: FirebaseStorageDataSource,
    private val dataStore: DataStore<Preferences>,
) : MessageRepository {

    companion object {
        private const val PENDING_KEY_PREFIX = "pending_msg"
    }

    private fun pendingKey(localId: String) =
        stringPreferencesKey("${PENDING_KEY_PREFIX}_$localId")

    // ── observeMessages ───────────────────────────────────────────────────────

    override fun observeMessages(): Flow<List<MessageEntity>> =
        combine(
            firebaseDataSource.observeMessages(),
            localMessagesFlow(),
        ) { firebaseMessages, localMessages ->
            mergeMessages(firebaseMessages, localMessages)
        }.distinctUntilChanged()

    private fun localMessagesFlow(): Flow<List<MessageEntity>> =
        dataStore.data
            .catch { cause ->
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }
            .map { prefs -> prefs.toLocalMessageList() }
            .onStart { emit(emptyList()) }

    private fun mergeMessages(
        firebaseMessages: List<MessageEntity>,
        localMessages: List<MessageEntity>,
    ): List<MessageEntity> {
        val firebaseById = firebaseMessages.associateBy { it.localId }
        val nonDuplicateLocals = localMessages.filter { local ->
            local.status != MessageStatus.SENT &&
                    !firebaseById.containsKey(local.localId)
        }
        return (firebaseMessages + nonDuplicateLocals).sortedBy { it.timestamp }
    }

    // ── sendMessage ───────────────────────────────────────────────────────────

    override suspend fun sendMessage(message: MessageEntity): Result<String> =
        firebaseDataSource.sendMessage(message)

    // ── loadOlderMessages ─────────────────────────────────────────────────────

    override suspend fun loadOlderMessages(
        beforeTimestamp: Long,
        limit: Int,
    ): Result<List<MessageEntity>> =
        firebaseDataSource.loadOlderMessages(
            beforeTimestamp = beforeTimestamp,
            limit = limit,
        )

    // ── deleteMessage ─────────────────────────────────────────────────────────

    override suspend fun deleteMessage(
        firebaseKey: String,
        localId: String,
        type: MessageType
    ): Result<Unit> = runCatching {
        firebaseDataSource.deleteMessage(firebaseKey)
            .getOrElse { throw it }

        // Only delete associated media if it is NOT a text message
        if (type != MessageType.TEXT) {
            firebaseStorageDataSource.deleteMediaForMessage(localId)
        }

        clearPendingMessage(localId)
            .getOrElse { throw it }
    }

    // ── saveMessageLocally ────────────────────────────────────────────────────

    override suspend fun saveMessageLocally(message: MessageEntity): Result<Unit> =
        runCatching {
            val json = Json.encodeToString(message.toSerializable())
            dataStore.edit { prefs -> prefs[pendingKey(message.localId)] = json }
            Unit
        }

    override suspend fun updateMessageStatus(
        localId: String,
        status: MessageStatus,
        firebaseKey: String?,
    ): Result<Unit> = runCatching {
        val key = pendingKey(localId)
        dataStore.edit { prefs ->
            val existing = prefs[key]
                ?: throw NoSuchElementException("No local message with localId=$localId")
            val current = Json.decodeFromString<MessageSerializable>(existing).toEntity()
            val updated = current.copy(
                status = status,
                firebaseKey = firebaseKey ?: current.firebaseKey,
            )
            prefs[key] = Json.encodeToString(updated.toSerializable())
        }
        Unit
    }

    // ── getPendingMessages ────────────────────────────────────────────────────

    override suspend fun getPendingMessages(): Result<List<MessageEntity>> =
        runCatching {
            dataStore.data
                .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
                .map { prefs -> prefs.toLocalMessageList() }
                .first()
                .filter { it.status == MessageStatus.PENDING }
                .sortedBy { it.timestamp }
        }

    // ── clearPendingMessage ───────────────────────────────────────────────────

    override suspend fun clearPendingMessage(localId: String): Result<Unit> =
        runCatching {
            dataStore.edit { prefs -> prefs.remove(pendingKey(localId)) }
            Unit
        }

    // ── getLocalMessages ──────────────────────────────────────────────────────

    override suspend fun getLocalMessages(): Result<List<MessageEntity>> =
        runCatching {
            dataStore.data
                .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
                .map { prefs -> prefs.toLocalMessageList() }
                .first()
                .sortedBy { it.timestamp }
        }

    // ── Typing Presence ───────────────────────────────────────────────────────

    override fun observeTypingUsers(): Flow<Map<String, String>> =
        firebaseDataSource.observeTypingUsers()

    override suspend fun setTypingStatus(
        uid: String,
        displayName: String,
        isTyping: Boolean
    ): Result<Unit> =
        firebaseDataSource.setTypingStatus(uid, displayName, isTyping)

    // ── DataStore helpers ─────────────────────────────────────────────────────

    private fun Preferences.toLocalMessageList(): List<MessageEntity> =
        asMap()
            .entries
            .filter { (key, _) -> key.name.startsWith(PENDING_KEY_PREFIX) }
            .mapNotNull { (_, value) ->
                runCatching {
                    Json.decodeFromString<MessageSerializable>(value as String).toEntity()
                }.getOrNull()
            }
}

// ── Serialization bridge ──────────────────────────────────────────────────────

@Serializable
private data class MessageSerializable(
    val localId: String,
    val firebaseKey: String?,
    val senderUid: String,
    val senderName: String,
    val text: String?,
    val mediaUrl: String?,
    val mediaType: String?,
    val timestamp: Long,
    val status: String,
    val type: String,
    val replyToId: String?,
)

private fun MessageEntity.toSerializable() = MessageSerializable(
    localId = localId,
    firebaseKey = firebaseKey,
    senderUid = senderUid,
    senderName = senderName,
    text = text,
    mediaUrl = mediaUrl,
    mediaType = mediaType,
    timestamp = timestamp,
    status = status.name,
    type = type.name,
    replyToId = replyToId,
)

private fun MessageSerializable.toEntity() = MessageEntity(
    localId = localId,
    firebaseKey = firebaseKey,
    senderUid = senderUid,
    senderName = senderName,
    text = text,
    mediaUrl = mediaUrl,
    mediaType = mediaType,
    timestamp = timestamp,
    status = MessageStatus.valueOf(status),
    type = MessageType.fromString(type),
    replyToId = replyToId,
)