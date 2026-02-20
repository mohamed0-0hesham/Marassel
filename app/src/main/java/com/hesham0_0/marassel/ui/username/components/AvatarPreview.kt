package com.hesham0_0.marassel.ui.username.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hesham0_0.marassel.domain.model.UserEntity
import com.hesham0_0.marassel.ui.theme.AvatarColors
import com.hesham0_0.marassel.ui.theme.MarasselTheme
import com.hesham0_0.marassel.ui.theme.White

@Composable
fun AvatarPreview(
    username: String,
    isValid: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val initials = remember(username) {
        UserEntity.computeInitials(username.ifBlank { "?" })
    }

    val avatarColor = remember(username) {
        if (username.isBlank()) Color.Gray
        else AvatarColors[username.trim().hashCode().mod(AvatarColors.size)]
    }

    // Animate color: grey when invalid, brand color when valid
    val animatedColor by animateColorAsState(
        targetValue = avatarColor,
        animationSpec = tween(durationMillis = 300),
        label = "avatar_color",
    )

    // Subtle size pulse when name becomes valid
    val animatedSize by animateDpAsState(
        targetValue = if (isValid) size else (size - 4.dp),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium,
        ),
        label = "avatar_size",
    )

    Box(
        modifier = modifier
            .size(animatedSize)
            .shadow(
                elevation = if (isValid) 8.dp else 2.dp,
                shape     = CircleShape,
                ambientColor = animatedColor.copy(alpha = 0.3f),
                spotColor    = animatedColor.copy(alpha = 0.3f),
            )
            .clip(CircleShape)
            .background(animatedColor),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState  = initials,
            transitionSpec = {
                (scaleIn(initialScale = 0.7f) + fadeIn(tween(200))) togetherWith
                        (scaleOut(targetScale = 0.7f) + fadeOut(tween(150)))
            },
            label = "initials",
        ) { displayInitials ->
            Text(
                text      = displayInitials,
                color     = White,
                fontSize  = when {
                    displayInitials.length >= 2 -> (animatedSize.value * 0.32f).sp
                    else                        -> (animatedSize.value * 0.38f).sp
                },
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AvatarPreviewValid() {
    MarasselTheme {
        AvatarPreview(username = "John Doe", isValid = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun AvatarPreviewInvalid() {
    MarasselTheme {
        AvatarPreview(username = "J", isValid = false)
    }
}

@Preview(showBackground = true)
@Composable
private fun AvatarPreviewEmpty() {
    MarasselTheme {
        AvatarPreview(username = "", isValid = false)
    }
}