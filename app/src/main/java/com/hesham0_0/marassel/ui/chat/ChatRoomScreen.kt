package com.hesham0_0.marassel.ui.chat

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hesham0_0.marassel.ui.chat.components.ChatInputBar
import com.hesham0_0.marassel.ui.chat.components.MessageBubble
import com.hesham0_0.marassel.ui.chat.components.MessageContextMenu
import com.hesham0_0.marassel.ui.media.MediaPickerBottomSheet
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ChatRoomScreen(
    onNavigateToMediaViewer: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatRoomViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var newMessageCount by remember { mutableIntStateOf(0) }
    var lastSeenCount by remember { mutableIntStateOf(0) }

    val isNearBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems == 0 || lastVisible >= totalItems - 3
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is ChatUiEffect.ScrollToBottom -> {
                    if (isNearBottom) {
                        listState.animateScrollToItem(
                            (state.messages.size - 1).coerceAtLeast(0)
                        )
                        newMessageCount = 0
                        lastSeenCount = state.messages.size
                    } else {
                        newMessageCount = state.messages.size - lastSeenCount
                    }
                }

                is ChatUiEffect.NavigateToMediaViewer ->
                    onNavigateToMediaViewer(effect.mediaUrl)

                is ChatUiEffect.ShowSnackbar ->
                    snackbarHost.showSnackbar(effect.message)

                is ChatUiEffect.OpenMediaPicker ->
                    viewModel.onEvent(ChatUiEvent.AttachmentClicked)
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .filter { it == 0 }
            .debounce(300)
            .distinctUntilChanged()
            .collect {
                if (!state.isLoadingOlder && state.hasMoreMessages) {
                    viewModel.onEvent(ChatUiEvent.LoadOlderMessages)
                }
            }
    }

    // ── Main scaffold ─────────────────────────────────────────────────────────
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                    text = "Chat Room",
                    style = MaterialTheme.typography.titleLarge
                    )
                        },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = state.inputText,
                selectedMediaUris = state.selectedMediaUris,
                isSendEnabled = state.isSendEnabled,
                onInputChanged = { viewModel.onEvent(ChatUiEvent.MessageInputChanged(it)) },
                onSendClick = {
                    if (state.selectedMediaUris.isNotEmpty()) {
                        viewModel.onEvent(ChatUiEvent.SendMediaClicked(state.selectedMediaUris))
                    } else {
                        viewModel.onEvent(ChatUiEvent.SendTextClicked)
                    }
                },
                onAttachmentClick = { viewModel.onEvent(ChatUiEvent.AttachmentClicked) },
                onRemoveMedia = { viewModel.onEvent(ChatUiEvent.RemoveSelectedMedia(it)) },
            )
        },
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                reverseLayout = false,
            ) {

                // ── CHAT-052: Header — loading indicator or end-of-history ───
                item(key = "header") {
                    when {
                        state.isLoadingOlder -> LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )

                        !state.hasMoreMessages && state.messages.isNotEmpty() ->
                            Text(
                                text = "Beginning of conversation",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                            )

                        else -> androidx.compose.foundation.layout.Spacer(
                            Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                if (state.isEmpty) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No messages yet. Say hello! 👋",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                items(
                    items = state.messages,
                    key = { it.localId }
                ) { message ->
                    MessageBubble(
                        message = message,
                        modifier = Modifier.animateItem(),
                        onRetryClick = { viewModel.onEvent(ChatUiEvent.RetryMessageClicked(it)) },
                        onLongPress = { viewModel.onEvent(ChatUiEvent.MessageLongPressed(it)) },
                        onImageClick = { onNavigateToMediaViewer(it) },
                    )
                }
            }

            AnimatedVisibility(
                visible = !isNearBottom,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
            ) {
                BadgedBox(
                    badge = {
                        if (newMessageCount > 0) {
                            Badge {
                                Text(if (newMessageCount > 99) "99+" else "$newMessageCount")
                            }
                        }
                    },
                ) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(
                                    (state.messages.size - 1).coerceAtLeast(0)
                                )
                                newMessageCount = 0
                                lastSeenCount = state.messages.size
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to bottom"
                        )
                    }
                }
            }
        }
    }

    if (state.showMediaPicker) {
        MediaPickerBottomSheet(
            onDismiss = { viewModel.onEvent(ChatUiEvent.MediaPickerDismissed) },
            onMediaSelected = { uris: List<Uri> ->
                viewModel.onEvent(ChatUiEvent.MediaSelected(uris))
            },
        )
    }

    val selectedMessage = state.selectedMessage
    if (selectedMessage != null) {
        MessageContextMenu(
            message = selectedMessage,
            onDismiss = { viewModel.onEvent(ChatUiEvent.DismissMessageContextMenu) },
            onRetry = { viewModel.onEvent(ChatUiEvent.RetryMessageClicked(selectedMessage.localId)) },
            onDelete = {
                viewModel.onEvent(
                    ChatUiEvent.DeleteMessageClicked(
                        localId = selectedMessage.localId,
                        firebaseKey = selectedMessage.firebaseKey,
                        senderUid = selectedMessage.senderUid,
                        type = selectedMessage.type
                    )
                )
            },
        )
    }
}