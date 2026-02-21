package com.hesham0_0.marassel.data.remote

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.hesham0_0.marassel.data.remote.dto.MessageDto
import com.hesham0_0.marassel.data.remote.dto.MessageDtoMapper
import com.hesham0_0.marassel.domain.model.MessageEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMessageDataSource @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
) {

    companion object {
        private const val MESSAGES_PATH = "marassel/messages"
        private const val TYPING_PATH = "marassel/typing"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val INITIAL_LOAD_LIMIT = 100

        const val UPLOAD_NOTIFICATION_ID = 1001
    }

    private val messagesRef get() = firebaseDatabase.getReference(MESSAGES_PATH)
    private val typingRef get() = firebaseDatabase.getReference(TYPING_PATH)

    fun observeMessages(): Flow<List<MessageEntity>> =
        callbackFlow {
            val query = messagesRef.limitToLast(INITIAL_LOAD_LIMIT)

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

    suspend fun sendMessage(message: MessageEntity): Result<String> =
        runCatching {
            val dto = MessageDtoMapper.toDto(message)
            val dtoMap = buildDtoMap(dto)          // inject ServerValue.TIMESTAMP
            val pushRef = messagesRef.push()
            val key = pushRef.key
                ?: error("Firebase push() returned a null key — this should not happen")
            pushRef.setValue(dtoMap).await()
            key
        }

    private fun buildDtoMap(dto: MessageDto): Map<String, Any?> = mapOf(
        "sender_uid" to dto.senderUid,
        "sender_name" to dto.senderName,
        "text" to dto.text,
        "media_url" to dto.mediaUrl,
        "media_type" to dto.mediaType,
        "timestamp" to ServerValue.TIMESTAMP,   // ← server-side stamp
        "type" to dto.type,
        "local_id" to dto.localId,
        "reply_to_id" to dto.replyToId,
    )

    suspend fun loadOlderMessages(
        beforeTimestamp: Long,
        limit: Int,
    ): Result<List<MessageEntity>> = runCatching {
        val snapshot = messagesRef
            .orderByChild(FIELD_TIMESTAMP)
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

    suspend fun deleteMessage(firebaseKey: String): Result<Unit> =
        runCatching {
            messagesRef
                .child(firebaseKey)
                .removeValue()
                .await()
            Unit
        }

    fun observeTypingUsers(): Flow<Map<String, String>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val typingMap = mutableMapOf<String, String>()
                snapshot.children.forEach { child ->
                    val uid = child.key
                    val name = child.getValue(String::class.java)
                    if (uid != null && name != null) {
                        typingMap[uid] = name
                    }
                }
                trySend(typingMap)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        typingRef.addValueEventListener(listener)
        awaitClose { typingRef.removeEventListener(listener) }
    }

    suspend fun setTypingStatus(uid: String, displayName: String, isTyping: Boolean): Result<Unit> =
        runCatching {
            val userTypingRef = typingRef.child(uid)
            if (isTyping) {
                userTypingRef.setValue(displayName).await()
                // Ensure it gets cleaned up if the app crashes or disconnects
                userTypingRef.onDisconnect().removeValue()
            } else {
                userTypingRef.removeValue().await()
                userTypingRef.onDisconnect().cancel()
            }
            Unit
        }
}