package com.hesham0_0.marassel.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.hesham0_0.marassel.domain.model.MessageEntity
import com.hesham0_0.marassel.domain.model.MessageStatus
import com.hesham0_0.marassel.domain.model.MessageType
import com.hesham0_0.marassel.domain.repository.MessageRepository
import com.hesham0_0.marassel.worker.WorkDataUtils.buildSendMessageInputData
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SendMessageWorkerTest {

    private lateinit var context: Context
    private lateinit var messageRepository: MessageRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        messageRepository = mockk(relaxed = true)
    }

    private fun validInput(
        localId: String = UUID.randomUUID().toString(),
        senderUid: String = "uid-1",
        senderName: String = "Alice",
        text: String = "Hello, World!",
        timestamp: Long = System.currentTimeMillis(),
        type: MessageType = MessageType.TEXT,
    ): Data {
        val entity = MessageEntity(
            localId = localId,
            firebaseKey = null,
            senderUid = senderUid,
            senderName = senderName,
            text = text,
            mediaUrl = null,
            mediaType = null,
            timestamp = timestamp,
            status = MessageStatus.PENDING,
            type = type,
        )
        return buildSendMessageInputData(entity)
    }

    private fun buildWorker(
        inputData: Data = validInput(),
        runAttemptCount: Int = 0,
    ): SendMessageWorker {
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): androidx.work.ListenableWorker {
                return SendMessageWorker(appContext, workerParameters, messageRepository)
            }
        }
        return TestListenableWorkerBuilder<SendMessageWorker>(context)
            .setInputData(inputData)
            .setRunAttemptCount(runAttemptCount)
            .setWorkerFactory(factory)
            .build()
    }

    @Test
    fun `doWork returns Result_success when sendMessage succeeds`() = runTest {
        val localId = UUID.randomUUID().toString()
        coEvery { messageRepository.updateMessageStatus(any(), any(), any()) } returns
                kotlin.Result.success(Unit)
        coEvery { messageRepository.sendMessage(any()) } returns
                kotlin.Result.success("fb-key-123")

        val result = buildWorker(validInput(localId = localId)).doWork()

        assertTrue("Expected success but got: $result", result is Result.Success)
    }

    @Test
    fun `doWork marks status PENDING before calling sendMessage`() = runTest {
        val localId = UUID.randomUUID().toString()
        coEvery { messageRepository.sendMessage(any()) } returns kotlin.Result.success("fb-key")
        coEvery { messageRepository.updateMessageStatus(any(), any(), any()) } returns
                kotlin.Result.success(Unit)

        buildWorker(validInput(localId = localId)).doWork()

        coVerify(ordering = Ordering.ORDERED) {
            messageRepository.updateMessageStatus(localId, MessageStatus.PENDING, null)
            messageRepository.sendMessage(any())
        }
    }

    @Test
    fun `doWork marks status SENT with firebase key after successful send`() = runTest {
        val localId = UUID.randomUUID().toString()
        val fbKey = "firebase-key-abc"
        coEvery { messageRepository.sendMessage(any()) } returns kotlin.Result.success(fbKey)
        coEvery { messageRepository.updateMessageStatus(any(), any(), any()) } returns
                kotlin.Result.success(Unit)

        buildWorker(validInput(localId = localId)).doWork()

        coVerify { messageRepository.updateMessageStatus(localId, MessageStatus.SENT, fbKey) }
    }

    @Test
    fun `doWork passes correct text content to sendMessage`() = runTest {
        val localId = UUID.randomUUID().toString()
        val text = "My test message"
        coEvery { messageRepository.sendMessage(any()) } returns kotlin.Result.success("fb")
        coEvery { messageRepository.updateMessageStatus(any(), any(), any()) } returns
                kotlin.Result.success(Unit)

        val captured = slot<MessageEntity>()
        coEvery { messageRepository.sendMessage(capture(captured)) } returns kotlin.Result.success("fb")

        buildWorker(validInput(localId = localId, text = text)).doWork()

        assertEquals(text, captured.captured.text)
        assertEquals(localId, captured.captured.localId)
    }

    @Test
    fun `doWork sends message with correct senderUid and senderName`() = runTest {
        val captured = slot<MessageEntity>()
        coEvery { messageRepository.sendMessage(capture(captured)) } returns kotlin.Result.success("fb")
        coEvery { messageRepository.updateMessageStatus(any(), any(), any()) } returns
                kotlin.Result.success(Unit)

        buildWorker(validInput(senderUid = "uid-xyz", senderName = "Bob")).doWork()

        assertEquals("uid-xyz", captured.captured.senderUid)
        assertEquals("Bob", captured.captured.senderName)
    }

    @Test
    fun `doWork returns Result_retry on first attempt when sendMessage fails`() = runTest {
        coEvery { messageRepository.updateMessageStatus(any(), any(), any()) } returns
                kotlin.Result.success(Unit)
        coEvery { messageRepository.sendMessage(any()) } returns
                kotlin.Result.failure(RuntimeException("timeout"))

        val result = buildWorker(runAttemptCount = 0).doWork()

        assertTrue("Expected retry but got: $result", result is Result.Retry)
    }

    @Test
    fun `doWork returns Result_retry on second attempt when sendMessage fails`() = runTest {
        coEvery { messageRepository.updateMessageStatus(any(), any(), any()) } returns
                kotlin.Result.success(Unit)
        coEvery { messageRepository.sendMessage(any()) } returns
                kotlin.Result.failure(RuntimeException("timeout"))

        val result = buildWorker(runAttemptCount = 1).doWork()

        assertTrue("Expected retry but got: $result", result is Result.Retry)
    }

    @Test
    fun `doWork returns Result_failure after all 3 attempts exhausted`() = runTest {
        coEvery { messageRepository.updateMessageStatus(any(), any(), any()) } returns
                kotlin.Result.success(Unit)
        coEvery { messageRepository.sendMessage(any()) } returns
                kotlin.Result.failure(RuntimeException("unreachable host"))

        val result = buildWorker(runAttemptCount = 2).doWork()

        assertTrue("Expected failure but got: $result", result is Result.Failure)
    }

    @Test
    fun `doWork marks status FAILED when all retries exhausted`() = runTest {
        val localId = UUID.randomUUID().toString()
        coEvery { messageRepository.updateMessageStatus(any(), any(), any()) } returns
                kotlin.Result.success(Unit)
        coEvery { messageRepository.sendMessage(any()) } returns
                kotlin.Result.failure(RuntimeException("error"))

        buildWorker(inputData = validInput(localId = localId), runAttemptCount = 2).doWork()

        coVerify {
            messageRepository.updateMessageStatus(localId, MessageStatus.FAILED, null)
        }
    }

    @Test
    fun `doWork does NOT mark FAILED on retryable attempts`() = runTest {
        val localId = UUID.randomUUID().toString()
        coEvery { messageRepository.updateMessageStatus(any(), any(), any()) } returns
                kotlin.Result.success(Unit)
        coEvery { messageRepository.sendMessage(any()) } returns
                kotlin.Result.failure(RuntimeException("error"))

        buildWorker(inputData = validInput(localId = localId), runAttemptCount = 0).doWork()

        coVerify(exactly = 0) {
            messageRepository.updateMessageStatus(localId, MessageStatus.FAILED, any())
        }
    }

    @Test
    fun `doWork returns Result_failure when input data is empty`() = runTest {
        val worker = buildWorker(inputData = workDataOf())
        val result = worker.doWork()

        assertTrue("Expected failure for empty input but got: $result", result is Result.Failure)
    }

    @Test
    fun `doWork returns Result_failure when localId is blank`() = runTest {
        val badInput = workDataOf(
            WorkerKeys.KEY_LOCAL_ID to "",
            WorkerKeys.KEY_SENDER_UID to "uid-1",
            WorkerKeys.KEY_SENDER_NAME to "Alice",
            WorkerKeys.KEY_CONTENT to "Hello",
            WorkerKeys.KEY_TIMESTAMP to System.currentTimeMillis(),
        )
        val result = buildWorker(inputData = badInput).doWork()

        assertTrue("Expected failure for blank localId but got: $result", result is Result.Failure)
    }

    @Test
    fun `doWork returns Result_failure when senderUid is missing`() = runTest {
        val badInput = workDataOf(
            WorkerKeys.KEY_LOCAL_ID to UUID.randomUUID().toString(),
            WorkerKeys.KEY_SENDER_NAME to "Alice",
            WorkerKeys.KEY_CONTENT to "Hello",
        )
        val result = buildWorker(inputData = badInput).doWork()

        assertTrue(
            "Expected failure for missing senderUid but got: $result",
            result is Result.Failure
        )
    }

    @Test
    fun `doWork does not call sendMessage when input validation fails`() = runTest {
        buildWorker(inputData = workDataOf()).doWork()

        coVerify(exactly = 0) { messageRepository.sendMessage(any()) }
    }

    @Test
    fun `buildSendMessageInputData uses KEY_CONTENT for text field`() {
        val entity = MessageEntity(
            localId = "local-1",
            firebaseKey = null,
            senderUid = "uid-1",
            senderName = "Alice",
            text = "Test text",
            mediaUrl = null,
            mediaType = null,
            timestamp = 1_000L,
            status = MessageStatus.PENDING,
            type = MessageType.TEXT,
        )
        val data = buildSendMessageInputData(entity)

        val content = data.getString(WorkerKeys.KEY_CONTENT)
        assertEquals("Test text", content)
    }

    @Test
    fun `toSendMessageParams deserializes all fields correctly`() {
        val entity = MessageEntity(
            localId = "local-abc",
            firebaseKey = null,
            senderUid = "uid-999",
            senderName = "Charlie",
            text = "Round-trip test",
            mediaUrl = null,
            mediaType = null,
            timestamp = 999_000L,
            status = MessageStatus.PENDING,
            type = MessageType.TEXT,
        )
        val data = buildSendMessageInputData(entity)
        val params = with(WorkDataUtils) { data.toSendMessageParams() }!!

        assertEquals("local-abc", params.localId)
        assertEquals("uid-999", params.senderUid)
        assertEquals("Charlie", params.senderName)
        assertEquals("Round-trip test", params.content)
        assertEquals(999_000L, params.timestamp)
        assertEquals(MessageType.TEXT, params.messageType)
    }

    @Test
    fun `getForegroundInfo returns ForegroundInfo without throwing`() = runTest {
        val worker = buildWorker()

        val info = runCatching { worker.getForegroundInfo() }

        assertTrue("getForegroundInfo should not throw", info.isSuccess)
    }
}