package com.hesham0_0.marassel.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Design system preview — use this to visually validate the theme
 * in Android Studio's Preview panel without running the app.
 *
 * Shows: colors, typography, shapes, spacing, and bubble styles.
 */
@Preview(name = "Theme — Light", showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
private fun ThemePreviewLight() = ThemePreviewContent(darkTheme = false)

@Preview(name = "Theme — Dark", showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun ThemePreviewDark() = ThemePreviewContent(darkTheme = true)

@Composable
private fun ThemePreviewContent(darkTheme: Boolean) {
    MarasselTheme(darkTheme = darkTheme, dynamicColor = false) {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                // Typography
                Text("Headline Medium", style = MaterialTheme.typography.headlineMedium)
                Text("Title Large", style = MaterialTheme.typography.titleLarge)
                Text("Body Large — message content goes here", style = MaterialTheme.typography.bodyLarge)
                Text("Body Small · 12:34 PM", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(Modifier.height(8.dp))

                // Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}) { Text("Primary") }
                    OutlinedButton(onClick = {}) { Text("Outlined") }
                }

                Spacer(Modifier.height(8.dp))

                // Message bubbles preview
                Text("Message Bubbles", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Outgoing bubble
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Box(
                        modifier = Modifier
                            .clip(BubbleShapeOutgoing)
                            .background(MaterialTheme.bubbleColors.outgoingBackground)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "Hey! How are you? 👋",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.bubbleColors.outgoingContent,
                        )
                    }
                }

                // Incoming bubble
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Box(
                        modifier = Modifier
                            .clip(BubbleShapeIncoming)
                            .background(MaterialTheme.bubbleColors.incomingBackground)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "Doing great, thanks! 😊",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.bubbleColors.incomingContent,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Avatar colors preview
                Text("Avatar Colors", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AvatarColors.take(7).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(ChatSizes.AvatarMedium)
                                .clip(CircleShape)
                                .background(color),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("AB", style = MaterialTheme.typography.labelMedium,
                                color = White)
                        }
                    }
                }
            }
        }
    }
}