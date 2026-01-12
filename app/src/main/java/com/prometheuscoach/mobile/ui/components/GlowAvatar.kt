package com.prometheuscoach.mobile.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.prometheuscoach.mobile.ui.theme.*

/**
 * Consistent avatar component with Prometheus-branded orange glow ring.
 * Use this component throughout the app for all avatar displays.
 *
 * @param avatarUrl URL of the avatar image, can be null
 * @param name Name to display as fallback initial
 * @param size Size of the avatar in dp (default 48dp)
 * @param showGlow Whether to show the orange glow effect (default true)
 * @param borderWidth Width of the orange gradient border (default 2dp)
 */
@Composable
fun GlowAvatar(
    avatarUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showGlow: Boolean = true,
    borderWidth: Dp = 2.dp
) {
    Box(
        modifier = modifier
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = avatarUrl,
                contentDescription = "$name avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading -> {
                        AvatarLoadingPlaceholder(size = size)
                    }
                    is AsyncImagePainter.State.Error -> {
                        Log.e("GlowAvatar", "Failed to load avatar for $name: $avatarUrl")
                        AvatarFallback(name = name, size = size)
                    }
                    is AsyncImagePainter.State.Success -> {
                        SubcomposeAsyncImageContent(
                            modifier = Modifier
                                .size(size)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        AvatarFallback(name = name, size = size)
                    }
                }
            }
        } else {
            AvatarFallback(name = name, size = size)
        }
    }
}

/**
 * Small glow avatar variant for list items (32dp)
 */
@Composable
fun GlowAvatarSmall(
    avatarUrl: String?,
    name: String,
    modifier: Modifier = Modifier
) {
    GlowAvatar(
        avatarUrl = avatarUrl,
        name = name,
        modifier = modifier,
        size = 32.dp,
        borderWidth = 1.5.dp
    )
}

/**
 * Medium glow avatar variant for cards/headers (56dp)
 */
@Composable
fun GlowAvatarMedium(
    avatarUrl: String?,
    name: String,
    modifier: Modifier = Modifier
) {
    GlowAvatar(
        avatarUrl = avatarUrl,
        name = name,
        modifier = modifier,
        size = 56.dp,
        borderWidth = 2.dp
    )
}

/**
 * Large glow avatar variant for profile pages (120dp)
 */
@Composable
fun GlowAvatarLarge(
    avatarUrl: String?,
    name: String,
    modifier: Modifier = Modifier
) {
    GlowAvatar(
        avatarUrl = avatarUrl,
        name = name,
        modifier = modifier,
        size = 120.dp,
        borderWidth = 3.dp
    )
}

@Composable
private fun AvatarLoadingPlaceholder(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(PrometheusOrange.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size / 3),
            color = PrometheusOrange,
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun AvatarFallback(
    name: String,
    size: Dp
) {
    val fontSize = when {
        size >= 100.dp -> 48.sp
        size >= 56.dp -> 24.sp
        size >= 40.dp -> 18.sp
        else -> 14.sp
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrometheusOrange.copy(alpha = 0.2f),
                        PrometheusOrange.copy(alpha = 0.1f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = PrometheusOrange
        )
    }
}
