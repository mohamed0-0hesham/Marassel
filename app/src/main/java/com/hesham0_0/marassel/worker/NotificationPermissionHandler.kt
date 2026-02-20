package com.hesham0_0.marassel.worker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat

fun Context.isNotificationPermissionGranted(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return NotificationManagerCompat.from(this).areNotificationsEnabled()
}

fun Context.openNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(intent)
}

@Composable
fun NotificationPermissionHandler(
    onPermissionResult: (granted: Boolean) -> Unit = {},
) {
    // On API < 33 there's nothing to do
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current

    // Already granted — nothing to do
    if (context.isNotificationPermissionGranted()) return

    var showRationaleDialog by remember { mutableStateOf(true) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (!isGranted) {
            // If the system dialog was shown and denied, we can't distinguish
            // "denied once" from "permanently denied" without shouldShowRequestPermissionRationale.
            // Since we're in Compose we check via a flag set before launching.
            isPermanentlyDenied = !isGranted
        }
        onPermissionResult(isGranted)
    }

    if (showRationaleDialog) {
        if (isPermanentlyDenied) {
            // Permanently denied — offer Settings link
            NotificationPermissionPermanentlyDeniedDialog(
                onOpenSettings = {
                    showRationaleDialog = false
                    context.openNotificationSettings()
                },
                onDismiss = { showRationaleDialog = false },
            )
        } else {
            // First request — show rationale
            NotificationPermissionRationaleDialog(
                onAllow = {
                    showRationaleDialog = false
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onDismiss = {
                    showRationaleDialog = false
                    onPermissionResult(false)
                },
            )
        }
    }
}

@Composable
private fun NotificationPermissionRationaleDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Stay updated on your messages") },
        text    = {
            Text(
                "Enable notifications so you're alerted when a message " +
                        "fails to send or when an upload completes in the background."
            )
        },
        confirmButton = {
            TextButton(onClick = onAllow) { Text("Allow") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        },
    )
}

@Composable
private fun NotificationPermissionPermanentlyDeniedDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Notifications are disabled") },
        text    = {
            Text(
                "To receive alerts for failed messages and uploads, " +
                        "enable notifications for Marassel in your device settings."
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text("Open Settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        },
    )
}