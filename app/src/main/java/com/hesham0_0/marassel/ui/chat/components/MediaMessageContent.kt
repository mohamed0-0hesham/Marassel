package com.hesham0_0.marassel.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.hesham0_0.marassel.ui.theme.MediaThumbnailShape

@Composable
fun MediaMessageContent(
    mediaUrls: List<String>,
    modifier: Modifier = Modifier,
    localUri: String? = null,
    uploadProgress: Int? = null,
    onImageClick: (String) -> Unit = {},
) {

    val displayUrls = mediaUrls.ifEmpty {
        listOfNotNull(localUri)
    }

    if (displayUrls.isEmpty()) return

    Box(modifier = modifier.fillMaxWidth()) {
        when (displayUrls.size) {
            1 -> SingleImage(
                url = displayUrls[0],
                uploadProgress = uploadProgress,
                onClick = { onImageClick(displayUrls[0]) },
            )

            2 -> TwoImageRow(
                urls = displayUrls,
                onClick = onImageClick,
            )

            3 -> ThreeImageLayout(
                urls = displayUrls,
                onClick = onImageClick,
            )

            else -> FourPlusGrid(
                urls = displayUrls,
                onClick = onImageClick,
            )
        }
    }
}

@Composable
private fun SingleImage(
    url: String,
    uploadProgress: Int?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MAX_MEDIA_HEIGHT)
            .clip(MediaThumbnailShape)
            .clickable(onClick = onClick),
    ) {
        MediaImage(
            url = url,
            modifier = Modifier.fillMaxSize(),
        )
        if (uploadProgress != null) {
            UploadProgressOverlay(progress = uploadProgress)
        }
    }
}

@Composable
private fun TwoImageRow(urls: List<String>, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GRID_GAP),
    ) {
        urls.take(2).forEach { url ->
            MediaImage(
                url = url,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(MediaThumbnailShape)
                    .clickable { onClick(url) },
            )
        }
    }
}

@Composable
private fun ThreeImageLayout(urls: List<String>, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MAX_MEDIA_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(GRID_GAP),
    ) {

        MediaImage(
            url = urls[0],
            modifier = Modifier
                .weight(1.3f)
                .fillMaxSize()
                .clip(MediaThumbnailShape)
                .clickable { onClick(urls[0]) },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(GRID_GAP),
        ) {
            urls.drop(1).take(2).forEach { url ->
                MediaImage(
                    url = url,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(MediaThumbnailShape)
                        .clickable { onClick(url) },
                )
            }
        }
    }
}

@Composable
private fun FourPlusGrid(urls: List<String>, onClick: (String) -> Unit) {
    val visible = urls.take(4)
    val overflow = urls.size - 4

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GRID_GAP),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(GRID_CELL_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(GRID_GAP),
        ) {
            visible.take(2).forEach { url ->
                MediaImage(
                    url = url,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(MediaThumbnailShape)
                        .clickable { onClick(url) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(GRID_CELL_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(GRID_GAP),
        ) {
            visible.drop(2).forEachIndexed { index, url ->
                val isLast = index == 1 && overflow > 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(MediaThumbnailShape)
                        .clickable { onClick(url) },
                ) {
                    MediaImage(url = url, modifier = Modifier.fillMaxSize())
                    if (isLast) {

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "+$overflow",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Shared image loader ───────────────────────────────────────────────────────

@Composable
private fun MediaImage(url: String, modifier: Modifier) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = "Media",
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "⚠",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    )
}

@Composable
private fun UploadProgressOverlay(progress: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.size(40.dp),
            strokeWidth = 3.dp,
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.3f),
        )
        Text(
            text = "$progress%",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 48.dp),
        )
    }
}

private val MAX_MEDIA_HEIGHT = 240.dp
private val GRID_GAP = 2.dp
private val GRID_CELL_HEIGHT = 110.dp