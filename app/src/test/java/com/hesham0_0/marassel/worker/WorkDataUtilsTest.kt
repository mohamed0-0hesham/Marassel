package com.hesham0_0.marassel.worker

import androidx.work.workDataOf
import com.hesham0_0.marassel.domain.model.MessageEntity
import com.hesham0_0.marassel.domain.model.MessageStatus
import com.hesham0_0.marassel.domain.model.MessageType
import com.hesham0_0.marassel.worker.WorkDataUtils.buildSendMessageInputData
import com.hesham0_0.marassel.worker.WorkDataUtils.buildUploadMediaInputData
import com.hesham0_0.marassel.worker.WorkDataUtils.buildUploadSuccessOutput
import com.hesham0_0.marassel.worker.WorkDataUtils.extractMediaUrls
import com.hesham0_0.marassel.worker.WorkDataUtils.toMessageEntity
import com.hesham0_0.marassel.worker.WorkDataUtils.toSendMessageParams
import com.hesham0_0.marassel.worker.WorkDataUtils.toUploadMediaParams
import android.net.Uri
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WorkDataUtilsTest {

    private val textEntity = MessageEntity(
        localId = "local-abc",
        firebaseKey = null,
        senderUid = "uid-1",
        senderName = "Alice",
        text = "Hello, World!",
        mediaUrl = null,
        mediaType = null,
        timestamp = 123_456_789L,
        status = MessageStatus.PENDING,
        type = MessageType.TEXT,
    )

    private val imageEntity = textEntity.copy(
        localId = "local-img",
        text = null,
        mediaUrl = "https://cdn.example.com/img.jpg",
        mediaType = "image/jpeg",
        type = MessageType.IMAGE,
    )

    @Test
    fun `buildSendMessageInputData writes KEY_LOCAL_ID`() {
        val data = buildSendMessageInputData(textEntity)
        assertEquals("local-abc", data.getString(WorkerKeys.KEY_LOCAL_ID))
    }

    @Test
    fun `buildSendMessageInputData writes KEY_SENDER_UID`() {
        val data = buildSendMessageInputData(textEntity)
        assertEquals("uid-1", data.getString(WorkerKeys.KEY_SENDER_UID))
    }

    @Test
    fun `buildSendMessageInputData writes KEY_SENDER_NAME`() {
        val data = buildSendMessageInputData(textEntity)
        assertEquals("Alice", data.getString(WorkerKeys.KEY_SENDER_NAME))
    }

    @Test
    fun `buildSendMessageInputData writes KEY_CONTENT (not KEY_MESSAGE_TEXT)`() {
        val data = buildSendMessageInputData(textEntity)
        assertEquals("Hello, World!", data.getString(WorkerKeys.KEY_CONTENT))
    }

    @Test
    fun `buildSendMessageInputData writes KEY_TIMESTAMP`() {
        val data = buildSendMessageInputData(textEntity)
        assertEquals(123_456_789L, data.getLong(WorkerKeys.KEY_TIMESTAMP, -1L))
    }

    @Test
    fun `buildSendMessageInputData writes KEY_MESSAGE_TYPE`() {
        val data = buildSendMessageInputData(textEntity)
        assertEquals("TEXT", data.getString(WorkerKeys.KEY_MESSAGE_TYPE))
    }

    @Test
    fun `buildSendMessageInputData writes KEY_MEDIA_TYPE as empty string when null`() {
        val data = buildSendMessageInputData(textEntity)
        val mediaType = data.getString(WorkerKeys.KEY_MEDIA_TYPE) ?: ""
        assertEquals("", mediaType)
    }

    @Test
    fun `buildSendMessageInputData writes KEY_MEDIA_TYPE for image entity`() {
        val data = buildSendMessageInputData(imageEntity)
        assertEquals("image/jpeg", data.getString(WorkerKeys.KEY_MEDIA_TYPE))
    }

    @Test
    fun `buildSendMessageInputData includes mediaUrl in KEY_MEDIA_URLS JSON`() {
        val data = buildSendMessageInputData(imageEntity)
        val urls = data.extractMediaUrls()
        assertTrue(urls.contains("https://cdn.example.com/img.jpg"))
    }

    @Test
    fun `buildSendMessageInputData KEY_MEDIA_URLS is empty list for text entity`() {
        val data = buildSendMessageInputData(textEntity)
        val urls = data.extractMediaUrls()
        assertTrue(urls.isEmpty())
    }

    @Test
    fun `toSendMessageParams full round-trip for TEXT entity`() {
        val params = buildSendMessageInputData(textEntity).toSendMessageParams()!!

        assertEquals("local-abc", params.localId)
        assertEquals("uid-1", params.senderUid)
        assertEquals("Alice", params.senderName)
        assertEquals("Hello, World!", params.content)
        assertEquals(123_456_789L, params.timestamp)
        assertEquals(MessageType.TEXT, params.messageType)
        assertTrue(params.mediaUrls.isEmpty())
    }

    @Test
    fun `toSendMessageParams full round-trip for IMAGE entity`() {
        val params = buildSendMessageInputData(imageEntity).toSendMessageParams()!!

        assertEquals("local-img", params.localId)
        assertEquals("image/jpeg", params.mediaType)
        assertEquals(MessageType.IMAGE, params.messageType)
        assertTrue(params.mediaUrls.contains("https://cdn.example.com/img.jpg"))
    }

    @Test
    fun `toSendMessageParams returns null when KEY_LOCAL_ID is missing`() {
        val data = workDataOf(
            WorkerKeys.KEY_SENDER_UID to "uid-1",
            WorkerKeys.KEY_SENDER_NAME to "Alice",
            WorkerKeys.KEY_CONTENT to "hello",
        )
        assertNull(data.toSendMessageParams())
    }

    @Test
    fun `toSendMessageParams returns null when KEY_LOCAL_ID is blank`() {
        val data = workDataOf(
            WorkerKeys.KEY_LOCAL_ID to "",
            WorkerKeys.KEY_SENDER_UID to "uid-1",
            WorkerKeys.KEY_SENDER_NAME to "Alice",
        )
        assertNull(data.toSendMessageParams())
    }

    @Test
    fun `toSendMessageParams returns null when KEY_SENDER_UID is missing`() {
        val data = workDataOf(
            WorkerKeys.KEY_LOCAL_ID to "local-1",
            WorkerKeys.KEY_SENDER_NAME to "Alice",
        )
        assertNull(data.toSendMessageParams())
    }

    @Test
    fun `toSendMessageParams returns null when KEY_SENDER_NAME is missing`() {
        val data = workDataOf(
            WorkerKeys.KEY_LOCAL_ID to "local-1",
            WorkerKeys.KEY_SENDER_UID to "uid-1",
        )
        assertNull(data.toSendMessageParams())
    }

    @Test
    fun `toSendMessageParams defaults timestamp to 0L when missing`() {
        val data = workDataOf(
            WorkerKeys.KEY_LOCAL_ID to "local-1",
            WorkerKeys.KEY_SENDER_UID to "uid-1",
            WorkerKeys.KEY_SENDER_NAME to "Alice",
            WorkerKeys.KEY_CONTENT to "Hello",
        )
        val params = data.toSendMessageParams()!!
        assertEquals(0L, params.timestamp)
    }

    @Test
    fun `toSendMessageParams defaults messageType to TEXT for unknown type string`() {
        val data = workDataOf(
            WorkerKeys.KEY_LOCAL_ID to "local-1",
            WorkerKeys.KEY_SENDER_UID to "uid-1",
            WorkerKeys.KEY_SENDER_NAME to "Alice",
            WorkerKeys.KEY_MESSAGE_TYPE to "UNKNOWN_TYPE",
        )
        val params = data.toSendMessageParams()!!
        assertEquals(MessageType.TEXT, params.messageType)
    }

    @Test
    fun `toMessageEntity maps all fields from SendMessageParams`() {
        val params = buildSendMessageInputData(textEntity).toSendMessageParams()!!
        val entity = params.toMessageEntity()

        assertEquals("local-abc", entity.localId)
        assertEquals("uid-1", entity.senderUid)
        assertEquals("Alice", entity.senderName)
        assertEquals("Hello, World!", entity.text)
        assertEquals(MessageStatus.PENDING, entity.status)
        assertEquals(MessageType.TEXT, entity.type)
        assertNull(entity.firebaseKey)
    }

    @Test
    fun `toMessageEntity sets mediaUrl to first URL from mediaUrls list`() {
        val params = buildSendMessageInputData(imageEntity).toSendMessageParams()!!
        val entity = params.toMessageEntity()

        assertEquals("https://cdn.example.com/img.jpg", entity.mediaUrl)
    }

    @Test
    fun `toMessageEntity sets text to null when content is blank`() {
        val blankContentEntity = textEntity.copy(text = null)
        val params = buildSendMessageInputData(blankContentEntity).toSendMessageParams()!!
        val entity = params.toMessageEntity()

        assertNull(entity.text)
    }

    @Test
    fun `buildUploadMediaInputData toUploadMediaParams round-trip`() {
        val uri = Uri.parse("content://media/external/images/1234")
        val data = buildUploadMediaInputData("local-upload", uri, "image/png")
        val params = data.toUploadMediaParams()!!

        assertEquals("local-upload", params.localId)
        assertEquals(uri.toString(), params.mediaUri.toString())
        assertEquals("image/png", params.mimeType)
    }

    @Test
    fun `toUploadMediaParams returns null when KEY_LOCAL_ID is blank`() {
        val data = workDataOf(
            WorkerKeys.KEY_LOCAL_ID to "",
            WorkerKeys.KEY_MEDIA_URI to "content://media/1",
        )
        assertNull(data.toUploadMediaParams())
    }

    @Test
    fun `toUploadMediaParams returns null when KEY_MEDIA_URI is blank`() {
        val data = workDataOf(
            WorkerKeys.KEY_LOCAL_ID to "local-1",
            WorkerKeys.KEY_MEDIA_URI to "",
        )
        assertNull(data.toUploadMediaParams())
    }

    @Test
    fun `extractMediaUrls returns empty list when KEY_MEDIA_URLS is missing`() {
        val data = workDataOf(WorkerKeys.KEY_LOCAL_ID to "local-1")
        assertTrue(data.extractMediaUrls().isEmpty())
    }

    @Test
    fun `extractMediaUrls returns empty list for blank JSON`() {
        val data = workDataOf(WorkerKeys.KEY_MEDIA_URLS to "")
        assertTrue(data.extractMediaUrls().isEmpty())
    }

    @Test
    fun `extractMediaUrls returns empty list for malformed JSON`() {
        val data = workDataOf(WorkerKeys.KEY_MEDIA_URLS to "{not-a-list}")
        assertTrue(data.extractMediaUrls().isEmpty())
    }

    @Test
    fun `extractMediaUrls returns list of URLs from valid JSON`() {
        val urls = listOf("https://example.com/a.jpg", "https://example.com/b.jpg")
        val json = Json.encodeToString(urls)
        val data = workDataOf(WorkerKeys.KEY_MEDIA_URLS to json)
        assertEquals(urls, data.extractMediaUrls())
    }

    @Test
    fun `buildUploadSuccessOutput contains localId`() {
        val output = buildUploadSuccessOutput("local-1", "https://cdn.example.com/file.jpg")
        assertEquals("local-1", output.getString(WorkerKeys.KEY_LOCAL_ID))
    }

    @Test
    fun `buildUploadSuccessOutput contains downloadUrl in KEY_MEDIA_URLS`() {
        val url = "https://cdn.example.com/file.jpg"
        val output = buildUploadSuccessOutput("local-1", url)
        val urls = output.extractMediaUrls()
        assertTrue(urls.contains(url))
    }
}