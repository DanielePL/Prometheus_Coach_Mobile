package com.prometheuscoach.mobile.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.util.Log
import kotlinx.coroutines.delay
import com.prometheuscoach.mobile.data.model.CommunityComment
import com.prometheuscoach.mobile.data.model.FeedPost
import com.prometheuscoach.mobile.data.model.PostType
import com.prometheuscoach.mobile.ui.components.GlowAvatar
import com.prometheuscoach.mobile.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ═══════════════════════════════════════════════════════════════════════════
// FEED POST CARD
// ═══════════════════════════════════════════════════════════════════════════

enum class FeedPostMenuAction {
    DELETE,
    REPORT
}

@Composable
fun FeedPostCard(
    post: FeedPost,
    onUserClick: (String) -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onPostClick: () -> Unit,
    onAddComment: (String) -> Unit = {},
    onMenuClick: ((FeedPostMenuAction) -> Unit)? = null,
    isOwnPost: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showCommentInput by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus comment input when shown
    LaunchedEffect(showCommentInput) {
        if (showCommentInput) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    // Glass card with border
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMedium))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkSurface.copy(alpha = 0.95f),
                        DarkSurface
                    )
                )
            )
            .border(
                width = 1.dp,
                color = DarkBorder,
                shape = RoundedCornerShape(RadiusMedium)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ============ HEADER ROW (user info, badge, menu) ============
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar - clickable to user profile
                UserAvatar(
                    avatarUrl = post.userAvatarUrl,
                    displayName = post.userDisplayName,
                    isCoach = post.isCoach,
                    size = 44,
                    onClick = { onUserClick(post.userId) }
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Name + time - clickable to user profile
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onUserClick(post.userId) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.userDisplayName ?: "User",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.isCoach) {
                            Spacer(modifier = Modifier.width(6.dp))
                            CoachBadge()
                        }
                    }
                    post.createdAt?.let {
                        Text(
                            text = formatTimeAgo(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Post type badge
                PostTypeBadge(postType = post.postType)

                // Menu
                if (onMenuClick != null) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (isOwnPost) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        showMenu = false
                                        onMenuClick(FeedPostMenuAction.DELETE)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Report") },
                                    onClick = {
                                        showMenu = false
                                        onMenuClick(FeedPostMenuAction.REPORT)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Flag, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ============ POST CONTENT (clickable to post detail) ============
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPostClick() }
            ) {
                // Title (if present)
                val postTitle = post.title
                if (!postTitle.isNullOrBlank()) {
                    Text(
                        text = postTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Content
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )

                // Media (images/videos)
                if (post.hasMedia) {
                    Spacer(modifier = Modifier.height(12.dp))
                    PostMediaPreview(
                        imageUrls = post.imageUrls ?: emptyList(),
                        videoUrls = post.videoUrls ?: emptyList()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ============ ENGAGEMENT ROW (separate from content) ============
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLikeClick() }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLiked) Color.Red else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    if (post.likesCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatCount(post.likesCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Comment button - toggles inline comment input
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showCommentInput = !showCommentInput }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = "Comment",
                        tint = if (showCommentInput) PrometheusOrange else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    if (post.commentsCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatCount(post.commentsCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Share button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { /* TODO: Share */ }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ============ INLINE COMMENT INPUT (Instagram-style) ============
            if (showCommentInput) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = {
                            Text(
                                text = "Add a comment...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = PrometheusOrange
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (commentText.isNotBlank()) {
                                    onAddComment(commentText.trim())
                                    commentText = ""
                                    showCommentInput = false
                                }
                            }
                        )
                    )

                    // Send button
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(commentText.trim())
                                commentText = ""
                                showCommentInput = false
                            }
                        },
                        enabled = commentText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Post comment",
                            tint = if (commentText.isNotBlank()) PrometheusOrange else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ============ COMMENTS PREVIEW ============
            if (post.previewComments.isNotEmpty() || post.commentsCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                CommentPreviewSection(
                    comments = post.previewComments,
                    totalCount = post.commentsCount,
                    onViewAllClick = onCommentClick,
                    onUserClick = onUserClick
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// COMMENT PREVIEW SECTION
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun CommentPreviewSection(
    comments: List<CommunityComment>,
    totalCount: Int,
    onViewAllClick: () -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("CommentPreview", "Rendering ${comments.size} comments, totalCount=$totalCount")
    Column(modifier = modifier.fillMaxWidth()) {
        // Show "View all X comments" if there are more comments
        if (totalCount > comments.size) {
            Text(
                text = "View all $totalCount comments",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .clickable { onViewAllClick() }
                    .padding(vertical = 4.dp)
            )
        }

        // Show preview comments
        comments.forEach { comment ->
            CommentPreviewItem(
                comment = comment,
                onUserClick = onUserClick
            )
        }
    }
}

@Composable
private fun CommentPreviewItem(
    comment: CommunityComment,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // User name (clickable)
        Text(
            text = comment.userDisplayName ?: "User",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.clickable { onUserClick(comment.userId) }
        )
        Spacer(modifier = Modifier.width(6.dp))
        // Comment content
        Text(
            text = comment.content,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASS CARD COMPOSABLE (reusable)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMedium))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkSurface.copy(alpha = 0.95f),
                        DarkSurface
                    )
                )
            )
            .border(
                width = 1.dp,
                color = DarkBorder,
                shape = RoundedCornerShape(RadiusMedium)
            )
    ) {
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// USER AVATAR
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun UserAvatar(
    avatarUrl: String?,
    displayName: String?,
    isCoach: Boolean = false,
    size: Int = 40,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier.clickable { onClick() }
    ) {
        GlowAvatar(
            avatarUrl = avatarUrl,
            name = displayName ?: "?",
            size = size.dp,
            showGlow = isCoach, // Show glow for coaches
            borderWidth = if (size >= 56) 2.dp else 1.5.dp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BADGES
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun CoachBadge() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = PrometheusOrange
    ) {
        Text(
            text = "COACH",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 9.sp
        )
    }
}

@Composable
fun PostTypeBadge(postType: PostType) {
    val (text, color) = when (postType) {
        PostType.TIP -> "TIP" to Color(0xFF4CAF50)
        PostType.TRANSFORMATION -> "TRANSFORMATION" to Color(0xFFFF9800)
        PostType.MOTIVATION -> "MOTIVATION" to Color(0xFF9C27B0)
        PostType.WORKOUT -> "WORKOUT" to Color(0xFF2196F3)
        PostType.GENERAL -> return // Don't show badge for general posts
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MEDIA PREVIEW
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PostMediaPreview(
    imageUrls: List<String>,
    videoUrls: List<String>,
    modifier: Modifier = Modifier
) {
    val allMedia = imageUrls + videoUrls
    if (allMedia.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A2A2A))
    ) {
        // Show first image
        if (imageUrls.isNotEmpty()) {
            AsyncImage(
                model = imageUrls.first(),
                contentDescription = "Post media",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (videoUrls.isNotEmpty()) {
            // Video placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Media count indicator
        if (allMedia.size > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${allMedia.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// EMPTY STATE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun EmptyFeedState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DynamicFeed,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = PrometheusOrange.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No posts yet",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your clients' workout posts will appear here when they share their progress.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════════════════════

fun formatTimeAgo(timestamp: String): String {
    return try {
        val instant = Instant.parse(timestamp)
        val now = Instant.now()
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        val hours = ChronoUnit.HOURS.between(instant, now)
        val days = ChronoUnit.DAYS.between(instant, now)

        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d")
                instant.atZone(ZoneId.systemDefault()).format(formatter)
            }
        }
    } catch (e: Exception) {
        timestamp
    }
}

fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000 -> "${count / 1_000}K"
        else -> count.toString()
    }
}
