package com.hesham0_0.marassel.domain.repository

import com.hesham0_0.marassel.domain.model.MessageEntity
import com.hesham0_0.marassel.domain.model.MessageStatus
import com.hesham0_0.marassel.domain.model.MessageType
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun observeMessages(): Flow<List<MessageEntity>>

    suspend fun sendMessage(message: MessageEntity): Result<String>

    suspend fun saveMessageLocally(message: MessageEntity): Result<Unit>

    suspend fun updateMessageStatus(
        localId: String,
        status: MessageStatus,
        firebaseKey: String? = null,
    ): Result<Unit>

    suspend fun getPendingMessages(): Result<List<MessageEntity>>

    suspend fun clearPendingMessage(localId: String): Result<Unit>

    suspend fun getLocalMessages(): Result<List<MessageEntity>>

    suspend fun loadOlderMessages(
        beforeTimestamp: Long,
        limit: Int,
    ): Result<List<MessageEntity>>

    suspend fun deleteMessage(
        firebaseKey: String,
        localId: String,
        type: MessageType
    ): Result<Unit>

    fun observeTypingUsers(): Flow<Map<String, String>>

    suspend fun setTypingStatus(uid: String, displayName: String, isTyping: Boolean): Result<Unit>
}