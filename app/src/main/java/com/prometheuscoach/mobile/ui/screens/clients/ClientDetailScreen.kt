package com.prometheuscoach.mobile.ui.screens.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.prometheuscoach.mobile.data.model.AssignedWorkout
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToWorkouts: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onNavigateToNutrition: (clientId: String, clientName: String) -> Unit = { _, _ -> },
    viewModel: ClientDetailViewModel = hiltViewModel()
) {
    val detailState by viewModel.detailState.collectAsState()
    val scope = rememberCoroutineScope()
    var showAssignWorkoutSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(clientId) {
        viewModel.loadClient(clientId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Client Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Edit */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
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
                    // Avatar
                    if (client.avatarUrl != null) {
                        AsyncImage(
                            model = client.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(PrometheusOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = client.fullName.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrometheusOrange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        client.fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Client",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Quick Actions
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
                            icon = Icons.Default.Restaurant,
                            label = "Nutrition",
                            onClick = { onNavigateToNutrition(clientId, client.fullName) }
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Assigned Workouts Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black
                        ),
                        border = BorderStroke(1.dp, PrometheusOrange)
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black
                        ),
                        border = BorderStroke(1.dp, PrometheusOrange)
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
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(icon, contentDescription = null)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    onRemove: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = assignment.routineName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
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
            text = { Text("Remove \"${assignment.routineName}\" from this client?") },
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
