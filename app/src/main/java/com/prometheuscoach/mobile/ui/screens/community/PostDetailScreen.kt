package com.prometheuscoach.mobile.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prometheuscoach.mobile.data.model.CommunityComment
import com.prometheuscoach.mobile.data.model.FeedPost
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val postDetailState by viewModel.postDetailState.collectAsState()
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(postId) {
        viewModel.loadPostDetail(postId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearPostDetail()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Comment input
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                color = Color(0xFF1A1A1A),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Write a comment...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedBorderColor = PrometheusOrange
                        ),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                viewModel.addComment(postId, commentText)
                                commentText = ""
                            }
                        },
                        enabled = commentText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (commentText.isNotBlank()) PrometheusOrange else Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            postDetailState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrometheusOrange)
                }
            }
            postDetailState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = postDetailState.error ?: "Error loading post",
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadPostDetail(postId) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            postDetailState.post != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Post content
                    item {
                        PostDetailContent(
                            post = postDetailState.post!!,
                            onUserClick = onNavigateToUserProfile,
                            onLikeClick = { viewModel.toggleLike(postId) },
                            isCurrentUser = viewModel.isCurrentUser(postDetailState.post!!.userId)
                        )
                    }

                    // Comments header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Comments",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${postDetailState.comments.size})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Loading comments indicator
                    if (postDetailState.isLoadingComments) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = PrometheusOrange,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    // Comments list
                    items(
                        items = postDetailState.comments,
                        key = { it.id }
                    ) { comment ->
                        CommentItem(
                            comment = comment,
                            onUserClick = onNavigateToUserProfile
                        )
                    }

                    // Empty comments state
                    if (postDetailState.comments.isEmpty() && !postDetailState.isLoadingComments) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No comments yet. Be the first to comment!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostDetailContent(
    post: FeedPost,
    onUserClick: (String) -> Unit,
    onLikeClick: () -> Unit,
    isCurrentUser: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // User header
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    avatarUrl = post.userAvatarUrl,
                    displayName = post.userDisplayName,
                    isCoach = post.isCoach,
                    size = 48,
                    onClick = { onUserClick(post.userId) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.userDisplayName ?: "User",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        if (post.isCoach) {
                            Spacer(modifier = Modifier.width(8.dp))
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            post.title?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Workout stats
            if (post.totalVolumeKg != null || post.totalSets != null || post.totalReps != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    post.totalVolumeKg?.let {
                        WorkoutStat(label = "Volume", value = "${it}kg")
                    }
                    post.totalSets?.let {
                        WorkoutStat(label = "Sets", value = it.toString())
                    }
                    post.totalReps?.let {
                        WorkoutStat(label = "Reps", value = it.toString())
                    }
                    post.durationMinutes?.let {
                        if (it > 0) WorkoutStat(label = "Duration", value = "${it}min")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Content/Caption
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Media
            if (post.hasMedia) {
                PostMediaPreview(
                    imageUrls = post.imageUrls ?: emptyList(),
                    videoUrls = post.videoUrls ?: emptyList()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Engagement row
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Like button
                IconButton(onClick = onLikeClick) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLiked) Color.Red else Color.White.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = "${post.likesCount} likes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(24.dp))
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${post.commentsCount} comments",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun WorkoutStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrometheusOrange
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun CommentItem(
    comment: CommunityComment,
    onUserClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        UserAvatar(
            avatarUrl = comment.userAvatarUrl,
            displayName = comment.userDisplayName,
            isCoach = comment.isCoach,
            size = 36,
            onClick = { onUserClick(comment.userId) }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.userDisplayName ?: "User",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                if (comment.isCoach) {
                    Spacer(modifier = Modifier.width(6.dp))
                    CoachBadge()
                }
                Spacer(modifier = Modifier.width(8.dp))
                comment.createdAt?.let {
                    Text(
                        text = formatTimeAgo(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}
