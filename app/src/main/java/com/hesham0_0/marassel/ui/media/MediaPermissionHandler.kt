package com.hesham0_0.marassel.ui.media

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

fun mediaReadPermission(): String? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
        Manifest.permission.READ_MEDIA_IMAGES
    else ->
        Manifest.permission.READ_EXTERNAL_STORAGE
}

fun Context.hasMediaReadPermission(): Boolean {
    val permission = mediaReadPermission() ?: return true   // Photo Picker path — no permission needed
    return ContextCompat.checkSelfPermission(this, permission) ==
            PermissionChecker.PERMISSION_GRANTED
}

fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data  = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
}

/**
 * Stable state holder for the media permission flow.
 *
 * Consumed by [rememberMediaPermissionState] and used in composables
 * that need to trigger permission requests and react to results.
 */
@Stable
class MediaPermissionState(
    val hasPermission: Boolean,
    val shouldShowRationale: Boolean,
    val isPermanentlyDenied: Boolean,
    val onRequestPermission: () -> Unit,
    val onOpenSettings: () -> Unit,
)

@Composable
fun rememberMediaPermissionState(
    onGranted: () -> Unit = {},
): MediaPermissionState {
    val context    = LocalContext.current
    val permission = mediaReadPermission()

    // If no permission needed (Photo Picker path), return granted immediately
    if (permission == null) {
        return remember {
            MediaPermissionState(
                hasPermission        = true,
                shouldShowRationale  = false,
                isPermanentlyDenied  = false,
                onRequestPermission  = onGranted,
                onOpenSettings       = {},
            )
        }
    }

    var hasPermission       by remember { mutableStateOf(context.hasMediaReadPermission()) }
    var permissionRequested by remember { mutableStateOf(false) }

    val launcher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasPermission       = isGranted
            permissionRequested = true
            if (isGranted) onGranted()
        }

    val isPermanentlyDenied = permissionRequested && !hasPermission

    return remember(hasPermission, isPermanentlyDenied) {
        MediaPermissionState(
            hasPermission       = hasPermission,
            shouldShowRationale = !hasPermission && !isPermanentlyDenied,
            isPermanentlyDenied = isPermanentlyDenied,
            onRequestPermission = { launcher.launch(permission) },
            onOpenSettings      = { context.openAppSettings() },
        )
    }
}