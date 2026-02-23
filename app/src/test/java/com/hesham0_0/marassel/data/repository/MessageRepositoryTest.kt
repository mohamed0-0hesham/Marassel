package com.hesham0_0.marassel.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hesham0_0.marassel.data.remote.FirebaseMessageDataSource
import com.hesham0_0.marassel.data.remote.FirebaseStorageDataSource
import com.hesham0_0.marassel.domain.model.MessageEntity
import com.hesham0_0.marassel.domain.model.MessageStatus
import com.hesham0_0.marassel.domain.model.MessageType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class   MessageRepositoryTest {

    private lateinit var firebaseDataSource: FirebaseMessageDataSource
    private lateinit var storageDataSource: FirebaseStorageDataSource
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: MessageRepositoryImpl

    @Before
    fun setUp() {
        firebaseDataSource = mockk()
        storageDataSource = mockk()
        dataStore = mockk()

        every { dataStore.data } returns flowOf(emptyPreferences())
        // edit is an inline function that calls updateData
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val prefs = mutablePreferencesOf()
            transform(prefs)
            prefs
        }
        every { firebaseDataSource.observeMessages() } returns flowOf(emptyList())
        every { firebaseDataSource.observeTypingUsers() } returns flowOf(emptyMap())

        repository = MessageRepositoryImpl(
            firebaseDataSource = firebaseDataSource,
            firebaseStorageDataSource = storageDataSource,
            dataStore = dataStore,
        )
    }

    private fun pending(
        localId: String = "local-1",
        timestamp: Long = 1_000L,
        status: MessageStatus = MessageStatus.PENDING,
        senderUid: String = "uid-1",
    ) = MessageEntity(
        localId = localId,
        firebaseKey = null,
        senderUid = senderUid,
        senderName = "Alice",
        text = "Hello",
        mediaUrl = null,
        mediaType = null,
        timestamp = timestamp,
        status = status,
        type = MessageType.TEXT,
    )

    private fun sent(
        localId: String = "local-1",
        firebaseKey: String = "fb-1",
        timestamp: Long = 1_000L,
    ) = pending(localId = localId, timestamp = timestamp, status = MessageStatus.SENT)
        .copy(firebaseKey = firebaseKey)

    private fun prefsWithMessages(messages: List<MessageEntity>): Preferences {
        val prefs = mutablePreferencesOf()
        messages.forEach { msg ->
            val key = stringPreferencesKey("pending_msg_${msg.localId}")
            val json = buildMessageJson(msg)
            prefs[key] = json
        }
        return prefs
    }

    private fun buildMessageJson(msg: MessageEntity): String = """
        {
          "localId":     "${msg.localId}",
          "firebaseKey": ${msg.firebaseKey?.let { "\"$it\"" } ?: "null"},
          "senderUid":   "${msg.senderUid}",
          "senderName":  "${msg.senderName}",
          "text":        ${msg.text?.let { "\"$it\"" } ?: "null"},
          "mediaUrl":    ${msg.mediaUrl?.let { "\"$it\"" } ?: "null"},
          "mediaType":   ${msg.mediaType?.let { "\"$it\"" } ?: "null"},
          "timestamp":   ${msg.timestamp},
          "status":      "${msg.status.name}",
          "type":        "${msg.type.name}",
          "replyToId":   ${msg.replyToId?.let { "\"$it\"" } ?: "null"}
        }
    """.trimIndent()

    @Test
    fun `observeMessages emits only Firebase messages when DataStore is empty`() = runTest {
        val remote = listOf(sent("local-1", "fb-1"))
        every { firebaseDataSource.observeMessages() } returns flowOf(remote)

        val result = repository.observeMessages().first()

        assertEquals(1, result.size)
        assertEquals("fb-1", result[0].firebaseKey)
        assertEquals(MessageStatus.SENT, result[0].status)
    }

    @Test
    fun `observeMessages deduplicates PENDING local when Firebase confirms it`() = runTest {
        val localPending = pending(localId = "local-1")
        val remoteSent = sent(localId = "local-1", firebaseKey = "fb-1")

        every { firebaseDataSource.observeMessages() } returns flowOf(listOf(remoteSent))
        every { dataStore.data } returns flowOf(prefsWithMessages(listOf(localPending)))

        val result = repository.observeMessages().first()

        assertEquals(1, result.size)
        assertEquals(MessageStatus.SENT, result[0].status)
        assertEquals("fb-1", result[0].firebaseKey)
    }

    @Test
    fun `observeMessages keeps PENDING local not yet confirmed by Firebase`() = runTest {
        val localUnconfirmed = pending(localId = "local-unconfirmed", timestamp = 2_000L)
        val remoteOther = sent(localId = "other", firebaseKey = "fb-other", timestamp = 1_000L)

        every { firebaseDataSource.observeMessages() } returns flowOf(listOf(remoteOther))
        every { dataStore.data } returns flowOf(prefsWithMessages(listOf(localUnconfirmed)))

        val result = repository.observeMessages().first()

        assertEquals(2, result.size)
        assertTrue(result.any { it.localId == "local-unconfirmed" && it.isPending })
        assertTrue(result.any { it.localId == "other" && it.isSent })
    }

    @Test
    fun `observeMessages does not include SENT local messages that match Firebase`() = runTest {
        val sentLocal = sent(localId = "local-1", firebaseKey = "fb-1")
        val remoteSent = sent(localId = "local-1", firebaseKey = "fb-1")

        every { firebaseDataSource.observeMessages() } returns flowOf(listOf(remoteSent))
        every { dataStore.data } returns flowOf(prefsWithMessages(listOf(sentLocal)))

        val result = repository.observeMessages().first()

        assertEquals(1, result.size)
    }

    @Test
    fun `observeMessages sorts all merged messages by timestamp ascending`() = runTest {
        val late = sent(localId = "late", firebaseKey = "fb-late", timestamp = 3_000L)
        val early = sent(localId = "early", firebaseKey = "fb-early", timestamp = 1_000L)
        val mid = pending(localId = "mid", timestamp = 2_000L)

        every { firebaseDataSource.observeMessages() } returns flowOf(listOf(late, early))
        every { dataStore.data } returns flowOf(prefsWithMessages(listOf(mid)))

        val result = repository.observeMessages().first()

        assertEquals(3, result.size)
        assertTrue(result[0].timestamp <= result[1].timestamp)
        assertTrue(result[1].timestamp <= result[2].timestamp)
        assertEquals("early", result[0].localId)
        assertEquals("mid", result[1].localId)
        assertEquals("late", result[2].localId)
    }

    @Test
    fun `observeMessages emits empty list when both Firebase and DataStore are empty`() = runTest {
        val result = repository.observeMessages().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `sendMessage delegates to firebaseDataSource and returns success`() = runTest {
        val msg = pending()
        coEvery { firebaseDataSource.sendMessage(msg) } returns Result.success("fb-new")

        val result = repository.sendMessage(msg)

        assertTrue(result.isSuccess)
        assertEquals("fb-new", result.getOrNull())
        coVerify(exactly = 1) { firebaseDataSource.sendMessage(msg) }
    }

    @Test
    fun `sendMessage propagates Firebase failure as Result failure`() = runTest {
        val cause = RuntimeException("network timeout")
        coEvery { firebaseDataSource.sendMessage(any()) } returns Result.failure(cause)

        val result = repository.sendMessage(pending())

        assertTrue(result.isFailure)
        assertEquals(cause, result.exceptionOrNull())
    }

    @Test
    fun `loadOlderMessages delegates to firebaseDataSource with correct args`() = runTest {
        val messages = listOf(sent())
        coEvery {
            firebaseDataSource.loadOlderMessages(
                5_000L,
                20
            )
        } returns Result.success(messages)

        val result = repository.loadOlderMessages(5_000L, 20)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        coVerify(exactly = 1) { firebaseDataSource.loadOlderMessages(5_000L, 20) }
    }

    @Test
    fun `loadOlderMessages propagates Firebase failure`() = runTest {
        coEvery { firebaseDataSource.loadOlderMessages(any(), any()) } returns
                Result.failure(RuntimeException("not found"))

        val result = repository.loadOlderMessages(5_000L, 20)

        assertTrue(result.isFailure)
    }

    @Test
    fun `saveMessageLocally calls dataStore edit exactly once`() = runTest {
        val result = repository.saveMessageLocally(pending())

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { dataStore.updateData(any()) }
    }

    @Test
    fun `saveMessageLocally returns failure when dataStore throws`() = runTest {
        coEvery { dataStore.updateData(any()) } throws RuntimeException("disk full")

        val result = repository.saveMessageLocally(pending())

        assertTrue(result.isFailure)
    }

    @Test
    fun `updateMessageStatus returns failure when localId not in DataStore`() = runTest {
        coEvery { dataStore.updateData(any()) } throws NoSuchElementException("key not found inside edit block")

        val result = repository.updateMessageStatus("nonexistent", MessageStatus.SENT, "fb-key")

        assertTrue(result.isFailure)
    }

    @Test
    fun `clearPendingMessage calls dataStore edit and returns success`() = runTest {
        val result = repository.clearPendingMessage("local-1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { dataStore.updateData(any()) }
    }

    @Test
    fun `clearPendingMessage returns failure when dataStore throws`() = runTest {
        coEvery { dataStore.updateData(any()) } throws RuntimeException("io error")

        val result = repository.clearPendingMessage("local-1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getPendingMessages returns only PENDING messages`() = runTest {
        val p1 = pending(localId = "p1", status = MessageStatus.PENDING, timestamp = 1_000L)
        val p2 = pending(localId = "p2", status = MessageStatus.FAILED, timestamp = 2_000L)
        val p3 = pending(localId = "p3", status = MessageStatus.PENDING, timestamp = 3_000L)

        every { dataStore.data } returns flowOf(prefsWithMessages(listOf(p1, p2, p3)))

        val result = repository.getPendingMessages()

        assertTrue(result.isSuccess)
        val messages = result.getOrNull()!!
        assertEquals(2, messages.size)
        assertTrue(messages.all { it.status == MessageStatus.PENDING })
        assertEquals("p1", messages[0].localId)
        assertEquals("p3", messages[1].localId)
    }

    @Test
    fun `getPendingMessages returns empty list when no pending messages`() = runTest {
        val result = repository.getPendingMessages()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `getLocalMessages returns all messages sorted by timestamp`() = runTest {
        val m1 = pending(localId = "m1", timestamp = 3_000L)
        val m2 = pending(localId = "m2", timestamp = 1_000L)
        val m3 = pending(localId = "m3", timestamp = 2_000L, status = MessageStatus.FAILED)

        every { dataStore.data } returns flowOf(prefsWithMessages(listOf(m1, m2, m3)))

        val result = repository.getLocalMessages()

        assertTrue(result.isSuccess)
        val messages = result.getOrNull()!!
        assertEquals(3, messages.size)
        assertEquals("m2", messages[0].localId) // 1_000L
        assertEquals("m3", messages[1].localId) // 2_000L
        assertEquals("m1", messages[2].localId) // 3_000L
    }

    @Test
    fun `deleteMessage calls firebase deleteMessage then clearPendingMessage for TEXT`() = runTest {
        coEvery { firebaseDataSource.deleteMessage("fb-1") } returns Result.success(Unit)

        val result = repository.deleteMessage("fb-1", "local-1", MessageType.TEXT)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { firebaseDataSource.deleteMessage("fb-1") }
        coVerify(exactly = 1) { dataStore.updateData(any()) }
        coVerify(exactly = 0) { storageDataSource.deleteMediaForMessage(any()) }
    }

    @Test
    fun `deleteMessage calls storage deleteMediaForMessage for IMAGE messages`() = runTest {
        coEvery { firebaseDataSource.deleteMessage("fb-img") } returns Result.success(Unit)
        coEvery { storageDataSource.deleteMediaForMessage("local-img") } returns Result.success(Unit)

        val result = repository.deleteMessage("fb-img", "local-img", MessageType.IMAGE)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { storageDataSource.deleteMediaForMessage("local-img") }
    }

    @Test
    fun `deleteMessage calls storage deleteMediaForMessage for VIDEO messages`() = runTest {
        coEvery { firebaseDataSource.deleteMessage("fb-vid") } returns Result.success(Unit)
        coEvery { storageDataSource.deleteMediaForMessage("local-vid") } returns Result.success(Unit)

        val result = repository.deleteMessage("fb-vid", "local-vid", MessageType.VIDEO)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { storageDataSource.deleteMediaForMessage("local-vid") }
    }

    @Test
    fun `deleteMessage returns failure when Firebase delete throws`() = runTest {
        coEvery { firebaseDataSource.deleteMessage(any()) } throws RuntimeException("forbidden")

        val result = repository.deleteMessage("fb-1", "local-1", MessageType.TEXT)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { dataStore.updateData(any()) }
    }

    @Test
    fun `deleteMessage returns failure when storage delete fails`() = runTest {
        coEvery { firebaseDataSource.deleteMessage("fb-img") } returns Result.success(Unit)
        coEvery { storageDataSource.deleteMediaForMessage("local-img") } returns
                Result.failure(RuntimeException("storage error"))

        val result = repository.deleteMessage("fb-img", "local-img", MessageType.IMAGE)

        assertTrue(result.isFailure)
    }

    @Test
    fun `observeTypingUsers delegates to firebaseDataSource`() = runTest {
        val typingMap = mapOf("uid-2" to "Bob")
        every { firebaseDataSource.observeTypingUsers() } returns flowOf(typingMap)

        val result = repository.observeTypingUsers().first()

        assertEquals(typingMap, result)
    }

    @Test
    fun `setTypingStatus delegates to firebaseDataSource`() = runTest {
        coEvery { firebaseDataSource.setTypingStatus("uid-1", "Alice", true) } returns
                Result.success(Unit)

        val result = repository.setTypingStatus("uid-1", "Alice", true)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { firebaseDataSource.setTypingStatus("uid-1", "Alice", true) }
    }

    @Test
    fun `observeMessages emits empty local list when DataStore throws IOException`() = runTest {
        every { dataStore.data } returns kotlinx.coroutines.flow.flow {
            throw java.io.IOException("disk error")
        }
        every { firebaseDataSource.observeMessages() } returns flowOf(emptyList())

        val result = repository.observeMessages().first()
        assertTrue(result.isEmpty())
    }
}