package com.hesham0_0.marassel.ui.chat.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hesham0_0.marassel.domain.model.MessageStatus
import com.hesham0_0.marassel.ui.theme.Green500
import com.hesham0_0.marassel.ui.theme.MarasselTheme

@Composable
fun MessageStatusIndicator(
    status: MessageStatus,
    modifier: Modifier = Modifier,
    uploadProgress: Int? = null,
    onRetryClick: () -> Unit = {},
) {
    AnimatedContent(
        targetState  = status to uploadProgress,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
        label        = "message_status",
        modifier     = modifier,
    ) { (currentStatus, progress) ->
        when (currentStatus) {
            MessageStatus.PENDING -> PendingIndicator(progress)
            MessageStatus.SENT    -> SentIndicator()
            MessageStatus.FAILED  -> FailedIndicator(onRetryClick)
        }
    }
}

@Composable
private fun PendingIndicator(uploadProgress: Int?) {
    Box(
        modifier          = Modifier.size(INDICATOR_SIZE),
        contentAlignment  = Alignment.Center,
    ) {
        if (uploadProgress != null) {
            // Determinate — shows actual upload %
            CircularProgressIndicator(
                progress      = { uploadProgress / 100f },
                modifier      = Modifier.size(INDICATOR_SIZE),
                strokeWidth   = 1.5.dp,
                strokeCap     = StrokeCap.Round,
                color         = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                trackColor    = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
            )
            Text(
                text     = "$uploadProgress",
                fontSize = 6.sp,
                color    = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            )
        } else {
            // Indeterminate — message queued, waiting for network/worker
            val infiniteTransition = rememberInfiniteTransition(label = "spinner")
            val rotation by infiniteTransition.animateFloat(
                initialValue   = 0f,
                targetValue    = 360f,
                animationSpec  = infiniteRepeatable(
                    animation  = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "spinner_rotation",
            )
            CircularProgressIndicator(
                modifier    = Modifier.size(INDICATOR_SIZE),
                strokeWidth = 1.5.dp,
                strokeCap   = StrokeCap.Round,
                color       = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun SentIndicator() {
    Icon(
        imageVector        = Icons.Default.CheckCircle,
        contentDescription = "Sent",
        modifier           = Modifier.size(INDICATOR_SIZE),
        tint               = Green500.copy(alpha = 0.9f),
    )
}

@Composable
private fun FailedIndicator(onRetryClick: () -> Unit) {
    Row(
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector        = Icons.Default.ErrorOutline,
            contentDescription = "Failed",
            modifier           = Modifier.size(INDICATOR_SIZE),
            tint               = MaterialTheme.colorScheme.error,
        )
        TextButton(
            onClick          = onRetryClick,
            contentPadding   = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 4.dp, vertical = 0.dp,
            ),
        ) {
            Text(
                text     = "Retry",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private val INDICATOR_SIZE = 14.dp

@Preview(showBackground = true, backgroundColor = 0xFF2563EB)
@Composable
private fun PendingPreview() {
    MarasselTheme { MessageStatusIndicator(status = MessageStatus.PENDING) }
}

@Preview(showBackground = true, backgroundColor = 0xFF2563EB)
@Composable
private fun UploadingPreview() {
    MarasselTheme { MessageStatusIndicator(status = MessageStatus.PENDING, uploadProgress = 65) }
}

@Preview(showBackground = true, backgroundColor = 0xFF2563EB)
@Composable
private fun SentPreview() {
    MarasselTheme { MessageStatusIndicator(status = MessageStatus.SENT) }
}

@Preview(showBackground = true, backgroundColor = 0xFF2563EB)
@Composable
private fun FailedPreview() {
    MarasselTheme { MessageStatusIndicator(status = MessageStatus.FAILED) }
}