package com.hesham0_0.marassel.ui.chat.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hesham0_0.marassel.domain.model.MessageStatus
import com.hesham0_0.marassel.domain.model.MessageType
import com.hesham0_0.marassel.ui.chat.MessageUiModel
import com.hesham0_0.marassel.ui.theme.BubbleShapeIncoming
import com.hesham0_0.marassel.ui.theme.BubbleShapeOutgoing
import com.hesham0_0.marassel.ui.theme.ChatSizes
import com.hesham0_0.marassel.ui.theme.MarasselTheme
import com.hesham0_0.marassel.ui.theme.bubbleColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageUiModel,
    modifier: Modifier = Modifier,
    onRetryClick: (String) -> Unit = {},
    onLongPress: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {},
) {
    val colors = MaterialTheme.bubbleColors
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxWidth = screenWidth * ChatSizes.BubbleMaxWidthFraction

    if (message.showDayDivider && message.dayDividerLabel != null) {
        DayDivider(label = message.dayDividerLabel)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .animateContentSize(),
        horizontalArrangement = if (message.isFromCurrentUser)
            Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (message.isFromCurrentUser) {
            // ── Outgoing ──────────────────────────────────────────────────────
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                BubbleContent(
                    message = message,
                    isOutgoing = true,
                    maxWidth = maxWidth,
                    onLongPress = onLongPress,
                    onImageClick = onImageClick,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.padding(end = 4.dp, top = 2.dp),
                ) {
                    Text(
                        text = message.formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )
                    Spacer(Modifier.width(4.dp))
                    MessageStatusIndicator(
                        status = message.status,
                        uploadProgress = message.uploadProgress,
                        onRetryClick = { onRetryClick(message.localId) },
                    )
                }
            }
        } else {

            if (message.isLastInBurst) {
                AvatarInitials(
                    initials = message.senderInitials,
                    username = message.senderName,
                    size = ChatSizes.AvatarSmall,
                    modifier = Modifier.padding(end = 6.dp),
                )
            } else {
                Spacer(Modifier.size(ChatSizes.AvatarSmall + 6.dp))
            }

            Column(horizontalAlignment = Alignment.Start) {
                if (message.showSenderInfo) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                    )
                }
                BubbleContent(
                    message = message,
                    isOutgoing = false,
                    maxWidth = maxWidth,
                    onLongPress = onLongPress,
                    onImageClick = onImageClick,
                )
                Text(
                    text = message.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BubbleContent(
    message: MessageUiModel,
    isOutgoing: Boolean,
    maxWidth: androidx.compose.ui.unit.Dp,
    onLongPress: (String) -> Unit,
    onImageClick: (String) -> Unit,
) {
    val colors = MaterialTheme.bubbleColors
    val bubbleColor = if (isOutgoing) colors.outgoingBackground else colors.incomingBackground
    val textColor = if (isOutgoing) colors.outgoingContent else colors.incomingContent
    val shape = if (isOutgoing) BubbleShapeOutgoing else BubbleShapeIncoming

    Box(
        modifier = Modifier
            .widthIn(min = ChatSizes.BubbleMinWidth, max = maxWidth)
            .clip(shape)
            .background(bubbleColor)
            .combinedClickable(
                onClick = {},
                onLongClick = { onLongPress(message.localId) },
            ),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (message.isMedia && message.text == null) 0.dp else 10.dp,
                vertical = if (message.isMedia && message.text == null) 0.dp else 8.dp,
            )
        ) {
            // Media content (images/video)
            if (message.isMedia) {
                MediaMessageContent(
                    mediaUrls = listOfNotNull(message.mediaUrl),
                    uploadProgress = message.uploadProgress,
                    onImageClick = onImageClick,
                    onLongClick = { onLongPress(message.localId) },
                    modifier = if (message.text != null)
                        Modifier.padding(bottom = 6.dp) else Modifier,
                )
            }
            // Text content
            if (!message.text.isNullOrBlank()) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    modifier = Modifier.padding(
                        // Extra padding when text follows media
                        horizontal = if (message.isMedia) 10.dp else 0.dp,
                        vertical = if (message.isMedia) 8.dp else 0.dp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DayDivider(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .size(height = 1.dp, width = 0.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .size(height = 1.dp, width = 0.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OutgoingTextPreview() {
    MarasselTheme {
        MessageBubble(
            message = previewMessage(
                text = "Hey, this is an outgoing message!",
                isOutgoing = true,
                status = MessageStatus.SENT,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IncomingTextPreview() {
    MarasselTheme {
        MessageBubble(
            message = previewMessage(
                text = "And this is an incoming one 👋",
                isOutgoing = false,
                status = MessageStatus.SENT,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FailedMessagePreview() {
    MarasselTheme {
        MessageBubble(
            message = previewMessage(
                text = "This failed to send",
                isOutgoing = true,
                status = MessageStatus.FAILED,
            ),
        )
    }
}

private fun previewMessage(
    text: String,
    isOutgoing: Boolean,
    status: MessageStatus,
) = MessageUiModel(
    localId = "preview",
    firebaseKey = null,
    senderUid = "uid1",
    senderName = "Alice",
    senderInitials = "A",
    text = text,
    mediaUrl = null,
    mediaType = null,
    timestamp = System.currentTimeMillis(),
    formattedTime = "3:45 PM",
    status = status,
    type = MessageType.TEXT,
    replyToId = null,
    isFromCurrentUser = isOutgoing,
    showSenderInfo = !isOutgoing,
    isLastInBurst = true,
    showDayDivider = false,
    dayDividerLabel = null,
)