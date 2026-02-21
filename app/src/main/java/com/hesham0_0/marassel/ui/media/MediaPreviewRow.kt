package com.hesham0_0.marassel.ui.media

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.hesham0_0.marassel.ui.theme.ChatSizes
import com.hesham0_0.marassel.ui.theme.MediaThumbnailShape

@Composable
fun MediaPreviewRow(
    uris: List<Uri>,
    onRemoveUri: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible  = uris.isNotEmpty(),
        enter    = expandVertically(expandFrom = Alignment.Bottom),
        exit     = shrinkVertically(shrinkTowards = Alignment.Bottom),
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding        = PaddingValues(end = if (uris.size >= 3) 48.dp else 0.dp),
            ) {
                items(items = uris, key = { it.toString() }) { uri ->
                    MediaThumbnailChip(
                        uri      = uri,
                        onRemove = { onRemoveUri(uri) },
                    )
                }
            }

            // Count badge — shown when 3+ items are selected
            if (uris.size >= 3) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text  = "${uris.size} / $MAX_MEDIA_ITEMS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnailChip(
    uri: Uri,
    onRemove: () -> Unit,
) {
    Box(modifier = Modifier.size(ChatSizes.MediaThumbnailSize)) {
        // Thumbnail image
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = "Selected media",
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .size(ChatSizes.MediaThumbnailSize)
                .clip(MediaThumbnailShape),
            loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        )

        Box(
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                )
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Default.Close,
                contentDescription = "Remove",
                tint               = MaterialTheme.colorScheme.onSurface,
                modifier           = Modifier.size(13.dp),
            )
        }
    }
}
