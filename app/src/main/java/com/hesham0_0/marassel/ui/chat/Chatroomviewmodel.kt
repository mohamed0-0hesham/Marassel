package com.hesham0_0.marassel.ui.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.viewModelScope
import com.hesham0_0.marassel.core.mvi.BaseViewModel
import com.hesham0_0.marassel.domain.model.MessageType
import com.hesham0_0.marassel.domain.model.UserEntity
import com.hesham0_0.marassel.domain.repository.AuthRepository
import com.hesham0_0.marassel.domain.usecase.message.DeleteMessageUseCase
import com.hesham0_0.marassel.domain.usecase.message.DeleteResult
import com.hesham0_0.marassel.domain.usecase.message.LoadOlderMessagesUseCase
import com.hesham0_0.marassel.domain.usecase.message.MessageUiItem
import com.hesham0_0.marassel.domain.usecase.message.ObserveMessagesUseCase
import com.hesham0_0.marassel.domain.usecase.message.ObserveTypingUsersUseCase
import com.hesham0_0.marassel.domain.usecase.message.RetryMessageResult
import com.hesham0_0.marassel.domain.usecase.message.RetryMessageUseCase
import com.hesham0_0.marassel.domain.usecase.message.SendMessageResult
import com.hesham0_0.marassel.domain.usecase.message.SendMessageUseCase
import com.hesham0_0.marassel.domain.usecase.message.SetTypingStatusUseCase
import com.hesham0_0.marassel.domain.usecase.user.GetCurrentUserUseCase
import com.hesham0_0.marassel.worker.MessageSendOrchestrator
import com.hesham0_0.marassel.worker.MessageStatusUpdate
import com.hesham0_0.marassel.worker.WorkInfoMessageBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import androidx.core.net.toUri

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val loadOlderMessagesUseCase: LoadOlderMessagesUseCase,
    private val retryMessageUseCase: RetryMessageUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeTypingUsersUseCase: ObserveTypingUsersUseCase,
    private val setTypingStatusUseCase: SetTypingStatusUseCase,
    private val orchestrator: MessageSendOrchestrator,
    private val workInfoBridge: WorkInfoMessageBridge,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : BaseViewModel<ChatUiState, ChatUiEvent, ChatUiEffect>(ChatUiState()) {

    private val activeWorkJobs = mutableMapOf<String, Job>()
    private val uploadProgressMap = mutableMapOf<String, Int?>()
    private var oldestTimestamp: Long = Long.MAX_VALUE
    
    private var typingJob: Job? = null

    init {
        observeCurrentUser()
        observeMessages()
        observeTyping()
    }

    private fun observeCurrentUser() {
        getCurrentUserUseCase()
            .onEach { user -> setState { copy(currentUser = user) } }
            .launchIn(viewModelScope)
    }

    private fun observeMessages() {
        observeMessagesUseCase()
            .onEach { items ->
                val user = currentState.currentUser
                val models = items.map { item -> item.toUiModel(user) }

                models.minOfOrNull { it.timestamp }?.let { ts ->
                    if (ts < oldestTimestamp) oldestTimestamp = ts
                }

                val oldMessages = currentState.messages

                setState {
                    val combined = if (models.isEmpty()) {
                        emptyList()
                    } else {
                        val incomingOldest = models.minOf { it.timestamp }
                        val paginated = messages.filter { it.timestamp < incomingOldest }
                        (paginated + models).distinctBy { it.localId }.sortedBy { it.timestamp }
                    }

                    val finalized = recalculateUiFlags(combined)

                    copy(
                        messages = mergeWithProgress(finalized),
                    )
                }

                val isInitialLoad = currentState.isLoadingInitial
                val newLastId = models.lastOrNull()?.localId
                val oldLastId = oldMessages.lastOrNull()?.localId

                if (isInitialLoad || (newLastId != null && newLastId != oldLastId && models.size >= oldMessages.size)) {
                    setEffect(ChatUiEffect.ScrollToBottom)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeTyping() {
        observeTypingUsersUseCase()
            .onEach { users -> setState { copy(typingUsers = users) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ChatUiEvent) {
        when (event) {
            ChatUiEvent.InitialScrollCompleted -> {
                setState { copy(isLoadingInitial = false) }
            }

            is ChatUiEvent.MessageInputChanged -> {
                setState {
                    copy(
                        inputText = event.text,
                        error = null
                    )
                }
                handleTypingState(event.text)
            }

            is ChatUiEvent.MediaSelected -> setState {
                copy(
                    selectedMediaUris = selectedMediaUris + event.uris,
                    showMediaPicker = false
                )
            }

            is ChatUiEvent.RemoveSelectedMedia -> setState { copy(selectedMediaUris = selectedMediaUris - event.uri) }
            ChatUiEvent.ClearSelectedMedia -> setState { copy(selectedMediaUris = emptyList()) }
            ChatUiEvent.AttachmentClicked -> setState { copy(showMediaPicker = true) }
            ChatUiEvent.MediaPickerDismissed -> setState { copy(showMediaPicker = false) }

            ChatUiEvent.SendTextClicked -> onSendText()
            is ChatUiEvent.SendMediaClicked -> onSendMedia(event.uris)

            is ChatUiEvent.RetryMessageClicked -> onRetryMessage(event.localId)
            is ChatUiEvent.DeleteMessageClicked -> onDeleteMessage(
                event.localId,
                event.firebaseKey,
                event.senderUid,
                event.type
            )

            is ChatUiEvent.MessageLongPressed -> setState { copy(selectedMessageLocalId = event.localId) }
            ChatUiEvent.DismissMessageContextMenu -> setState { copy(selectedMessageLocalId = null) }

            ChatUiEvent.LoadOlderMessages -> onLoadOlderMessages()

            ChatUiEvent.DismissError -> setState { copy(error = null) }

            ChatUiEvent.LogoutClicked -> onLogout()
        }
    }

    private fun onLogout() {
        viewModelScope.launch {
            authRepository.signOut()
            setEffect(
                ChatUiEvent.NavigateToAuth
            )
        }
    }

    private fun handleTypingState(text: String) {
        typingJob?.cancel()

        if (text.isNotEmpty()) {
            launch { setTypingStatusUseCase(isTyping = true) }

            typingJob = viewModelScope.launch {
                delay(3000)
                setTypingStatusUseCase(isTyping = false)
            }
        } else {
            launch { setTypingStatusUseCase(isTyping = false) }
        }
    }

    private fun onSendText() {
        val text = currentState.inputText.trim()
        if (text.isBlank()) return

        setState { copy(inputText = "") }

        typingJob?.cancel()
        launch { setTypingStatusUseCase(isTyping = false) }

        launch {
            when (val result = sendMessageUseCase(text)) {
                is SendMessageResult.Success -> {
                    val workRequest = orchestrator.enqueueTextMessage(result.message)
                    observeWorkStatus(
                        localId = result.message.localId,
                        workRequestId = workRequest.id,
                    )
                }

                is SendMessageResult.ValidationFailed -> {
                    setState { copy(inputText = text) }
                    setEffect(
                        ChatUiEffect.ShowSnackbar(
                            com.hesham0_0.marassel.domain.usecase.message.MessageValidator
                                .toErrorMessage(result.reason) ?: "Invalid message"
                        )
                    )
                }

                is SendMessageResult.NotAuthenticated,
                is SendMessageResult.NotOnboarded -> {
                    setState { copy(inputText = text) }
                    setEffect(ChatUiEffect.ShowSnackbar("Please sign in to send messages"))
                }

                is SendMessageResult.StorageError -> {
                    setState { copy(inputText = text) }
                    setEffect(ChatUiEffect.ShowSnackbar("Failed to queue message. Please try again."))
                }
            }
        }
    }

    private fun onSendMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return

        setState { copy(selectedMediaUris = emptyList()) }

        launch {
            uris.forEach { uri ->
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                var size = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        size = cursor.getLong(sizeIndex)
                    }
                }

                if (size == 0L) size = 1L

                when (val result = sendMessageUseCase.sendMedia(
                    mimeType = mimeType,
                    fileSizeBytes = size,
                )) {
                    is SendMessageResult.Success -> {
                        val (uploadRequest, sendRequest) = orchestrator.enqueueMediaMessage(
                            message = result.message,
                            mediaUri = uri,
                            mimeType = mimeType,
                        )
                        observeUploadProgress(
                            localId = result.message.localId,
                            workRequestId = uploadRequest.id,
                        )
                        observeWorkStatus(
                            localId = result.message.localId,
                            workRequestId = sendRequest.id,
                        )
                    }

                    is SendMessageResult.ValidationFailed -> {
                        setEffect(
                            ChatUiEffect.ShowSnackbar(
                                com.hesham0_0.marassel.domain.usecase.message.MessageValidator
                                    .toErrorMessage(result.reason) ?: "Invalid media"
                            )
                        )
                    }

                    else -> setEffect(ChatUiEffect.ShowSnackbar("Failed to queue media. Please try again."))
                }
            }
        }
    }

    private fun onRetryMessage(localId: String) {
        launch {
            when (val result = retryMessageUseCase(localId)) {
                is RetryMessageResult.Success -> {
                    val message = result.message

                    val mediaUri = message.mediaUrl?.toUri()
                    val requests = orchestrator.retryMessage(
                        message = message,
                        mediaUri = mediaUri,
                        mimeType = message.mediaType
                    )

                    if (requests.size == 2) {
                        observeUploadProgress(localId, requests[0].id)
                        observeWorkStatus(localId, requests[1].id)
                    } else if (requests.isNotEmpty()) {
                        observeWorkStatus(localId, requests[0].id)
                    }
                }

                is RetryMessageResult.MessageNotFound -> {
                    setEffect(ChatUiEffect.ShowSnackbar("Message not found"))
                }

                is RetryMessageResult.MessageNotFailed -> {
                    // Already retrying or sent — no action needed
                }

                is RetryMessageResult.StorageError -> {
                    setEffect(ChatUiEffect.ShowSnackbar("Failed to retry. Please try again."))
                }
            }
        }
    }

    private fun onDeleteMessage(localId: String, firebaseKey: String?, senderUid: String, type: MessageType) {
        setState { copy(selectedMessageLocalId = null) }
        launch {
            val result = deleteMessageUseCase(
                localId = localId,
                firebaseKey = firebaseKey,
                senderUid = senderUid,
                type = type
            )
            when (result) {
                is DeleteResult.Success -> {
                    setState {
                        copy(messages = messages.filter { it.localId != localId })
                    }
                }

                is DeleteResult.NotAuthenticated -> setEffect(ChatUiEffect.ShowSnackbar("Not signed in"))
                is DeleteResult.NotOwner -> setEffect(ChatUiEffect.ShowSnackbar("You can only delete your own messages"))
                is DeleteResult.UnconfirmedMessage -> setEffect(ChatUiEffect.ShowSnackbar("Can't delete a message that hasn't been sent yet"))
                is DeleteResult.StorageError -> setEffect(ChatUiEffect.ShowSnackbar("Failed to delete. Please try again."))
            }
        }
    }

    private fun onLoadOlderMessages() {
        if (currentState.isLoadingOlder || !currentState.hasMoreMessages) return

        setState { copy(isLoadingOlder = true) }

        launch {
            when (val result = loadOlderMessagesUseCase(
                beforeTimestamp = oldestTimestamp,
                limit = LoadOlderMessagesUseCase.DEFAULT_PAGE_SIZE,
            )) {
                is com.hesham0_0.marassel.domain.usecase.message.LoadOlderResult.Success -> {
                    val data = result.data
                    val user = currentState.currentUser
                    val models = data.messages.map { entity ->
                        MessageUiItem(
                            message = entity,
                            meta = com.hesham0_0.marassel.domain.usecase.message.MessageUiMeta(
                                isOwnMessage = entity.senderUid == user?.uid,
                                showSenderInfo = true,
                                isLastInBurst = true,
                                showTimestamp = true,
                            ),
                        ).toUiModel(user)
                    }

                    models.minOfOrNull { it.timestamp }?.let { ts ->
                        if (ts < oldestTimestamp) oldestTimestamp = ts
                    }

                    setState {
                        val combined = (models + messages)
                            .distinctBy { it.localId }
                            .sortedBy { it.timestamp }

                        val finalized = recalculateUiFlags(combined)

                        copy(
                            messages = mergeWithProgress(finalized),
                            isLoadingOlder = false,
                            hasMoreMessages = !data.hasReachedEnd,
                        )
                    }
                }

                is com.hesham0_0.marassel.domain.usecase.message.LoadOlderResult.Error -> {
                    setState { copy(isLoadingOlder = false) }
                    setEffect(ChatUiEffect.ShowSnackbar("Failed to load older messages"))
                }
            }
        }
    }

    private fun observeWorkStatus(localId: String, workRequestId: UUID) {
        activeWorkJobs[localId]?.cancel()

        val job = workInfoBridge
            .observeMessageStatus(localId, workRequestId)
            .onEach { update: MessageStatusUpdate ->
                updateMessageProgress(localId, progress = null)

                if (update.isTerminal) {
                    activeWorkJobs.remove(localId)?.cancel()
                }
            }
            .launchIn(viewModelScope)

        activeWorkJobs[localId] = job
    }

    private fun observeUploadProgress(localId: String, workRequestId: UUID) {
        workInfoBridge
            .observeUploadProgress(workRequestId)
            .onEach { progress -> updateMessageProgress(localId, progress) }
            .launchIn(viewModelScope)
    }

    private fun updateMessageProgress(localId: String, progress: Int?) {
        uploadProgressMap[localId] = progress
        setState { copy(messages = mergeWithProgress(messages)) }
    }

    private fun mergeWithProgress(models: List<MessageUiModel>): List<MessageUiModel> {
        if (uploadProgressMap.isEmpty()) return models
        return models.map { model ->
            val progress = uploadProgressMap[model.localId]
            if (progress != model.uploadProgress) model.copy(uploadProgress = progress)
            else model
        }
    }

    private fun recalculateUiFlags(models: List<MessageUiModel>): List<MessageUiModel> {
        if (models.isEmpty()) return emptyList()

        return models.mapIndexed { index, model ->
            val prev = models.getOrNull(index - 1)
            val next = models.getOrNull(index + 1)

            val showTimestamp = prev == null || !isSameDay(prev.timestamp, model.timestamp)
            val showSenderInfo = prev == null || prev.senderUid != model.senderUid || showTimestamp
            val isLastInBurst = next == null || next.senderUid != model.senderUid || !isSameDay(model.timestamp, next.timestamp)

            model.copy(
                showSenderInfo = !model.isFromCurrentUser && showSenderInfo,
                isLastInBurst = isLastInBurst,
                showDayDivider = showTimestamp,
                dayDividerLabel = if (showTimestamp) formatDay(model.timestamp) else null
            )
        }
    }

    private fun isSameDay(timestampA: Long, timestampB: Long): Boolean {
        val calA = java.util.Calendar.getInstance().apply { timeInMillis = timestampA }
        val calB = java.util.Calendar.getInstance().apply { timeInMillis = timestampB }
        return calA.get(java.util.Calendar.YEAR) == calB.get(java.util.Calendar.YEAR)
                && calA.get(java.util.Calendar.DAY_OF_YEAR) == calB.get(java.util.Calendar.DAY_OF_YEAR)
    }

    override fun onCleared() {
        super.onCleared()
        typingJob?.cancel()
        launch { setTypingStatusUseCase(isTyping = false) }
        activeWorkJobs.values.forEach { it.cancel() }
        activeWorkJobs.clear()
    }
}

private val timeFormatterThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat("h:mm a", Locale.getDefault())
}

private val dayFormatterThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
}

private fun formatTime(timestamp: Long): String {
    return timeFormatterThreadLocal.get()?.format(Date(timestamp)) ?: ""
}

private fun formatDay(timestamp: Long): String {
    return dayFormatterThreadLocal.get()?.format(Date(timestamp)) ?: ""
}

private fun MessageUiItem.toUiModel(currentUser: UserEntity?): MessageUiModel {
    val entity = message
    return MessageUiModel(
        localId = entity.localId,
        firebaseKey = entity.firebaseKey,
        senderUid = entity.senderUid,
        senderName = entity.senderName,
        senderInitials = UserEntity.computeInitials(entity.senderName),
        text = entity.text,
        mediaUrl = entity.mediaUrl,
        mediaType = entity.mediaType,
        timestamp = entity.timestamp,
        formattedTime = formatTime(entity.timestamp),
        status = entity.status,
        type = entity.type,
        replyToId = entity.replyToId,
        isFromCurrentUser = meta.isOwnMessage,
        showSenderInfo = meta.showSenderInfo,
        isLastInBurst = meta.isLastInBurst,
        showDayDivider = meta.showTimestamp,
        dayDividerLabel = if (meta.showTimestamp) formatDay(entity.timestamp) else null,
        uploadProgress = null,
    )
}