package com.hesham0_0.marassel.ui.media

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"

fun Context.fileProviderAuthority(): String =
    "$packageName$FILE_PROVIDER_AUTHORITY_SUFFIX"

fun Context.createCameraTempFile(): File {
    val cameraDir = File(cacheDir, "camera").apply { mkdirs() }
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
    return File(cameraDir, "IMG_$timestamp.jpg")
}

fun Context.fileToContentUri(file: File): Uri =
    FileProvider.getUriForFile(this, fileProviderAuthority(), file)

@Composable
fun rememberCameraCaptureLauncher(
    onCaptured: (Uri) -> Unit,
): CameraCaptureLauncher {
    val context = LocalContext.current

    // Holds the URI of the temp file currently being written by the camera
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingFile by remember { mutableStateOf<File?>(null) }

    val launcher: ManagedActivityResultLauncher<Uri, Boolean> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
        ) { success ->
            if (success) {
                pendingUri?.let { onCaptured(it) }
            } else {
                // Clean up the empty temp file if user cancelled
                pendingFile?.delete()
            }
            pendingUri  = null
            pendingFile = null
        }

    return remember(launcher) {
        CameraCaptureLauncher(
            context     = context,
            launcher    = launcher,
            onSetPending = { file, uri ->
                pendingFile = file
                pendingUri  = uri
            },
        )
    }
}

class CameraCaptureLauncher(
    private val context: Context,
    private val launcher: ManagedActivityResultLauncher<Uri, Boolean>,
    private val onSetPending: (File, Uri) -> Unit,
) {
    fun launch() {
        val tempFile   = context.createCameraTempFile()
        val contentUri = context.fileToContentUri(tempFile)
        onSetPending(tempFile, contentUri)
        launcher.launch(contentUri)
    }
}