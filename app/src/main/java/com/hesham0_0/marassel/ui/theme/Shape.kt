package com.hesham0_0.marassel.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp


val ChatShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// ── Custom shape tokens not in the M3 scale ───────────────────────────────────

/** Outgoing message bubble — fully rounded on left, less on bottom-right "tail" */
val BubbleShapeOutgoing = RoundedCornerShape(
    topStart    = 16.dp,
    topEnd      = 16.dp,
    bottomStart = 16.dp,
    bottomEnd   = 4.dp,   // ← subtle "tail" pointing right
)

/** Incoming message bubble — fully rounded on right, less on bottom-left "tail" */
val BubbleShapeIncoming = RoundedCornerShape(
    topStart    = 16.dp,
    topEnd      = 16.dp,
    bottomStart = 4.dp,   // ← subtle "tail" pointing left
    bottomEnd   = 16.dp,
)

/** Fully circular shape — used for avatars and icon buttons */
val CircleShape = RoundedCornerShape(percent = 50)

/** Input field — slightly rounded for a modern look */
val InputShape = RoundedCornerShape(24.dp)

/** Media thumbnail grid items */
val MediaThumbnailShape = RoundedCornerShape(8.dp)