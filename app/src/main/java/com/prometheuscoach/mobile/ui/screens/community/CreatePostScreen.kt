package com.prometheuscoach.mobile.ui.screens.community
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prometheuscoach.mobile.data.model.PostType
import com.prometheuscoach.mobile.data.model.PostVisibility
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onNavigateBack: () -> Unit,
    onPostCreated: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val createPostState by viewModel.createPostState.collectAsState()
    val context = LocalContext.current

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        uris.forEach { uri ->
            viewModel.addSelectedImage(uri.toString())
        }
    }

    // Video picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.addSelectedVideo(it.toString())
        }
    }

    // Handle success
    LaunchedEffect(createPostState.isSuccess) {
        if (createPostState.isSuccess) {
            viewModel.resetCreatePostState()
            onPostCreated()
        }
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Create Post", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        Button(
                            onClick = { viewModel.createPost() },
                            enabled = createPostState.content.isNotBlank() && !createPostState.isPosting,
                            colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            if (createPostState.isPosting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Post")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Post Type Selection
            Text(
                text = "Post Type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PostTypeChip(
                    type = PostType.TIP,
                    label = "Tip",
                    icon = Icons.Default.Lightbulb,
                    selected = createPostState.postType == PostType.TIP,
                    onClick = { viewModel.updateCreatePostType(PostType.TIP) },
                    modifier = Modifier.weight(1f)
                )
                PostTypeChip(
                    type = PostType.TRANSFORMATION,
                    label = "Transform",
                    icon = Icons.Default.TrendingUp,
                    selected = createPostState.postType == PostType.TRANSFORMATION,
                    onClick = { viewModel.updateCreatePostType(PostType.TRANSFORMATION) },
                    modifier = Modifier.weight(1f)
                )
                PostTypeChip(
                    type = PostType.MOTIVATION,
                    label = "Motivate",
                    icon = Icons.Default.EmojiEvents,
                    selected = createPostState.postType == PostType.MOTIVATION,
                    onClick = { viewModel.updateCreatePostType(PostType.MOTIVATION) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PostTypeChip(
                    type = PostType.WORKOUT,
                    label = "Workout",
                    icon = Icons.Default.FitnessCenter,
                    selected = createPostState.postType == PostType.WORKOUT,
                    onClick = { viewModel.updateCreatePostType(PostType.WORKOUT) },
                    modifier = Modifier.weight(1f)
                )
                PostTypeChip(
                    type = PostType.GENERAL,
                    label = "General",
                    icon = Icons.Default.Article,
                    selected = createPostState.postType == PostType.GENERAL,
                    onClick = { viewModel.updateCreatePostType(PostType.GENERAL) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            // Title (optional)
            OutlinedTextField(
                value = createPostState.title,
                onValueChange = { viewModel.updateCreatePostTitle(it) },
                label = { Text("Title (optional)") },
                placeholder = { Text("Add a catchy title...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrometheusOrange,
                    focusedLabelColor = PrometheusOrange,
                    cursorColor = PrometheusOrange
                )
            )

            // Content
            OutlinedTextField(
                value = createPostState.content,
                onValueChange = { viewModel.updateCreatePostContent(it) },
                label = { Text("What's on your mind?") },
                placeholder = { Text("Share your expertise, tips, or client success stories...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp),
                minLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrometheusOrange,
                    focusedLabelColor = PrometheusOrange,
                    cursorColor = PrometheusOrange
                )
            )

            // Media selection section
            Text(
                text = "Media",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            // Media action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add photos button
                OutlinedButton(
                    onClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrometheusOrange
                    ),
                    border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Add photos",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Photos")
                }

                // Add video button
                OutlinedButton(
                    onClick = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrometheusOrange
                    ),
                    border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Add video",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Video")
                }
            }

            // Display selected images
            if (createPostState.selectedImageUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(createPostState.selectedImageUris) { uri ->
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(Uri.parse(uri))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Selected image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Remove button
                            IconButton(
                                onClick = { viewModel.removeSelectedImage(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove image",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Display selected videos
            if (createPostState.selectedVideoUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(createPostState.selectedVideoUris) { uri ->
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2A2A2A))
                        ) {
                            // Video thumbnail placeholder
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoFile,
                                    contentDescription = "Video",
                                    tint = PrometheusOrange,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Video",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            // Remove button
                            IconButton(
                                onClick = { viewModel.removeSelectedVideo(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove video",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Upload progress indicator
            if (createPostState.isUploadingMedia) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2A2A2A)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Uploading media...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { createPostState.uploadProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = PrometheusOrange
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(createPostState.uploadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Visibility
            Text(
                text = "Visibility",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisibilityChip(
                    visibility = PostVisibility.PUBLIC,
                    label = "Public",
                    icon = Icons.Default.Public,
                    selected = createPostState.visibility == PostVisibility.PUBLIC,
                    onClick = { viewModel.updateCreatePostVisibility(PostVisibility.PUBLIC) },
                    modifier = Modifier.weight(1f)
                )
                VisibilityChip(
                    visibility = PostVisibility.FOLLOWERS,
                    label = "Followers",
                    icon = Icons.Default.People,
                    selected = createPostState.visibility == PostVisibility.FOLLOWERS,
                    onClick = { viewModel.updateCreatePostVisibility(PostVisibility.FOLLOWERS) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Error message
            if (createPostState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = createPostState.error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Tips section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PrometheusOrange.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TipsAndUpdates,
                            contentDescription = null,
                            tint = PrometheusOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tips for great posts",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PrometheusOrange
                        )
                    }
                    Text(
                        text = "• Share actionable tips that help others\n• Include before/after for transformations\n• Be authentic and share your expertise\n• Engage with comments to build community",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
        }
    }
}

@Composable
private fun PostTypeChip(
    type: PostType,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = when (type) {
        PostType.TIP -> Color(0xFF4CAF50)
        PostType.TRANSFORMATION -> Color(0xFFFF9800)
        PostType.MOTIVATION -> Color(0xFF9C27B0)
        PostType.WORKOUT -> Color(0xFF2196F3)
        PostType.GENERAL -> Color.Gray
    }

    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) color.copy(alpha = 0.2f) else Color(0xFF2A2A2A),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) color else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) color else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) color else Color.White.copy(alpha = 0.7f),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun VisibilityChip(
    visibility: PostVisibility,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) PrometheusOrange.copy(alpha = 0.2f) else Color(0xFF2A2A2A),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) PrometheusOrange else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) PrometheusOrange else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) PrometheusOrange else Color.White.copy(alpha = 0.7f),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
