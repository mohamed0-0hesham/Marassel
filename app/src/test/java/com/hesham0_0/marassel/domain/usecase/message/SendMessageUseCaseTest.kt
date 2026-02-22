package com.hesham0_0.marassel.domain.usecase.message

import com.hesham0_0.marassel.domain.model.AuthProvider
import com.hesham0_0.marassel.domain.model.AuthUser
import com.hesham0_0.marassel.domain.model.MessageEntity
import com.hesham0_0.marassel.domain.model.MessageStatus
import com.hesham0_0.marassel.domain.model.MessageType
import com.hesham0_0.marassel.domain.model.UserEntity
import com.hesham0_0.marassel.domain.repository.AuthRepository
import com.hesham0_0.marassel.domain.repository.MessageRepository
import com.hesham0_0.marassel.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SendMessageUseCase].
 *
 * Dependencies are mocked with MockK. No Android framework required.
 *
 * Covers:
 * - Text message: validation, auth guard, profile guard, persistence
 * - Media message: media validation, auth guard, profile guard
 * - Failure propagation from [MessageRepository.saveMessageLocally]
 */
class SendMessageUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private lateinit var messageRepository: MessageRepository
    private lateinit var useCase: SendMessageUseCase

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private val authUser = AuthUser(
        uid = "uid-alice",
        email = "alice@example.com",
        displayName = "Alice",
        photoUrl = null,
        isEmailVerified = true,
        provider = AuthProvider.EMAIL_PASSWORD,
    )

    private val userProfile = UserEntity(
        uid = "uid-alice",
        username = "Alice",
        email = "alice@example.com",
        photoUrl = null,
        initials = "A",
    )

    @Before
    fun setUp() {
        authRepository = mockk()
        userRepository = mockk()
        messageRepository = mockk()
        useCase = SendMessageUseCase(authRepository, userRepository, messageRepository)

        // Happy-path defaults
        coEvery { authRepository.getCurrentUser() } returns authUser
        coEvery { userRepository.getProfile("uid-alice") } returns Result.success(userProfile)
        coEvery { messageRepository.saveMessageLocally(any()) } returns Result.success(Unit)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // invoke (text message)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `text message returns Success with entity matching input text`() = runTest {
        val result = useCase("Hello, World!") as SendMessageResult.Success

        assertEquals("Hello, World!", result.message.text)
        assertEquals("uid-alice", result.message.senderUid)
        assertEquals("Alice", result.message.senderName)
        assertEquals(MessageType.TEXT, result.message.type)
        assertEquals(MessageStatus.PENDING, result.message.status)
    }

    @Test
    fun `text message trims whitespace from content`() = runTest {
        val result = useCase("  Hello  ") as SendMessageResult.Success
        assertEquals("Hello", result.message.text)
    }

    @Test
    fun `text message returns ValidationFailed for blank text`() = runTest {
        val result = useCase("")
        assertTrue(result is SendMessageResult.ValidationFailed)
        val vf = result as SendMessageResult.ValidationFailed
        assertTrue(vf.reason is MessageValidator.ValidationResult.BlankText)
    }

    @Test
    fun `text message returns ValidationFailed for whitespace-only text`() = runTest {
        val result = useCase("   ")
        assertTrue(result is SendMessageResult.ValidationFailed)
    }

    @Test
    fun `text message returns ValidationFailed for text exceeding MAX_TEXT_LENGTH`() = runTest {
        val result = useCase("a".repeat(MessageValidator.MAX_TEXT_LENGTH + 1))
        assertTrue(result is SendMessageResult.ValidationFailed)
        val vf = result as SendMessageResult.ValidationFailed
        assertTrue(vf.reason is MessageValidator.ValidationResult.TextTooLong)
    }

    @Test
    fun `text message returns NotAuthenticated when auth user is null`() = runTest {
        coEvery { authRepository.getCurrentUser() } returns null

        val result = useCase("Hello")

        assertTrue(result is SendMessageResult.NotAuthenticated)
        coVerify(exactly = 0) { messageRepository.saveMessageLocally(any()) }
    }

    @Test
    fun `text message returns NotOnboarded when profile is null`() = runTest {
        coEvery { userRepository.getProfile(any()) } returns Result.success(null)

        val result = useCase("Hello")

        assertTrue(result is SendMessageResult.NotOnboarded)
        coVerify(exactly = 0) { messageRepository.saveMessageLocally(any()) }
    }

    @Test
    fun `text message returns NotOnboarded when getProfile fails`() = runTest {
        coEvery { userRepository.getProfile(any()) } returns Result.failure(RuntimeException("error"))

        val result = useCase("Hello")

        assertTrue(result is SendMessageResult.NotOnboarded)
    }

    @Test
    fun `text message returns StorageError when saveMessageLocally fails`() = runTest {
        coEvery { messageRepository.saveMessageLocally(any()) } returns
                Result.failure(RuntimeException("disk full"))

        val result = useCase("Hello")

        assertTrue(result is SendMessageResult.StorageError)
    }

    @Test
    fun `text message calls saveMessageLocally exactly once on success`() = runTest {
        useCase("Hello")
        coVerify(exactly = 1) { messageRepository.saveMessageLocally(any()) }
    }

    @Test
    fun `text message does not call auth or repo when validation fails`() = runTest {
        useCase("")
        coVerify(exactly = 0) { authRepository.getCurrentUser() }
        coVerify(exactly = 0) { messageRepository.saveMessageLocally(any()) }
    }

    @Test
    fun `text message entity has null firebaseKey (not yet sent)`() = runTest {
        val result = useCase("Hello") as SendMessageResult.Success
        assertTrue(result.message.firebaseKey == null)
    }

    @Test
    fun `text message entity localId is a non-blank UUID`() = runTest {
        val result = useCase("Hello") as SendMessageResult.Success
        assertTrue(result.message.localId.isNotBlank())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // sendMedia
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `sendMedia returns Success for valid image jpeg`() = runTest {
        val result = useCase.sendMedia("image/jpeg", 1024L)
        assertTrue(result is SendMessageResult.Success)
        val msg = (result as SendMessageResult.Success).message
        assertEquals(MessageType.IMAGE, msg.type)
        assertEquals("image/jpeg", msg.mediaType)
    }

    @Test
    fun `sendMedia returns Success for valid video mp4`() = runTest {
        val result = useCase.sendMedia("video/mp4", 2048L)
        assertTrue(result is SendMessageResult.Success)
        assertEquals(MessageType.VIDEO, (result as SendMessageResult.Success).message.type)
    }

    @Test
    fun `sendMedia returns ValidationFailed for empty file`() = runTest {
        val result = useCase.sendMedia("image/jpeg", 0L)
        assertTrue(result is SendMessageResult.ValidationFailed)
        val vf = result as SendMessageResult.ValidationFailed
        assertTrue(vf.reason is MessageValidator.ValidationResult.EmptyMediaFile)
    }

    @Test
    fun `sendMedia returns ValidationFailed for file over size limit`() = runTest {
        val result = useCase.sendMedia("image/jpeg", MessageValidator.MAX_MEDIA_BYTES + 1)
        assertTrue(result is SendMessageResult.ValidationFailed)
        val vf = result as SendMessageResult.ValidationFailed
        assertTrue(vf.reason is MessageValidator.ValidationResult.MediaTooLarge)
    }

    @Test
    fun `sendMedia returns ValidationFailed for unsupported MIME type`() = runTest {
        val result = useCase.sendMedia("application/pdf", 1024L)
        assertTrue(result is SendMessageResult.ValidationFailed)
        val vf = result as SendMessageResult.ValidationFailed
        assertTrue(vf.reason is MessageValidator.ValidationResult.UnsupportedMediaType)
    }

    @Test
    fun `sendMedia returns NotAuthenticated when auth user is null`() = runTest {
        coEvery { authRepository.getCurrentUser() } returns null
        val result = useCase.sendMedia("image/jpeg", 1024L)
        assertTrue(result is SendMessageResult.NotAuthenticated)
    }

    @Test
    fun `sendMedia entity has null mediaUrl immediately (populated after upload)`() = runTest {
        val result = useCase.sendMedia("image/jpeg", 1024L) as SendMessageResult.Success
        assertTrue(result.message.mediaUrl == null)
    }

    @Test
    fun `sendMedia entity has correct mimeType`() = runTest {
        val capturedMsg = slot<MessageEntity>()
        coEvery { messageRepository.saveMessageLocally(capture(capturedMsg)) } returns Result.success(
            Unit
        )

        useCase.sendMedia("image/png", 2048L)

        assertEquals("image/png", capturedMsg.captured.mediaType)
    }

    // ── isSuccess ─────────────────────────────────────────────────────────────

    @Test
    fun `isSuccess is true only for Success`() = runTest {
        val success = useCase("Hello") as SendMessageResult.Success
        assertTrue(success.isSuccess)
    }

    @Test
    fun `isSuccess is false for ValidationFailed`() = runTest {
        val result = useCase("")
        assertFalse(result.isSuccess)
    }

    private fun assertFalse(condition: Boolean) = assertTrue(!condition)
}
