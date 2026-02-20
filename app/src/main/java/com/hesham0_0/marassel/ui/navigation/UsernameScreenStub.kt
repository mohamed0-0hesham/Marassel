package com.hesham0_0.marassel.ui.navigation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
internal fun UsernameScreenStub(
    onNavigateToChatRoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Username Screen — CHAT-015")
    }
}

@Composable
internal fun ChatRoomScreenStub(
    onNavigateToMediaViewer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Chat Room Screen — CHAT-044")
    }
}

@Composable
internal fun MediaViewerScreenStub(
    mediaUrl: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Media Viewer — CHAT-048\n$mediaUrl")
    }
}