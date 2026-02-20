package com.hesham0_0.marassel.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand Palette ─────────────────────────────────────────────────────────────
// Primary brand color and its tonal variants
val Brand400 = Color(0xFF60A5FA)   // light mode primary
val Brand500 = Color(0xFF3B82F6)   // buttons, FAB
val Brand600 = Color(0xFF2563EB)   // pressed state
val Brand700 = Color(0xFF1D4ED8)   // dark mode primary
val Brand100 = Color(0xFFDBEAFE)   // light tonal surface
val Brand050 = Color(0xFFEFF6FF)   // very light tonal background

// ── Neutrals ──────────────────────────────────────────────────────────────────
val Neutral050 = Color(0xFFF8FAFC)
val Neutral100 = Color(0xFFF1F5F9)
val Neutral200 = Color(0xFFE2E8F0)
val Neutral300 = Color(0xFFCBD5E1)
val Neutral400 = Color(0xFF94A3B8)
val Neutral500 = Color(0xFF64748B)
val Neutral600 = Color(0xFF475569)
val Neutral700 = Color(0xFF334155)
val Neutral800 = Color(0xFF1E293B)
val Neutral850 = Color(0xFF172033)
val Neutral900 = Color(0xFF0F172A)
val Neutral950 = Color(0xFF080F1E)

// ── Semantic: Success ─────────────────────────────────────────────────────────
val Green400 = Color(0xFF4ADE80)
val Green500 = Color(0xFF22C55E)
val Green600 = Color(0xFF16A34A)
val Green100 = Color(0xFFDCFCE7)

// ── Semantic: Error ───────────────────────────────────────────────────────────
val Red400  = Color(0xFFF87171)
val Red500  = Color(0xFFEF4444)
val Red600  = Color(0xFFDC2626)
val Red100  = Color(0xFFFEE2E2)

// ── Semantic: Warning ─────────────────────────────────────────────────────────
val Amber400 = Color(0xFFFBBF24)
val Amber500 = Color(0xFFF59E0B)
val Amber100 = Color(0xFFFEF3C7)

// ── Chat Bubble Colors ────────────────────────────────────────────────────────
// Outgoing message bubbles (current user)
val BubbleOutgoingLight = Color(0xFF2563EB)    // Brand600
val BubbleOutgoingDark  = Color(0xFF1D4ED8)    // Brand700
val BubbleOutgoingText  = Color(0xFFFFFFFF)

// Incoming message bubbles (other users)
val BubbleIncomingLight = Color(0xFFF1F5F9)    // Neutral100
val BubbleIncomingDark  = Color(0xFF1E293B)    // Neutral800
val BubbleIncomingTextLight = Color(0xFF0F172A) // Neutral900
val BubbleIncomingTextDark  = Color(0xFFF1F5F9) // Neutral100

// ── Avatar Palette ────────────────────────────────────────────────────────────
// Deterministic avatar colors — picked by username.hashCode() % size
val AvatarColors = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFF8B5CF6), // Violet
    Color(0xFFEC4899), // Pink
    Color(0xFFEF4444), // Red
    Color(0xFFF97316), // Orange
    Color(0xFFEAB308), // Yellow
    Color(0xFF22C55E), // Green
    Color(0xFF14B8A6), // Teal
    Color(0xFF3B82F6), // Blue
    Color(0xFF06B6D4), // Cyan
)

// ── Pure ──────────────────────────────────────────────────────────────────────
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val Transparent = Color(0x00000000)