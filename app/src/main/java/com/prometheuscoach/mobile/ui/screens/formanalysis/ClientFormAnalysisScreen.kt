package com.prometheuscoach.mobile.ui.screens.formanalysis

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientFormAnalysisScreen(
    clientId: String,
    clientName: String,
    onNavigateBack: () -> Unit,
    viewModel: FormAnalysisViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showUploadDialog by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var showVideoDetail by remember { mutableStateOf(false) }

    // Video picker
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedVideoUri = it
            showUploadDialog = true
        }
    }

    LaunchedEffect(clientId) {
        viewModel.loadClient(clientId, clientName)
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Form Analysis",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                clientName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    },
                    actions = {
                        // Filter button
                        IconButton(onClick = { /* Show filter */ }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = PrometheusOrange
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { videoPicker.launch("video/*") },
                    containerColor = PrometheusOrange,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.VideoCall, contentDescription = "Upload Video")
                }
            }
        ) { paddingValues ->
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrometheusOrange)
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(state.error!!, color = TextSecondary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                state.videos.isEmpty() -> {
                    EmptyFormAnalysisState(
                        modifier = Modifier.padding(paddingValues),
                        onUploadClick = { videoPicker.launch("video/*") }
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Stats row
                        StatsRow(
                            totalVideos = state.videos.size,
                            pendingCount = state.pendingCount,
                            reviewedCount = state.reviewedCount,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        // Video grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.filteredVideos) { video ->
                                VideoThumbnailCard(
                                    video = video,
                                    onClick = {
                                        viewModel.selectVideo(video)
                                        showVideoDetail = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Upload Dialog
        if (showUploadDialog && selectedVideoUri != null) {
            UploadVideoDialog(
                onDismiss = {
                    showUploadDialog = false
                    selectedVideoUri = null
                },
                onUpload = { exerciseName, title, notes ->
                    viewModel.uploadVideo(
                        videoUri = selectedVideoUri!!,
                        exerciseName = exerciseName,
                        title = title,
                        notes = notes
                    )
                    showUploadDialog = false
                    selectedVideoUri = null
                },
                isUploading = state.isUploading,
                uploadProgress = state.uploadProgress
            )
        }

        // Video Detail Sheet
        if (showVideoDetail && state.selectedVideo != null) {
            VideoDetailSheet(
                video = state.selectedVideo!!,
                timestamps = state.timestamps,
                onDismiss = {
                    showVideoDetail = false
                    viewModel.selectVideo(null)
                },
                onAddFeedback = { showFeedbackSheet = true },
                onArchive = { viewModel.archiveVideo(state.selectedVideo!!.id) },
                onDelete = {
                    viewModel.deleteVideo(state.selectedVideo!!.id)
                    showVideoDetail = false
                }
            )
        }

        // Feedback Sheet
        if (showFeedbackSheet && state.selectedVideo != null) {
            FeedbackSheet(
                video = state.selectedVideo!!,
                onDismiss = { showFeedbackSheet = false },
                onSubmit = { feedback, rating ->
                    viewModel.addFeedback(state.selectedVideo!!.id, feedback, rating)
                    showFeedbackSheet = false
                }
            )
        }
    }
}

@Composable
private fun StatsRow(
    totalVideos: Int,
    pendingCount: Int,
    reviewedCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatChip(
            label = "Total",
            count = totalVideos,
            color = PrometheusOrange,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Pending",
            count = pendingCount,
            color = Color(0xFFFFC107),
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Reviewed",
            count = reviewedCount,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun VideoThumbnailCard(
    video: FormAnalysisVideo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = PrometheusOrange.copy(alpha = 0.1f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
        border = BorderStroke(
            1.dp,
            if (video.isPending) Color(0xFFFFC107).copy(alpha = 0.5f)
            else PrometheusOrange.copy(alpha = 0.3f)
        )
    ) {
        Column {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (video.thumbnailUrl != null) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = PrometheusOrange.copy(alpha = 0.5f)
                    )
                }

                // Status badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (video.status) {
                                AnalysisStatus.PENDING -> Color(0xFFFFC107)
                                AnalysisStatus.REVIEWED -> Color(0xFF4CAF50)
                                AnalysisStatus.ARCHIVED -> Color.Gray
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        video.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }

                // Duration badge
                if (video.formattedDuration.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            video.formattedDuration,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            // Info
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    video.displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    video.uploadedAt.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                if (video.rating != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        repeat(5) { index ->
                            Icon(
                                if (index < video.rating) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (index < video.rating) PrometheusOrange else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadVideoDialog(
    onDismiss: () -> Unit,
    onUpload: (exerciseName: String?, title: String?, notes: String?) -> Unit,
    isUploading: Boolean,
    uploadProgress: Float
) {
    var exerciseName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("Upload Video") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isUploading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            progress = { uploadProgress },
                            color = PrometheusOrange
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Uploading... ${(uploadProgress * 100).toInt()}%",
                            color = TextSecondary
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = exerciseName,
                        onValueChange = { exerciseName = it },
                        label = { Text("Exercise Name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpload(
                        exerciseName.ifBlank { null },
                        title.ifBlank { null },
                        notes.ifBlank { null }
                    )
                },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
            ) {
                Text("Upload")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoDetailSheet(
    video: FormAnalysisVideo,
    timestamps: List<VideoTimestamp>,
    onDismiss: () -> Unit,
    onAddFeedback: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Video player placeholder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        modifier = Modifier.size(64.dp),
                        tint = PrometheusOrange
                    )
                    // Note: Full video player would use ExoPlayer
                    Text(
                        "Tap to play video",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Info
            Text(
                video.displayTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Uploaded: ${video.uploadedAt.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (video.formattedFileSize.isNotEmpty()) {
                    Text(
                        video.formattedFileSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Rating
            if (video.rating != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text("Rating: ", color = TextSecondary)
                    repeat(5) { index ->
                        Icon(
                            if (index < video.rating) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (index < video.rating) PrometheusOrange else TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(video.ratingDescription, color = PrometheusOrange)
                }
            }

            // Notes
            video.notes?.let { notes ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PrometheusOrange.copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Notes",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrometheusOrange
                        )
                        Text(notes, color = TextPrimary)
                    }
                }
            }

            // Feedback
            video.feedback?.let { feedback ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Coach Feedback",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(feedback, color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (video.isPending) {
                    Button(
                        onClick = onAddFeedback,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
                    ) {
                        Icon(Icons.Default.RateReview, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Feedback")
                    }
                }

                OutlinedButton(
                    onClick = onArchive,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null, tint = PrometheusOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Archive", color = PrometheusOrange)
                }
            }

            TextButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Video", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Video?") },
            text = { Text("This will permanently delete the video and cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackSheet(
    video: FormAnalysisVideo,
    onDismiss: () -> Unit,
    onSubmit: (feedback: String, rating: Int?) -> Unit
) {
    var feedback by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Add Feedback",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                video.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Rating
            Text(
                "Form Rating",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) { index ->
                    IconButton(
                        onClick = { rating = index + 1 }
                    ) {
                        Icon(
                            if (index < rating) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Rate ${index + 1}",
                            modifier = Modifier.size(32.dp),
                            tint = if (index < rating) PrometheusOrange else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Feedback text
            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it },
                label = { Text("Feedback") },
                placeholder = { Text("Describe form improvements, cues, and observations...") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = PrometheusOrange.copy(alpha = 0.5f),
                    focusedBorderColor = PrometheusOrange
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSubmit(feedback, if (rating > 0) rating else null)
                },
                enabled = feedback.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
            ) {
                Text("Submit Feedback")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EmptyFormAnalysisState(
    modifier: Modifier = Modifier,
    onUploadClick: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = PrometheusOrange.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Videos Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Upload videos of this client's form to review and provide feedback.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onUploadClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
            ) {
                Icon(Icons.Default.VideoCall, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload First Video")
            }
        }
    }
}
