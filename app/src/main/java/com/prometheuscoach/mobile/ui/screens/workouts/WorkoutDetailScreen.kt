package com.prometheuscoach.mobile.ui.screens.workouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.prometheuscoach.mobile.data.model.ExerciseListItem
import com.prometheuscoach.mobile.data.model.WorkoutExerciseDetail
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: String,
    onNavigateBack: () -> Unit,
    viewModel: WorkoutDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val pickerState by viewModel.pickerState.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddExerciseSheet by remember { mutableStateOf(false) }
    var showEditWorkoutDialog by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<WorkoutExerciseDetail?>(null) }
    var exerciseToEdit by remember { mutableStateOf<WorkoutExerciseDetail?>(null) }

    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId)
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                state.workout?.name ?: "Workout",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
                            state.workout?.let { workout ->
                                Text(
                                    "${workout.exercises.size} exercise${if (workout.exercises.size != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showEditWorkoutDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Workout", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                if (state.workout != null) {
                    FloatingActionButton(
                        onClick = {
                            viewModel.loadExercisesForPicker()
                            showAddExerciseSheet = true
                        },
                        containerColor = PrometheusOrange
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Exercise", tint = Color.White)
                    }
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
                        Text(state.error!!)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadWorkout(workoutId) }) {
                            Text("Retry")
                        }
                    }
                }
            }

            state.workout != null -> {
                val workout = state.workout!!

                if (workout.exercises.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No exercises yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Add exercises to this workout",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    viewModel.loadExercisesForPicker()
                                    showAddExerciseSheet = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Exercise")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Description
                        if (workout.description != null) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(
                                            elevation = 8.dp,
                                            shape = RoundedCornerShape(12.dp),
                                            ambientColor = PrometheusOrange.copy(alpha = 0.2f),
                                            spotColor = PrometheusOrange.copy(alpha = 0.2f)
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = DarkSurface.copy(alpha = 0.8f)
                                    ),
                                    border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        workout.description,
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // Exercises
                        itemsIndexed(workout.exercises) { index, exercise ->
                            ExerciseCard(
                                exercise = exercise,
                                index = index + 1,
                                onClick = { exerciseToEdit = exercise },
                                onDelete = { exerciseToDelete = exercise }
                            )
                        }

                        // Bottom spacing for FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
        }
    }

    // Add Exercise Bottom Sheet
    if (showAddExerciseSheet) {
        AddExerciseBottomSheet(
            exercises = pickerState.exercises,
            isLoading = pickerState.isLoading,
            searchQuery = pickerState.searchQuery,
            categories = pickerState.categories,
            selectedCategory = pickerState.selectedCategory,
            onSearchQueryChange = { viewModel.loadExercisesForPicker(it) },
            onCategoryChange = { viewModel.setExerciseCategoryFilter(it) },
            onDismiss = {
                showAddExerciseSheet = false
                viewModel.setExerciseCategoryFilter(null) // Reset filter on dismiss
            },
            onSelectExercise = { exerciseItem ->
                scope.launch {
                    viewModel.addExercise(exerciseId = exerciseItem.id)
                        .onSuccess {
                            showAddExerciseSheet = false
                            viewModel.setExerciseCategoryFilter(null) // Reset filter
                        }
                }
            }
        )
    }

    // Edit Workout Dialog
    if (showEditWorkoutDialog && state.workout != null) {
        EditWorkoutDialog(
            currentName = state.workout!!.name,
            currentDescription = state.workout!!.description,
            onDismiss = { showEditWorkoutDialog = false },
            onConfirm = { name, description ->
                scope.launch {
                    viewModel.updateWorkout(name, description)
                        .onSuccess {
                            showEditWorkoutDialog = false
                        }
                }
            },
            isSaving = state.isSaving
        )
    }

    // Delete Exercise Confirmation
    exerciseToDelete?.let { exercise ->
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("Remove Exercise") },
            text = { Text("Remove \"${exercise.exerciseTitle}\" from this workout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.removeExercise(exercise.workoutExerciseId)
                            exerciseToDelete = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Exercise Sets Dialog
    exerciseToEdit?.let { exercise ->
        SetInputDialog(
            exerciseName = exercise.exerciseTitle,
            initialSets = exercise.sets,
            initialRepsMin = exercise.targetReps,
            initialRepsMax = exercise.targetReps,
            initialRestSeconds = 90,
            onDismiss = { exerciseToEdit = null },
            onConfirm = { sets, repsMin, _, _ ->
                scope.launch {
                    viewModel.updateExercise(
                        workoutExerciseId = exercise.workoutExerciseId,
                        sets = sets,
                        targetReps = repsMin
                    ).onSuccess {
                        exerciseToEdit = null
                    }
                }
            },
            isSaving = state.isSaving
        )
    }
}

@Composable
private fun ExerciseCard(
    exercise: WorkoutExerciseDetail,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = PrometheusOrange.copy(alpha = 0.3f),
                spotColor = PrometheusOrange.copy(alpha = 0.3f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.8f)
        ),
        border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index number
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp),
                color = PrometheusOrange.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$index",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrometheusOrange
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Thumbnail
            if (exercise.exerciseThumbnailUrl != null) {
                AsyncImage(
                    model = exercise.exerciseThumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrometheusOrange.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = PrometheusOrange
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Exercise info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.exerciseTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Sets x Reps
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExerciseChip(
                        icon = Icons.Default.Repeat,
                        text = "${exercise.sets} sets"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ExerciseChip(
                        icon = Icons.Default.Speed,
                        text = "${exercise.repsDisplay} reps"
                    )
                }

                if (exercise.exerciseCategory != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        exercise.exerciseCategory,
                        style = MaterialTheme.typography.labelSmall,
                        color = PrometheusOrange
                    )
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ExerciseChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseBottomSheet(
    exercises: List<ExerciseListItem>,
    isLoading: Boolean,
    searchQuery: String,
    categories: List<String>,
    selectedCategory: String?,
    onSearchQueryChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onDismiss: () -> Unit,
    onSelectExercise: (ExerciseListItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Add Exercise",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search exercises...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Category Filter Chips
            if (categories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "All" chip
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { onCategoryChange(null) },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrometheusOrange.copy(alpha = 0.2f),
                                selectedLabelColor = PrometheusOrange
                            )
                        )
                    }

                    // Category chips
                    items(categories.size) { index ->
                        val category = categories[index]
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { onCategoryChange(category) },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrometheusOrange.copy(alpha = 0.2f),
                                selectedLabelColor = PrometheusOrange
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results count
            if (!isLoading && exercises.isNotEmpty()) {
                Text(
                    "${exercises.size} exercise${if (exercises.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrometheusOrange)
                    }
                }

                exercises.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No exercises found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (selectedCategory != null || searchQuery.isNotEmpty()) {
                                Text(
                                    "Try adjusting your filters",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(exercises) { exercise ->
                            ExercisePickerItem(
                                exercise = exercise,
                                onClick = { onSelectExercise(exercise) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ExercisePickerItem(
    exercise: ExerciseListItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        border = BorderStroke(1.dp, PrometheusOrange)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (exercise.thumbnailUrl != null) {
                AsyncImage(
                    model = exercise.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrometheusOrange.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = PrometheusOrange
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                val subtitle = exercise.category ?: exercise.mainMuscleGroup
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = PrometheusOrange
                    )
                }
            }

            Icon(
                Icons.Default.Add,
                contentDescription = "Add",
                tint = PrometheusOrange
            )
        }
    }
}

@Composable
private fun EditWorkoutDialog(
    currentName: String,
    currentDescription: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?) -> Unit,
    isSaving: Boolean
) {
    var name by remember { mutableStateOf(currentName) }
    var description by remember { mutableStateOf(currentDescription ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Workout") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description.ifBlank { null }) },
                enabled = name.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
