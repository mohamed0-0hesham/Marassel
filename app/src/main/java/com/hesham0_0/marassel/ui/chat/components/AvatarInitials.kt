package com.hesham0_0.marassel.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hesham0_0.marassel.ui.theme.AvatarColors
import com.hesham0_0.marassel.ui.theme.MarasselTheme
import com.hesham0_0.marassel.ui.theme.White

@Composable
fun AvatarInitials(
    initials: String,
    username: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val backgroundColor = remember(username) {
        AvatarColors[username.trim().hashCode().mod(AvatarColors.size)]
    }

    val fontSize = remember(initials, size) {
        when {
            initials.length >= 2 -> (size.value * 0.33f).sp
            else                 -> (size.value * 0.40f).sp
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = initials.take(2),
            color      = White,
            fontSize   = fontSize,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AvatarInitialsPreview() {
    MarasselTheme {
        AvatarInitials(initials = "JD", username = "John Doe")
    }
}