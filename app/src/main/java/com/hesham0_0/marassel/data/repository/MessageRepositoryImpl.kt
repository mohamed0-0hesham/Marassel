package com.hesham0_0.marassel.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hesham0_0.marassel.data.remote.dto.MessageDto
import com.hesham0_0.marassel.data.remote.dto.MessageDtoMapper
import com.hesham0_0.marassel.domain.model.MessageEntity
import com.hesham0_0.marassel.domain.model.MessageStatus
import com.hesham0_0.marassel.domain.model.MessageType
import com.hesham0_0.marassel.domain.repository.MessageRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val dataStore: DataStore<Preferences>,
) : MessageRepository {

    companion object {
        private const val MESSAGES_PATH = "chatapp/messages"
        private const val PENDING_KEY_PREFIX = "pending_msg"
        private const val INITIAL_MESSAGE_LIMIT = 100
    }

    private fun pendingKey(localId: String) =
        stringPreferencesKey("${PENDING_KEY_PREFIX}_$localId")

    private val messagesRef get() = firebaseDatabase.getReference(MESSAGES_PATH)

    override fun observeMessages(): Flow<List<MessageEntity>> =
        combine(
            firebaseMessagesFlow(),
            localMessagesFlow(),
        ) { firebaseMessages, localMessages ->
            mergeMessages(firebaseMessages, localMessages)
        }.distinctUntilChanged()

    private fun firebaseMessagesFlow(): Flow<List<MessageEntity>> =
        callbackFlow {
            val query = messagesRef.limitToLast(INITIAL_MESSAGE_LIMIT)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pairs = snapshot.children.mapNotNull { child ->
                        val key = child.key ?: return@mapNotNull null
                        val dto = child.getValue(MessageDto::class.java)
                            ?: return@mapNotNull null
                        key to dto
                    }
                    trySend(MessageDtoMapper.toEntityList(pairs))
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            query.addValueEventListener(listener)
            awaitClose { query.removeEventListener(listener) }
        }
            .onStart { emit(emptyList()) }
            .catch { emit(emptyList()) }

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
        runCatching {
            val dto = MessageDtoMapper.toDto(message)
            val pushRef = messagesRef.push()
            val key = pushRef.key ?: error("Firebase push() returned null key")
            pushRef.setValue(dto).await()
            key
        }

    // ── loadOlderMessages ─────────────────────────────────────────────────────

    /**
     * Fetches a page of messages older than [beforeTimestamp] from Firebase
     * using a cursor-based query:
     *
     * ```
     * messagesRef
     *   .orderByChild("timestamp")
     *   .endBefore(beforeTimestamp.toDouble())
     *   .limitToLast(limit)
     * ```
     *
     * [orderByChild("timestamp")] combined with [limitToLast] returns
     * the [limit] nodes with the largest timestamp values that are still
     * strictly less than [beforeTimestamp] — exactly the previous page.
     *
     * [endBefore] uses a Double because Firebase's Java SDK represents
     * all numeric query bounds as Double. Timestamps fit safely within
     * Double precision up to ~2^53 ≈ year 285,428 — no precision loss
     * for any realistic epoch-millisecond value.
     *
     * This is a one-shot suspend call — not a live listener. It uses
     * [get()] which returns a single snapshot and does not attach a
     * persistent listener. Offline cache is used automatically if
     * persistence is enabled (set in [FirebaseModule]).
     */
    override suspend fun loadOlderMessages(
        beforeTimestamp: Long,
        limit: Int,
    ): Result<List<MessageEntity>> = runCatching {
        val snapshot = messagesRef
            .orderByChild("timestamp")
            .endBefore(beforeTimestamp.toDouble())
            .limitToLast(limit)
            .get()
            .await()

        val pairs = snapshot.children.mapNotNull { child ->
            val key = child.key ?: return@mapNotNull null
            val dto = child.getValue(MessageDto::class.java)
                ?: return@mapNotNull null
            key to dto
        }

        MessageDtoMapper.toEntityList(pairs)
            .sortedBy { it.timestamp }
    }

    // ── deleteMessage ─────────────────────────────────────────────────────────

    /**
     * Hard-deletes a message node from Firebase and clears any matching
     * local queue entry.
     *
     * Steps:
     * 1. Remove the Firebase node at [MESSAGES_PATH]/[firebaseKey]
     * 2. Clear the local DataStore entry for [localId] (idempotent)
     *
     * The Firebase removal triggers [ValueEventListener.onChildRemoved]
     * on all connected clients, which automatically removes the message
     * from the next [observeMessages] emission — no extra signalling needed.
     *
     * If [firebaseKey] doesn't exist in Firebase the [removeValue()] call
     * is a no-op, so this method is safe to call on already-deleted messages.
     */
    override suspend fun deleteMessage(
        firebaseKey: String,
        localId: String,
    ): Result<Unit> = runCatching {
        // Step 1 — Remove from Firebase
        messagesRef
            .child(firebaseKey)
            .removeValue()
            .await()

        // Step 2 — Clear from local queue (idempotent — safe if not present)
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
                .catch { cause ->
                    if (cause is IOException) emit(emptyPreferences()) else throw cause
                }
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
                .catch { cause ->
                    if (cause is IOException) emit(emptyPreferences()) else throw cause
                }
                .map { prefs -> prefs.toLocalMessageList() }
                .first()
                .sortedBy { it.timestamp }
        }

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

    private suspend fun <T> Flow<T>.firstValue(): T = first()
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