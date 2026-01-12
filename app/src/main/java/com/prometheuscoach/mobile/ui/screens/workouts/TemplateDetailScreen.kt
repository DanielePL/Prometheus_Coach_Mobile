package com.prometheuscoach.mobile.ui.screens.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailScreen(
    templateId: String,
    onNavigateBack: () -> Unit,
    onNavigateToWorkoutDetail: (String) -> Unit,
    viewModel: TemplateDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showCloneDialog by remember { mutableStateOf(false) }

    // Handle successful clone - navigate to workout
    LaunchedEffect(state.clonedWorkoutId) {
        state.clonedWorkoutId?.let { workoutId ->
            viewModel.clearClonedWorkout()
            onNavigateToWorkoutDetail(workoutId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.template?.template?.name ?: "Template",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Favorite toggle
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (state.template?.isFavorite == true)
                                Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (state.template?.isFavorite == true)
                                Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A)
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrometheusOrange)
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            state.error ?: "Unknown error",
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadTemplate() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            state.template != null -> {
                val template = state.template!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Template Header
                    item {
                        TemplateHeaderCard(
                            template = template,
                            selectedLevel = state.selectedLevel,
                            onLevelSelected = { viewModel.selectLevel(it) }
                        )
                    }

                    // Scaling Slider
                    item {
                        ScalingCard(
                            scalingPercentage = state.scalingPercentage,
                            onScalingChanged = { viewModel.setScalingPercentage(it) }
                        )
                    }

                    // Exercises Section Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${template.exerciseCount} Übungen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Level: ${state.selectedLevel.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    // Exercise List
                    itemsIndexed(template.exercises) { index, exercise ->
                        TemplateExerciseCard(
                            index = index + 1,
                            exercise = exercise,
                            selectedLevel = state.selectedLevel,
                            scalingPercentage = state.scalingPercentage
                        )
                    }

                    // Bottom Spacer for FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }

                // Clone FAB
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { showCloneDialog = true },
                        containerColor = PrometheusOrange,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.ContentCopy, "Clone")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Template verwenden")
                    }
                }
            }
        }
    }

    // Clone Dialog
    if (showCloneDialog) {
        CloneConfirmDialog(
            templateName = state.template?.template?.name ?: "",
            selectedLevel = state.selectedLevel,
            scalingPercentage = state.scalingPercentage,
            isCloning = state.isCloning,
            error = state.cloneError,
            onConfirm = { customName ->
                scope.launch {
                    viewModel.cloneToWorkout(customName)
                }
            },
            onDismiss = { showCloneDialog = false }
        )
    }
}

@Composable
private fun TemplateHeaderCard(
    template: TemplateWithExercises,
    selectedLevel: FitnessLevel,
    onLevelSelected: (FitnessLevel) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Type & Category badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Template Type Badge
                Surface(
                    color = when (template.template.type) {
                        TemplateType.SYSTEM -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        TemplateType.AI -> Color(0xFF9C27B0).copy(alpha = 0.2f)
                        TemplateType.COACH -> PrometheusOrange.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        when (template.template.type) {
                            TemplateType.SYSTEM -> "System"
                            TemplateType.AI -> "KI"
                            TemplateType.COACH -> "Eigenes"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (template.template.type) {
                            TemplateType.SYSTEM -> Color(0xFF4CAF50)
                            TemplateType.AI -> Color(0xFF9C27B0)
                            TemplateType.COACH -> PrometheusOrange
                        }
                    )
                }

                // Category Badge
                template.category?.let { category ->
                    Surface(
                        color = Color.Gray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            category.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Description
            template.template.description?.let { desc ->
                Text(
                    desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // Target Muscles
            template.template.targetMuscles?.takeIf { it.isNotEmpty() }?.let { muscles ->
                val scrollState = rememberScrollState()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(scrollState)
                ) {
                    muscles.forEach { muscle ->
                        Surface(
                            color = PrometheusOrange.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                muscle.replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = PrometheusOrange
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

            // Level Selector
            Text(
                "Schwierigkeitsgrad",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FitnessLevel.entries.forEach { level ->
                    val isSelected = level == selectedLevel
                    FilterChip(
                        selected = isSelected,
                        onClick = { onLevelSelected(level) },
                        label = { Text(level.displayName) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrometheusOrange,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF2A2A2A),
                            labelColor = Color.Gray
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ScalingCard(
    scalingPercentage: Int,
    onScalingChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Volumen-Skalierung",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Text(
                    "$scalingPercentage%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrometheusOrange
                )
            }

            Slider(
                value = scalingPercentage.toFloat(),
                onValueChange = { onScalingChanged(it.toInt()) },
                valueRange = 50f..100f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = PrometheusOrange,
                    activeTrackColor = PrometheusOrange,
                    inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                )
            )

            Text(
                when {
                    scalingPercentage >= 90 -> "Volles Volumen - Für erfahrene Athleten"
                    scalingPercentage >= 70 -> "Reduziertes Volumen - Guter Einstieg"
                    else -> "Minimales Volumen - Für Anfänger/Rehab"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun TemplateExerciseCard(
    index: Int,
    exercise: TemplateExerciseDetail,
    selectedLevel: FitnessLevel,
    scalingPercentage: Int
) {
    val scaledSets = ((exercise.getSetsForLevel(selectedLevel) * scalingPercentage) / 100f)
        .toInt()
        .coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index Circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrometheusOrange.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$index",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrometheusOrange
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Exercise Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.exerciseName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    exercise.primaryMuscle?.let { muscle ->
                        Text(
                            muscle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Sets x Reps
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "$scaledSets Sets",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrometheusOrange
                )
                Text(
                    exercise.getRepsDisplay(selectedLevel) + " Reps",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        // Notes if present
        exercise.notes?.let { notes ->
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                color = Color.Gray.copy(alpha = 0.2f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Notes,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun CloneConfirmDialog(
    templateName: String,
    selectedLevel: FitnessLevel,
    scalingPercentage: Int,
    isCloning: Boolean,
    error: String?,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var customName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isCloning) onDismiss() },
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text("Workout erstellen", color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Erstelle ein neues Workout basierend auf \"$templateName\"",
                    color = Color.Gray
                )

                // Summary
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Level:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            Text(selectedLevel.displayName, color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Skalierung:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            Text("$scalingPercentage%", color = PrometheusOrange, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Custom Name Field
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Workout-Name (optional)") },
                    placeholder = { Text(templateName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrometheusOrange,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = PrometheusOrange,
                        cursorColor = PrometheusOrange,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Error message
                error?.let {
                    Text(
                        it,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(customName.takeIf { it.isNotBlank() }) },
                enabled = !isCloning,
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
            ) {
                if (isCloning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Erstellen")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCloning
            ) {
                Text("Abbrechen", color = Color.Gray)
            }
        }
    )
}
