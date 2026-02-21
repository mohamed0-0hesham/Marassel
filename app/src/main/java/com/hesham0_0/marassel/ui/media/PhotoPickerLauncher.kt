package com.hesham0_0.marassel.ui.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

const val MAX_MEDIA_ITEMS = 5

fun isPhotoPickerAvailable(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
            PickVisualMedia.isPhotoPickerAvailable()

fun Context.takePersistablePermissions(uris: List<Uri>) {
    uris.forEach { uri ->
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}

@Composable
fun rememberPhotoPickerLauncher(
    mediaType: PickVisualMedia.VisualMediaType = PickVisualMedia.ImageAndVideo,
    onResult: (List<Uri>) -> Unit,
): PhotoPickerLauncher {
    val context = LocalContext.current

    val multiPickerLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, List<Uri>> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_MEDIA_ITEMS),
        ) { uris ->
            if (uris.isNotEmpty()) {
                context.takePersistablePermissions(uris)
                onResult(uris)
            }
        }

    // Fallback launcher for API < 26 or when Photo Picker is not backported
    val fallbackLauncher: ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val uris = result.data?.let { intent ->
                val clipData = intent.clipData
                when {
                    clipData != null -> (0 until clipData.itemCount)
                        .map { clipData.getItemAt(it).uri }
                        .take(MAX_MEDIA_ITEMS)
                    intent.data != null -> listOf(intent.data!!)
                    else -> emptyList()
                }
            } ?: emptyList()

            if (uris.isNotEmpty()) {
                context.takePersistablePermissions(uris)
                onResult(uris)
            }
        }

    return remember(multiPickerLauncher, fallbackLauncher) {
        PhotoPickerLauncher(
            context              = context,
            mediaType            = mediaType,
            multiPickerLauncher  = multiPickerLauncher,
            fallbackLauncher     = fallbackLauncher,
        )
    }
}

class PhotoPickerLauncher(
    private val context: Context,
    private val mediaType: PickVisualMedia.VisualMediaType,
    private val multiPickerLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, List<Uri>>,
    private val fallbackLauncher: ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult>,
) {
    fun launch() {
        if (isPhotoPickerAvailable()) {
            multiPickerLauncher.launch(PickVisualMediaRequest(mediaType))
        } else {
            // Legacy fallback for API < 26
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type     = "image/* video/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            fallbackLauncher.launch(Intent.createChooser(intent, "Select media"))
        }
    }

    fun launchImageOnly() {
        if (isPhotoPickerAvailable()) {
            multiPickerLauncher.launch(
                PickVisualMediaRequest(PickVisualMedia.ImageOnly)
            )
        } else {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type     = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            fallbackLauncher.launch(Intent.createChooser(intent, "Select images"))
        }
    }
}