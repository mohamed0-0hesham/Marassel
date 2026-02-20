package com.hesham0_0.marassel.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Immutable
data class Spacing(
    val xxs: Dp  = 2.dp,
    val xs: Dp   = 4.dp,
    val sm: Dp   = 8.dp,
    val md: Dp   = 16.dp,
    val lg: Dp   = 24.dp,
    val xl: Dp   = 32.dp,
    val xxl: Dp  = 48.dp,
    val xxxl: Dp = 64.dp,
)


val LocalSpacing = staticCompositionLocalOf { Spacing() }

// ── Sizing tokens ─────────────────────────────────────────────────────────────
// Fixed sizes for specific UI elements — not part of the spacing grid.

object ChatSizes {
    // Avatars
    val AvatarSmall  = 32.dp
    val AvatarMedium = 40.dp
    val AvatarLarge  = 56.dp

    // Message bubbles
    val BubbleMaxWidthFraction = 0.75f   // bubbles take at most 75% of screen width
    val BubbleMinWidth = 64.dp

    // Input bar
    val InputBarMinHeight = 56.dp
    val InputBarMaxLines  = 5

    // Media
    val MediaThumbnailSize    = 72.dp
    val MediaPreviewMaxHeight = 240.dp

    // Icons
    val IconSmall  = 16.dp
    val IconMedium = 24.dp
    val IconLarge  = 32.dp

    // Status indicator
    val StatusIconSize = 14.dp

    // Bottom nav / top bar
    val TopBarHeight = 64.dp
}