package com.hesham0_0.marassel.ui.chat.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hesham0_0.marassel.ui.media.MediaPreviewRow
import com.hesham0_0.marassel.ui.theme.ChatSizes
import com.hesham0_0.marassel.ui.theme.InputShape
import com.hesham0_0.marassel.ui.theme.MarasselTheme

@Composable
fun ChatInputBar(
    inputText: String,
    selectedMediaUris: List<Uri>,
    isSendEnabled: Boolean,
    typingLabel: String,
    hasTypingUsers: Boolean,
    onInputChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onRemoveMedia: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 4.dp),
        ) {

            // Typing Indicator
            AnimatedVisibility(
                visible = hasTypingUsers,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Text(
                    text = typingLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            AnimatedVisibility(
                visible = selectedMediaUris.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                MediaPreviewRow(
                    uris = selectedMediaUris,
                    onRemoveUri = onRemoveMedia,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Attachment button
                IconButton(
                    onClick = onAttachmentClick,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterVertically),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Attach media",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                TextField(
                    value = inputText,
                    onValueChange = onInputChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message…") },
                    maxLines = ChatSizes.InputBarMaxLines,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default, // allow newlines
                    ),
                    shape = InputShape,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSendEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .align(Alignment.CenterVertically)
                        .clickable(enabled = isSendEnabled, onClick = onSendClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (isSendEnabled) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatInputBarPreview() {
    MarasselTheme {
        ChatInputBar(
            inputText = "Hello!",
            selectedMediaUris = emptyList(),
            isSendEnabled = true,
            typingLabel = "Alice is typing...",
            hasTypingUsers = true,
            onInputChanged = {},
            onSendClick = {},
            onAttachmentClick = {},
            onRemoveMedia = {},
        )
    }
}
