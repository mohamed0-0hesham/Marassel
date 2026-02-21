package com.hesham0_0.marassel.ui.media

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import coil.compose.SubcomposeAsyncImage
import com.hesham0_0.marassel.ui.theme.MarasselTheme

@Composable
fun MediaViewerScreen(
    mediaUrl: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
    ) {
        val uri = mediaUrl.toUri()
        val path = uri.path?.lowercase() ?: ""
        val isVideo = path.endsWith(".mp4") || path.endsWith(".3gp") || 
                      path.endsWith(".mov") || path.endsWith(".webm") ||
                      mediaUrl.lowercase().contains(".mp4?")

        if (isVideo) {
            var isVideoLoading by remember { mutableStateOf(true) }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            val mediaController = MediaController(context)
                            mediaController.setAnchorView(this)
                            setMediaController(mediaController)
                            setVideoURI(uri)
                            setOnPreparedListener { 
                                it.start() 
                                isVideoLoading = false
                            }
                            setOnErrorListener { _, _, _ ->
                                isVideoLoading = false
                                true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                if (isVideoLoading) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        } else {
            SubcomposeAsyncImage(
                model = mediaUrl,
                contentDescription = "Media Viewer",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaViewerScreenPreview() {
    MarasselTheme {
        MediaViewerScreen(
            mediaUrl = "https://example.com/sample_image.jpg",
            onBack = {}
        )
    }
}
