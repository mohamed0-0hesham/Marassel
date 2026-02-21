package com.hesham0_0.marassel.ui.chat

import android.net.Uri
import com.hesham0_0.marassel.core.mvi.UiEffect
import com.hesham0_0.marassel.core.mvi.UiEvent
import com.hesham0_0.marassel.core.mvi.UiState
import com.hesham0_0.marassel.domain.model.MessageStatus
import com.hesham0_0.marassel.domain.model.MessageType
import com.hesham0_0.marassel.domain.model.UserEntity
import com.hesham0_0.marassel.domain.usecase.message.MessageUiItem

data class ChatUiState(

    val messages: List<MessageUiModel> = emptyList(),

    val isLoadingInitial: Boolean = true,

    val isLoadingOlder: Boolean = false,

    val hasMoreMessages: Boolean = true,

    val currentUser: UserEntity? = null,

    val inputText: String = "",

    /** Media files selected but not yet sent — shown as thumbnails above input */
    val selectedMediaUris: List<Uri> = emptyList(),

    /** True while the media picker bottom sheet is open */
    val showMediaPicker: Boolean = false,

    // ── Typing indicators (Bonus CHAT-053) ────────────────────────────────────

    /** deviceId → display name for users currently typing (excludes self) */
    val typingUsers: Map<String, String> = emptyMap(),

    // ── Transient UI state ────────────────────────────────────────────────────

    /** Non-null when a snackbar error message should be shown */
    val error: String? = null,

    /** localId of the message currently being long-pressed (for context menu) */
    val selectedMessageLocalId: String? = null,

    ) : UiState {

    // ── Derived properties ────────────────────────────────────────────────────

    /**
     * Send button is enabled when there is text OR at least one media item.
     * Matches task spec: "isSendEnabled: inputText.isNotBlank() || selectedMediaUris.isNotEmpty()"
     */
    val isSendEnabled: Boolean
        get() = inputText.isNotBlank() || selectedMediaUris.isNotEmpty()

    /** True if there are typing users to display */
    val hasTypingUsers: Boolean
        get() = typingUsers.isNotEmpty()

    /**
     * Human-readable typing label.
     * "Alice is typing…" / "Alice and Bob are typing…" / "3 people are typing…"
     */
    val typingLabel: String
        get() {
            val names = typingUsers.values.toList()
            return when (names.size) {
                0    -> ""
                1    -> "${names[0]} is typing…"
                2    -> "${names[0]} and ${names[1]} are typing…"
                else -> "${names.size} people are typing…"
            }
        }

    /** True when the message list is empty and not loading */
    val isEmpty: Boolean
        get() = messages.isEmpty() && !isLoadingInitial

    /** The selected message entity for the context menu, or null */
    val selectedMessage: MessageUiModel?
        get() = selectedMessageLocalId?.let { id ->
            messages.find { it.localId == id }
        }
}

// ── MessageUiModel ────────────────────────────────────────────────────────────

/**
 * Presentation-layer wrapper around a message, enriched with UI metadata.
 *
 * Wraps [MessageUiItem] from the domain layer and adds:
 * - [isFromCurrentUser] for bubble alignment
 * - [formattedTime] for display
 * - [uploadProgress] for in-bubble progress overlay
 */
data class MessageUiModel(
    val localId: String,
    val firebaseKey: String?,
    val senderUid: String,
    val senderName: String,
    val senderInitials: String,
    val text: String?,
    val mediaUrl: String?,
    val mediaType: String?,
    val timestamp: Long,
    val formattedTime: String,
    val status: MessageStatus,
    val type: MessageType,
    val replyToId: String?,

    // UI metadata from ObserveMessagesUseCase
    val isFromCurrentUser: Boolean,
    val showSenderInfo: Boolean,
    val isLastInBurst: Boolean,
    val showDayDivider: Boolean,
    val dayDividerLabel: String?,

    // WorkManager upload progress (null when not uploading)
    val uploadProgress: Int? = null,
) {
    val isPending: Boolean get() = status == MessageStatus.PENDING
    val isFailed: Boolean  get() = status == MessageStatus.FAILED
    val isSent: Boolean    get() = status == MessageStatus.SENT
    val isMedia: Boolean   get() = type == MessageType.IMAGE || type == MessageType.VIDEO
}

// ── Events ────────────────────────────────────────────────────────────────────

sealed interface ChatUiEvent : UiEvent {

    // Input bar
    data class MessageInputChanged(val text: String)        : ChatUiEvent
    data object SendTextClicked                             : ChatUiEvent
    data class SendMediaClicked(val uris: List<Uri>)        : ChatUiEvent
    data object AttachmentClicked                           : ChatUiEvent
    data object MediaPickerDismissed                        : ChatUiEvent
    data class MediaSelected(val uris: List<Uri>)           : ChatUiEvent
    data class RemoveSelectedMedia(val uri: Uri)            : ChatUiEvent
    data object ClearSelectedMedia                          : ChatUiEvent

    // Message actions
    data class RetryMessageClicked(val localId: String)     : ChatUiEvent
    data class DeleteMessageClicked(
        val localId: String,
        val firebaseKey: String?,
        val senderUid: String,
        val type: MessageType
    )                                                       : ChatUiEvent
    data class MessageLongPressed(val localId: String)      : ChatUiEvent
    data object DismissMessageContextMenu                   : ChatUiEvent

    // Pagination
    data object LoadOlderMessages                           : ChatUiEvent

    // Initial Load
    data object InitialScrollCompleted                      : ChatUiEvent

    // Misc
    data object DismissError                                : ChatUiEvent
}

// ── Effects ───────────────────────────────────────────────────────────────────

sealed interface ChatUiEffect : UiEffect {
    /** Scroll the list to the bottom (newest message) */
    data object ScrollToBottom                              : ChatUiEffect
    /** Navigate to full-screen media viewer */
    data class NavigateToMediaViewer(val mediaUrl: String)  : ChatUiEffect
    /** Show a transient snackbar */
    data class ShowSnackbar(val message: String)            : ChatUiEffect
    /** Open the media picker */
    data object OpenMediaPicker                             : ChatUiEffect
}