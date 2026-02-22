package com.hesham0_0.marassel.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker.Result
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
import java.lang.reflect.Field
import java.util.UUID

/**
 * Instrumented unit tests for [SendMessageWorker].
 *
 * Run on a device/emulator (androidTest source set) because
 * [TestListenableWorkerBuilder] requires an Android [Context].
 *
 * All Firebase/repository calls are faked with MockK.
 * The worker's injected [MessageRepository] is set via reflection
 * because [SendMessageWorker] uses `@AssistedInject` — the Hilt worker
 * factory is not available in the WorkManager test harness, so we
 * bypass DI and inject the mock directly.
 *
 * Key worker constants validated here:
 * - Input key [WorkerKeys.KEY_CONTENT] (not KEY_MESSAGE_TEXT)
 * - MAX_ATTEMPTS = 3 (retry on attempt 0–1, fail on attempt 2+)
 * - Status sequence: PENDING → SENT (success) or PENDING → FAILED (exhausted)
 */
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Builds valid [Data] using the same helper the production code uses. */
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

    /**
     * Creates a [SendMessageWorker] and injects [messageRepository] by
     * reflection. [TestListenableWorkerBuilder] bypasses Hilt so the
     * @AssistedInject factory is never called.
     */
    private fun buildWorker(
        inputData: Data = validInput(),
        runAttemptCount: Int = 0,
    ): SendMessageWorker {
        val worker = TestListenableWorkerBuilder<SendMessageWorker>(context)
            .setInputData(inputData)
            .setRunAttemptCount(runAttemptCount)
            .build()

        injectField(worker, "messageRepository", messageRepository)
        return worker
    }

    /** Sets a private field on the worker via reflection. */
    private fun injectField(target: Any, fieldName: String, value: Any) {
        fun findField(cls: Class<*>, name: String): Field? =
            try {
                cls.getDeclaredField(name)
            } catch (_: NoSuchFieldException) {
                cls.superclass?.let { findField(it, name) }
            }

        val field = findField(target::class.java, fieldName)
            ?: error("Field '$fieldName' not found on ${target::class.java.name}")
        field.isAccessible = true
        field.set(target, value)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Success path
    // ══════════════════════════════════════════════════════════════════════════

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

        // PENDING must be set BEFORE sendMessage is called
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

    // ══════════════════════════════════════════════════════════════════════════
    // Retry path (attempts 0 and 1 of 3)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `doWork returns Result_retry on first attempt when sendMessage fails`() = runTest {
        coEvery { messageRepository.updateMessageStatus(any(), any(), any()) } returns
                kotlin.Result.success(Unit)
        coEvery { messageRepository.sendMessage(any()) } returns
                kotlin.Result.failure(RuntimeException("timeout"))

        // runAttemptCount = 0 → first attempt → should retry
        val result = buildWorker(runAttemptCount = 0).doWork()

        assertTrue("Expected retry but got: $result", result is Result.Retry)
    }

    @Test
    fun `doWork returns Result_retry on second attempt when sendMessage fails`() = runTest {
        coEvery { messageRepository.updateMessageStatus(any(), any(), any()) } returns
                kotlin.Result.success(Unit)
        coEvery { messageRepository.sendMessage(any()) } returns
                kotlin.Result.failure(RuntimeException("timeout"))

        // runAttemptCount = 1 → second attempt → still below MAX_ATTEMPTS - 1 = 2 → retry
        val result = buildWorker(runAttemptCount = 1).doWork()

        assertTrue("Expected retry but got: $result", result is Result.Retry)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Failure path (attempt 2 = MAX_ATTEMPTS - 1)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `doWork returns Result_failure after all 3 attempts exhausted`() = runTest {
        coEvery { messageRepository.updateMessageStatus(any(), any(), any()) } returns
                kotlin.Result.success(Unit)
        coEvery { messageRepository.sendMessage(any()) } returns
                kotlin.Result.failure(RuntimeException("unreachable host"))

        // runAttemptCount = 2 → third attempt = MAX_ATTEMPTS - 1 → give up
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

        // First attempt — should retry, not fail
        buildWorker(inputData = validInput(localId = localId), runAttemptCount = 0).doWork()

        coVerify(exactly = 0) {
            messageRepository.updateMessageStatus(localId, MessageStatus.FAILED, any())
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Missing / malformed input
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `doWork returns Result_failure when input data is empty`() = runTest {
        val worker = buildWorker(inputData = workDataOf())
        val result = worker.doWork()

        assertTrue("Expected failure for empty input but got: $result", result is Result.Failure)
    }

    @Test
    fun `doWork returns Result_failure when localId is blank`() = runTest {
        // Build data manually with blank localId — bypasses buildSendMessageInputData helper
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

    // ══════════════════════════════════════════════════════════════════════════
    // WorkDataUtils integration — KEY_CONTENT key is used (not KEY_MESSAGE_TEXT)
    // ══════════════════════════════════════════════════════════════════════════

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

        // KEY_CONTENT = "content" — this is what SendMessageWorker reads via toSendMessageParams()
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

    // ══════════════════════════════════════════════════════════════════════════
    // getForegroundInfo — does not throw
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getForegroundInfo returns ForegroundInfo without throwing`() = runTest {
        val worker = buildWorker()

        val info = runCatching { worker.getForegroundInfo() }

        assertTrue("getForegroundInfo should not throw", info.isSuccess)
    }
}