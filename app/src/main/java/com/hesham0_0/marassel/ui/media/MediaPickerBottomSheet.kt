package com.hesham0_0.marassel.ui.media

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerBottomSheet(
    onDismiss: () -> Unit,
    onMediaSelected: (List<Uri>) -> Unit,
) {
    val context    = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Camera permission ─────────────────────────────────────────────────────
    var pendingCameraLaunch by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) pendingCameraLaunch = true
        // If denied we simply don't open the camera — no rationale needed here
        // as the user just denied it from the system dialog
    }

    val cameraLauncher = rememberCameraCaptureLauncher { uri ->
        onMediaSelected(listOf(uri))
        onDismiss()
    }

    // Launch camera after permission is granted
    if (pendingCameraLaunch) {
        pendingCameraLaunch = false
        cameraLauncher.launch()
    }

    val photoPickerLauncher = rememberPhotoPickerLauncher { uris ->
        onMediaSelected(uris)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text     = "Add attachment",
                style    = MaterialTheme.typography.titleSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider()

            // Camera option
            ListItem(
                headlineContent  = { Text("Camera") },
                supportingContent = { Text("Take a new photo") },
                leadingContent   = {
                    Icon(
                        imageVector        = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA,
                        ) == PermissionChecker.PERMISSION_GRANTED

                        if (hasCameraPermission) {
                            cameraLauncher.launch()
                            onDismiss()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
            )

            // Photo Library option
            ListItem(
                headlineContent  = { Text("Photo Library") },
                supportingContent = { Text("Choose up to $MAX_MEDIA_ITEMS items") },
                leadingContent   = {
                    Icon(
                        imageVector        = Icons.Default.Photo,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        photoPickerLauncher.launch()
                        // Don't dismiss yet — wait for picker result
                    },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}