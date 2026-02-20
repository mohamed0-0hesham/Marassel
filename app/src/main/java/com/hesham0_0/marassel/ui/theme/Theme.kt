package com.hesham0_0.marassel.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.rememberSystemUiController

// ── Light Color Scheme ────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    // Primary — brand blue used for buttons, FAB, active states
    primary          = Brand500,
    onPrimary        = White,
    primaryContainer = Brand100,
    onPrimaryContainer = Brand700,

    // Secondary — muted blue-gray for chips, toggles
    secondary          = Neutral500,
    onSecondary        = White,
    secondaryContainer = Neutral100,
    onSecondaryContainer = Neutral800,

    // Tertiary — accent for special highlights (typing indicator, etc.)
    tertiary          = Color(0xFF7C3AED),  // violet
    onTertiary        = White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF4C1D95),

    // Error — failed messages, validation errors
    error          = Red500,
    onError        = White,
    errorContainer = Red100,
    onErrorContainer = Red600,

    // Surface — card backgrounds, bottom sheets, dialogs
    surface          = White,
    onSurface        = Neutral900,
    surfaceVariant   = Neutral100,
    onSurfaceVariant = Neutral600,

    // Background — screen background
    background   = Neutral050,
    onBackground = Neutral900,

    // Outline — borders, dividers, text field outlines
    outline        = Neutral300,
    outlineVariant = Neutral200,

    // Inverse — used in snackBars
    inverseSurface   = Neutral800,
    inverseOnSurface = Neutral100,
    inversePrimary   = Brand400,

    // Scrim — behind modal sheets
    scrim = Black.copy(alpha = 0.32f),
)

// ── Dark Color Scheme ─────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    // Primary
    primary          = Brand400,
    onPrimary        = Neutral900,
    primaryContainer = Brand700,
    onPrimaryContainer = Brand100,

    // Secondary
    secondary          = Neutral400,
    onSecondary        = Neutral900,
    secondaryContainer = Neutral700,
    onSecondaryContainer = Neutral100,

    // Tertiary
    tertiary          = Color(0xFFA78BFA),
    onTertiary        = Neutral900,
    tertiaryContainer = Color(0xFF4C1D95),
    onTertiaryContainer = Color(0xFFEDE9FE),

    // Error
    error          = Red400,
    onError        = Neutral900,
    errorContainer = Red600,
    onErrorContainer = Red100,

    // Surface
    surface          = Neutral850,
    onSurface        = Neutral100,
    surfaceVariant   = Neutral800,
    onSurfaceVariant = Neutral400,

    // Background
    background   = Neutral900,
    onBackground = Neutral100,

    // Outline
    outline        = Neutral600,
    outlineVariant = Neutral700,

    // Inverse
    inverseSurface   = Neutral100,
    inverseOnSurface = Neutral800,
    inversePrimary   = Brand600,

    // Scrim
    scrim = Black.copy(alpha = 0.5f),
)

/**
 * Root theme composable for the ChatApp.
 *
 * Features:
 * - Full Material 3 light and dark color schemes
 * - Dynamic Color on Android 12+ (wallpaper-based theming)
 * - Custom typography, shapes, and spacing tokens
 * - Status bar and navigation bar color management
 *
 * @param darkTheme Follow system dark mode by default
 * @param dynamicColor Use Android 12+ dynamic color (true by default)
 * @param content The composable content wrapped by this theme
 */
@Composable
fun MarasselTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    // ── Color scheme resolution ───────────────────────────────────────────────
    val colorScheme = when {
        // Dynamic Color: Android 12+ picks colors from the user's wallpaper.
        // Falls back to our custom scheme on older API levels.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    // ── System bars ───────────────────────────────────────────────────────────
    // Make status bar and navigation bar blend with our background color.
    // Uses Accompanist SystemUiController for compatibility across API levels.
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = !darkTheme,
        )
    }

    // ── Provide custom tokens ─────────────────────────────────────────────────
    // Spacing is provided via CompositionLocal so any composable can access
    // it without prop-drilling.
    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = ChatTypography,
            shapes      = ChatShapes,
            content     = content,
        )
    }
}

// ── Theme extension properties ────────────────────────────────────────────────
// Shortcuts for accessing our custom tokens alongside MaterialTheme.

/**
 * Access spacing tokens:
 * ```
 * MaterialTheme.spacing.md
 * ```
 */
val androidx.compose.material3.MaterialTheme.spacing: Spacing
    @Composable
    get() = LocalSpacing.current

/**
 * Resolve the correct bubble color for the current theme.
 * Usage: MaterialTheme.bubbleColors.outgoing
 */
data class BubbleColors(
    val outgoingBackground: Color,
    val outgoingContent: Color,
    val incomingBackground: Color,
    val incomingContent: Color,
)

val androidx.compose.material3.MaterialTheme.bubbleColors: BubbleColors
    @Composable
    get() = if (isSystemInDarkTheme()) {
        BubbleColors(
            outgoingBackground = BubbleOutgoingDark,
            outgoingContent    = BubbleOutgoingText,
            incomingBackground = BubbleIncomingDark,
            incomingContent    = BubbleIncomingTextDark,
        )
    } else {
        BubbleColors(
            outgoingBackground = BubbleOutgoingLight,
            outgoingContent    = BubbleOutgoingText,
            incomingBackground = BubbleIncomingLight,
            incomingContent    = BubbleIncomingTextLight,
        )
    }