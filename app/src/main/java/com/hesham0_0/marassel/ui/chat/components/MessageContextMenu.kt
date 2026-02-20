package com.hesham0_0.marassel.ui.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.hesham0_0.marassel.ui.chat.MessageUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageContextMenu(
    message: MessageUiModel,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = if (message.isFromCurrentUser) "Your message" else message.senderName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider()

            if (!message.text.isNullOrBlank()) {
                ListItem(
                    headlineContent = { Text("Copy text") },
                    leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(AnnotatedString(message.text))
                            onDismiss()
                        },
                )
            }

            if (message.isFromCurrentUser && message.isFailed) {
                ListItem(
                    headlineContent = { Text("Retry") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onRetry()
                            onDismiss()
                        },
                )
            }

            if (message.isFromCurrentUser) {
                ListItem(
                    headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDelete()
                            onDismiss()
                        },
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
