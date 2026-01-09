package com.prometheuscoach.mobile.ui.screens.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.prometheuscoach.mobile.data.model.AssignedWorkout
import com.prometheuscoach.mobile.ui.components.GlowAvatarLarge
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToWorkouts: () -> Unit = {},
    onNavigateToWorkoutDetail: (workoutId: String) -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onNavigateToNutrition: (clientId: String, clientName: String) -> Unit = { _, _ -> },
    onNavigateToVBT: (clientId: String, clientName: String) -> Unit = { _, _ -> },
    onNavigateToFormAnalysis: (clientId: String, clientName: String) -> Unit = { _, _ -> },
    onNavigateToAI: (clientId: String, clientName: String) -> Unit = { _, _ -> },
    onNavigateToGamification: (clientId: String, clientName: String) -> Unit = { _, _ -> },
    viewModel: ClientDetailViewModel = hiltViewModel()
) {
    val detailState by viewModel.detailState.collectAsState()
    val scope = rememberCoroutineScope()
    var showAssignWorkoutSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showEditAssignmentSheet by remember { mutableStateOf(false) }
    var selectedAssignment by remember { mutableStateOf<AssignedWorkout?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(clientId) {
        viewModel.loadClient(clientId)
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Client Details",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
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
                        IconButton(onClick = { showEditSheet = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = PrometheusOrange
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
        when {
            detailState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrometheusOrange)
                }
            }

            detailState.error != null -> {
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
                        Text(detailState.error!!)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadClient(clientId) }) {
                            Text("Retry")
                        }
                    }
                }
            }

            detailState.client != null -> {
                val client = detailState.client!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar with glow ring
                    Log.d("ClientDetail", "Client ${client.fullName} avatarUrl: ${client.avatarUrl}")
                    GlowAvatarLarge(
                        avatarUrl = client.avatarUrl,
                        name = client.fullName
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        client.fullName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        "Client",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Quick Actions - First Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickActionButton(
                            icon = Icons.Default.Add,
                            label = "Assign",
                            onClick = {
                                viewModel.loadAvailableWorkouts()
                                showAssignWorkoutSheet = true
                            }
                        )
                        QuickActionButton(
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            label = "Progress",
                            onClick = onNavigateToProgress
                        )
                        QuickActionButton(
                            icon = Icons.Default.Speed,
                            label = "VBT",
                            onClick = { onNavigateToVBT(clientId, client.fullName) }
                        )
                        QuickActionButton(
                            icon = Icons.AutoMirrored.Filled.Message,
                            label = "Message",
                            onClick = {
                                scope.launch {
                                    viewModel.startConversation(clientId)
                                        .onSuccess { conversationId ->
                                            onNavigateToChat(conversationId)
                                        }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Actions - Second Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickActionButton(
                            icon = Icons.Default.Restaurant,
                            label = "Nutrition",
                            onClick = { onNavigateToNutrition(clientId, client.fullName) }
                        )
                        QuickActionButton(
                            icon = Icons.Default.VideoLibrary,
                            label = "Form",
                            onClick = { onNavigateToFormAnalysis(clientId, client.fullName) }
                        )
                        QuickActionButton(
                            icon = Icons.Default.AutoAwesome,
                            label = "AI",
                            onClick = { onNavigateToAI(clientId, client.fullName) }
                        )
                        QuickActionButton(
                            icon = Icons.Default.EmojiEvents,
                            label = "Stats",
                            onClick = { onNavigateToGamification(clientId, client.fullName) }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Assigned Workouts Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = PrometheusOrange.copy(alpha = 0.1f),
                                spotColor = PrometheusOrange.copy(alpha = 0.15f)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkSurface.copy(alpha = 0.85f)
                        ),
                        border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Assigned Workouts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                TextButton(
                                    onClick = {
                                        viewModel.loadAvailableWorkouts()
                                        showAssignWorkoutSheet = true
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = PrometheusOrange
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Assign", color = PrometheusOrange)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (detailState.assignedWorkouts.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.FitnessCenter,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = PrometheusOrange.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "No workouts assigned",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            } else {
                                detailState.assignedWorkouts.forEach { assignment ->
                                    AssignedWorkoutCard(
                                        assignment = assignment,
                                        onClick = {
                                            selectedAssignment = assignment
                                            showEditAssignmentSheet = true
                                        },
                                        onRemove = {
                                            scope.launch {
                                                viewModel.removeAssignment(assignment.assignmentId)
                                                    .onSuccess {
                                                        snackbarHostState.showSnackbar("Workout removed")
                                                    }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Info Cards
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = PrometheusOrange.copy(alpha = 0.1f),
                                spotColor = PrometheusOrange.copy(alpha = 0.15f)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkSurface.copy(alpha = 0.85f)
                        ),
                        border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Client Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            InfoRow(label = "Name", value = client.fullName)
                            InfoRow(label = "Member since", value = client.createdAt?.take(10) ?: "-")
                            InfoRow(label = "Role", value = client.role)
                        }
                    }
                }

                // Assign Workout Sheet
                if (showAssignWorkoutSheet) {
                    AssignWorkoutSheet(
                        clientName = client.fullName,
                        workouts = detailState.availableWorkouts,
                        isLoading = detailState.isLoadingWorkouts,
                        isAssigning = detailState.isAssigning,
                        error = detailState.workoutsError,
                        onDismiss = { showAssignWorkoutSheet = false },
                        onAssign = { workoutId, notes ->
                            scope.launch {
                                viewModel.assignWorkout(workoutId, notes)
                                    .onSuccess {
                                        showAssignWorkoutSheet = false
                                        snackbarHostState.showSnackbar("Workout assigned successfully")
                                    }
                                    .onFailure { e ->
                                        snackbarHostState.showSnackbar("Failed: ${e.message}")
                                    }
                            }
                        },
                        onRetry = { viewModel.loadAvailableWorkouts() },
                        onCreateWorkout = onNavigateToWorkouts
                    )
                }

                // Edit Client Sheet
                if (showEditSheet) {
                    EditClientSheet(
                        clientName = client.fullName,
                        currentTimezone = client.preferredTimezone,
                        isSaving = detailState.isSavingClient,
                        error = detailState.clientSaveError,
                        onDismiss = {
                            showEditSheet = false
                            viewModel.clearClientSaveError()
                        },
                        onSave = { name, timezone ->
                            scope.launch {
                                viewModel.updateClient(name, timezone)
                                // Wait for save to complete and close if successful
                                kotlinx.coroutines.delay(500)
                                if (detailState.clientSaveError == null && !detailState.isSavingClient) {
                                    showEditSheet = false
                                    snackbarHostState.showSnackbar("Client updated successfully")
                                }
                            }
                        }
                    )
                }

                // Edit Assignment Sheet
                if (showEditAssignmentSheet && selectedAssignment != null) {
                    EditAssignedWorkoutSheet(
                        assignment = selectedAssignment!!,
                        isUpdating = detailState.isUpdatingAssignment,
                        error = detailState.assignmentUpdateError,
                        onDismiss = {
                            showEditAssignmentSheet = false
                            selectedAssignment = null
                            viewModel.clearAssignmentUpdateError()
                        },
                        onSave = { notes, scheduledDate, status, exerciseSets ->
                            scope.launch {
                                viewModel.updateAssignmentWithSets(
                                    assignmentId = selectedAssignment!!.assignmentId,
                                    notes = notes,
                                    scheduledDate = scheduledDate,
                                    status = status,
                                    exerciseSets = exerciseSets
                                ).onSuccess {
                                    showEditAssignmentSheet = false
                                    selectedAssignment = null
                                    snackbarHostState.showSnackbar("Workout updated successfully")
                                }
                            }
                        },
                        onEditWorkout = { workoutId ->
                            showEditAssignmentSheet = false
                            selectedAssignment = null
                            onNavigateToWorkoutDetail(workoutId)
                        }
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = PrometheusOrange.copy(alpha = 0.3f),
                    spotColor = PrometheusOrange.copy(alpha = 0.3f)
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DarkSurfaceVariant,
                            DarkSurface
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = PrometheusOrange.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = PrometheusOrange
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
private fun AssignedWorkoutCard(
    assignment: AssignedWorkout,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Status colors
    val statusColor = when (assignment.status) {
        "completed" -> Color(0xFF4CAF50) // Green
        "cancelled" -> Color.Gray
        else -> PrometheusOrange // active
    }
    val statusText = when (assignment.status) {
        "completed" -> "Completed"
        "cancelled" -> "Cancelled"
        else -> "Active"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrometheusOrange.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Workout icon
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = PrometheusOrange,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = assignment.workoutName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${assignment.exerciseCount} exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    if (assignment.scheduledDate != null) {
                        Text(
                            text = "Scheduled: ${assignment.scheduledDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrometheusOrange
                        )
                    }
                }
                if (assignment.notes != null) {
                    Text(
                        text = assignment.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
            }

            // Remove button
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Workout") },
            text = { Text("Remove \"${assignment.workoutName}\" from this client?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onRemove()
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
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

// Avatar component moved to GlowAvatar.kt in ui/components
