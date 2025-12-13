package com.prometheuscoach.mobile.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prometheuscoach.mobile.data.model.ExerciseListItem
import com.prometheuscoach.mobile.data.model.RoutineSummary
import com.prometheuscoach.mobile.ui.theme.*
import kotlinx.coroutines.launch

// Sport categories for filtering
enum class SportCategory(val displayName: String) {
    ALL("All"),
    STRENGTH("Strength"),
    OLYMPIC("Olympic Lifting"),
    POWERLIFTING("Powerlifting"),
    BODYBUILDING("Bodybuilding"),
    CROSSFIT("CrossFit"),
    CALISTHENICS("Calisthenics"),
    CARDIO("Cardio"),
    MOBILITY("Mobility")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWorkoutDetail: (String) -> Unit,
    onNavigateToExerciseDetail: ((String) -> Unit)? = null,
    onNavigateToCreateProgram: (() -> Unit)? = null,
    initialTab: Int = 0,
    selectionMode: String? = null,
    onItemSelected: ((String, String) -> Unit)? = null, // (type, id)
    onCancelSelection: (() -> Unit)? = null,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Main tabs: Library | Programs | Workouts | My Exercises
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf("Library", "Programs", "Workouts", "My Exercises")

    // Sub-tabs
    var exerciseSubTab by remember { mutableIntStateOf(0) } // 0 = All, 1 = My Sports
    var programSubTab by remember { mutableIntStateOf(0) }  // 0 = My Programs, 1 = Templates
    var workoutSubTab by remember { mutableIntStateOf(0) }  // 0 = My Workouts, 1 = Templates

    // Filters
    var selectedSport by remember { mutableStateOf(SportCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTemplateCategory by remember { mutableStateOf("All") }

    // Dialogs
    var showCreateSheet by remember { mutableStateOf(false) }
    var showCreateWorkoutDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val isSelectionMode = selectionMode != null

    // Load data when tab changes
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> viewModel.loadExercises()
            1 -> viewModel.loadPrograms()
            2 -> viewModel.loadWorkouts()
            3 -> viewModel.loadMyExercises()
        }
    }

    // Filter exercises
    val filteredExercises = remember(state.exercises, searchQuery, selectedSport, exerciseSubTab) {
        var exercises = state.exercises

        // Apply sport filter (only for "All Exercises" sub-tab)
        if (exerciseSubTab == 0 && selectedSport != SportCategory.ALL) {
            exercises = exercises.filter { exercise ->
                exercise.sports?.contains(selectedSport.displayName) == true
            }
        }

        // Apply search
        if (searchQuery.isNotBlank()) {
            exercises = exercises.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }

        exercises
    }

    // Filter workouts
    val filteredWorkouts = remember(state.workouts, searchQuery) {
        if (searchQuery.isBlank()) {
            state.workouts
        } else {
            state.workouts.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBackground, DarkBackgroundSecondary)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            if (isSelectionMode) {
                SelectionModeHeader(
                    selectionMode = selectionMode!!,
                    onCancelClick = { onCancelSelection?.invoke() }
                )
            } else {
                LibraryHeader(onNavigateBack = onNavigateBack)
            }

            // Main Tabs
            LibraryTabs(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // Search Bar
            LibrarySearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = {
                    searchQuery = it
                    viewModel.setSearchQuery(it)
                }
            )

            // Sub-Tabs based on main tab
            when (selectedTab) {
                0 -> {
                    // Exercise Sub-Tabs
                    ExerciseSubTabs(
                        selectedSubTab = exerciseSubTab,
                        onSubTabSelected = { exerciseSubTab = it }
                    )
                    // Sport Filter (only for "All Exercises")
                    if (exerciseSubTab == 0) {
                        SportFilterChips(
                            selectedSport = selectedSport,
                            onSportSelected = { selectedSport = it }
                        )
                    }
                }
                1 -> {
                    // Program Sub-Tabs
                    ProgramSubTabs(
                        selectedSubTab = programSubTab,
                        onSubTabSelected = { programSubTab = it }
                    )
                }
                2 -> {
                    // Workout Sub-Tabs
                    WorkoutSubTabs(
                        selectedSubTab = workoutSubTab,
                        onSubTabSelected = { workoutSubTab = it }
                    )
                    // Template Category Filter
                    if (workoutSubTab == 1) {
                        WorkoutCategoryFilterChips(
                            selectedCategory = selectedTemplateCategory,
                            onCategorySelected = { selectedTemplateCategory = it }
                        )
                    }
                }
            }

            // Content
            when (selectedTab) {
                0 -> ExercisesGrid(
                    exercises = filteredExercises,
                    isLoading = state.isLoading,
                    error = state.error,
                    isSelectionMode = isSelectionMode,
                    onExerciseClick = { exercise ->
                        if (isSelectionMode) {
                            onItemSelected?.invoke("exercise", exercise.id)
                        } else {
                            onNavigateToExerciseDetail?.invoke(exercise.id)
                        }
                    },
                    onRetry = { viewModel.loadExercises() }
                )
                1 -> ProgramsContent(
                    subTab = programSubTab,
                    isLoading = state.isLoading,
                    error = state.error,
                    isSelectionMode = isSelectionMode,
                    onProgramClick = { programId ->
                        if (isSelectionMode) {
                            onItemSelected?.invoke("program", programId)
                        }
                    }
                )
                2 -> WorkoutsContent(
                    subTab = workoutSubTab,
                    workouts = filteredWorkouts,
                    isLoading = state.isLoading,
                    error = state.error,
                    searchQuery = searchQuery,
                    isSelectionMode = isSelectionMode,
                    onWorkoutClick = { workoutId ->
                        if (isSelectionMode) {
                            onItemSelected?.invoke("workout", workoutId)
                        } else {
                            onNavigateToWorkoutDetail(workoutId)
                        }
                    },
                    onRetry = { viewModel.loadWorkouts() },
                    onCreateClick = { showCreateWorkoutDialog = true }
                )
                3 -> MyExercisesContent(
                    exercises = state.myExercises,
                    isLoading = state.isLoading,
                    error = state.error,
                    isSelectionMode = isSelectionMode,
                    onExerciseClick = { exercise ->
                        if (isSelectionMode) {
                            onItemSelected?.invoke("exercise", exercise.id)
                        } else {
                            onNavigateToExerciseDetail?.invoke(exercise.id)
                        }
                    },
                    onRetry = { viewModel.loadMyExercises() }
                )
            }
        }

        // FAB (only when not in selection mode)
        if (!isSelectionMode) {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = PrometheusOrange,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create", modifier = Modifier.size(28.dp))
            }
        }
    }

    // Create Bottom Sheet
    if (showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface
        ) {
            CreateNewBottomSheet(
                onCreateProgram = {
                    showCreateSheet = false
                    onNavigateToCreateProgram?.invoke()
                },
                onCreateWorkout = {
                    showCreateSheet = false
                    showCreateWorkoutDialog = true
                },
                onCreateExercise = {
                    showCreateSheet = false
                    // TODO: Navigate to create exercise
                }
            )
        }
    }

    // Create Workout Dialog
    if (showCreateWorkoutDialog) {
        CreateWorkoutDialog(
            onDismiss = { showCreateWorkoutDialog = false },
            onConfirm = { name, description ->
                viewModel.clearCreateError()
                scope.launch {
                    viewModel.createWorkout(name, description)
                        .onSuccess { workoutId ->
                            showCreateWorkoutDialog = false
                            onNavigateToWorkoutDetail(workoutId)
                        }
                }
            },
            isCreating = state.isCreating,
            error = state.createError
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// HEADER COMPONENTS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun LibraryHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 48.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.LocalLibrary,
            contentDescription = null,
            tint = PrometheusOrange,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Library",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )
    }
}

@Composable
private fun SelectionModeHeader(
    selectionMode: String,
    onCancelClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PrometheusOrange.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SELECT TO ADD",
                    color = PrometheusOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when (selectionMode) {
                        "calendar" -> "Choose a workout for your calendar"
                        "exercises" -> "Choose an exercise to add"
                        "workouts" -> "Choose a workout to start"
                        "programs" -> "Choose a program to follow"
                        else -> "Select an item"
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onCancelClick) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = Color.White
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TAB COMPONENTS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun LibraryTabs(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        contentColor = PrometheusOrange,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            if (selectedTab < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = PrometheusOrange
                )
            }
        },
        divider = {}
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        title,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                },
                selectedContentColor = PrometheusOrange,
                unselectedContentColor = Gray400
            )
        }
    }
}

@Composable
private fun ExerciseSubTabs(
    selectedSubTab: Int,
    onSubTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SubTabChip(
            text = "All Exercises",
            selected = selectedSubTab == 0,
            onClick = { onSubTabSelected(0) }
        )
        SubTabChip(
            text = "My Sports",
            selected = selectedSubTab == 1,
            onClick = { onSubTabSelected(1) }
        )
    }
}

@Composable
private fun ProgramSubTabs(
    selectedSubTab: Int,
    onSubTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SubTabChip(
            text = "My Programs",
            selected = selectedSubTab == 0,
            onClick = { onSubTabSelected(0) }
        )
        SubTabChip(
            text = "Templates",
            selected = selectedSubTab == 1,
            onClick = { onSubTabSelected(1) }
        )
    }
}

@Composable
private fun WorkoutSubTabs(
    selectedSubTab: Int,
    onSubTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SubTabChip(
            text = "My Workouts",
            selected = selectedSubTab == 0,
            onClick = { onSubTabSelected(0) }
        )
        SubTabChip(
            text = "Templates",
            selected = selectedSubTab == 1,
            onClick = { onSubTabSelected(1) }
        )
    }
}

@Composable
private fun SubTabChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) PrometheusOrange else Color.Transparent,
        border = if (!selected) BorderStroke(1.dp, Gray600) else null
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) Color.White else Gray400,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// FILTER COMPONENTS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun LibrarySearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = {
            Text(
                text = "Search exercises, workouts, programs...",
                color = Gray500,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = PrometheusOrange
            )
        },
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Gray400
                    )
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrometheusOrange,
            unfocusedBorderColor = Gray700,
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface
        )
    )
}

@Composable
private fun SportFilterChips(
    selectedSport: SportCategory,
    onSportSelected: (SportCategory) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(SportCategory.entries) { sport ->
            FilterChip(
                selected = selectedSport == sport,
                onClick = { onSportSelected(sport) },
                label = { Text(sport.displayName, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrometheusOrange.copy(alpha = 0.2f),
                    selectedLabelColor = PrometheusOrange,
                    containerColor = Color.Transparent,
                    labelColor = Gray400
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Gray700,
                    selectedBorderColor = PrometheusOrange,
                    enabled = true,
                    selected = selectedSport == sport
                )
            )
        }
    }
}

@Composable
private fun WorkoutCategoryFilterChips(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("All", "Push", "Pull", "Legs", "Upper", "Lower", "Full Body", "Core")

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrometheusOrange.copy(alpha = 0.2f),
                    selectedLabelColor = PrometheusOrange,
                    containerColor = Color.Transparent,
                    labelColor = Gray400
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Gray700,
                    selectedBorderColor = PrometheusOrange,
                    enabled = true,
                    selected = selectedCategory == category
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// CONTENT COMPONENTS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ExercisesGrid(
    exercises: List<ExerciseListItem>,
    isLoading: Boolean,
    error: String?,
    isSelectionMode: Boolean,
    onExerciseClick: (ExerciseListItem) -> Unit,
    onRetry: () -> Unit
) {
    when {
        isLoading -> LoadingContent()
        error != null -> ErrorContent(error = error, onRetry = onRetry)
        exercises.isEmpty() -> EmptyContent(
            icon = Icons.Default.FitnessCenter,
            title = "No exercises found",
            subtitle = "Try adjusting your filters"
        )
        else -> {
            Column {
                Text(
                    "${exercises.size} exercise${if (exercises.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(exercises) { exercise ->
                        ExerciseCard(
                            exercise = exercise,
                            isSelectionMode = isSelectionMode,
                            onClick = { onExerciseClick(exercise) }
                        )
                    }
                    // Bottom spacing for FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: ExerciseListItem,
    isSelectionMode: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, if (isSelectionMode) PrometheusOrange else Gray700)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Exercise Icon
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = PrometheusOrange.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = PrometheusOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                exercise.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )

            // Category / Muscle Group
            val category = exercise.category ?: exercise.mainMuscleGroup
            if (category != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    category,
                    style = MaterialTheme.typography.bodySmall,
                    color = PrometheusOrange,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Equipment
            val equipment = exercise.equipment?.firstOrNull()
            if (equipment != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    equipment,
                    style = MaterialTheme.typography.labelSmall,
                    color = Gray400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WorkoutsContent(
    subTab: Int,
    workouts: List<RoutineSummary>,
    isLoading: Boolean,
    error: String?,
    searchQuery: String,
    isSelectionMode: Boolean,
    onWorkoutClick: (String) -> Unit,
    onRetry: () -> Unit,
    onCreateClick: () -> Unit
) {
    when (subTab) {
        0 -> MyWorkoutsTab(
            workouts = workouts,
            isLoading = isLoading,
            error = error,
            searchQuery = searchQuery,
            isSelectionMode = isSelectionMode,
            onWorkoutClick = onWorkoutClick,
            onRetry = onRetry,
            onCreateClick = onCreateClick
        )
        1 -> WorkoutTemplatesTab(
            isSelectionMode = isSelectionMode,
            onTemplateClick = onWorkoutClick
        )
    }
}

@Composable
private fun MyWorkoutsTab(
    workouts: List<RoutineSummary>,
    isLoading: Boolean,
    error: String?,
    searchQuery: String,
    isSelectionMode: Boolean,
    onWorkoutClick: (String) -> Unit,
    onRetry: () -> Unit,
    onCreateClick: () -> Unit
) {
    when {
        isLoading -> LoadingContent()
        error != null -> ErrorContent(error = error, onRetry = onRetry)
        workouts.isEmpty() -> {
            EmptyContent(
                icon = Icons.Default.FitnessCenter,
                title = if (searchQuery.isNotEmpty()) "No workouts found" else "No workouts yet",
                subtitle = if (searchQuery.isNotEmpty()) "Try a different search" else "Create your first workout",
                actionLabel = if (searchQuery.isEmpty()) "Create Workout" else null,
                onAction = if (searchQuery.isEmpty()) onCreateClick else null
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "${workouts.size} workout${if (workouts.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(workouts) { workout ->
                    WorkoutCard(
                        workout = workout,
                        isSelectionMode = isSelectionMode,
                        onClick = { onWorkoutClick(workout.id) }
                    )
                }
                // Bottom spacing for FAB
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun WorkoutTemplatesTab(
    isSelectionMode: Boolean,
    onTemplateClick: (String) -> Unit
) {
    // TODO: Load workout templates from Supabase
    EmptyContent(
        icon = Icons.Default.AutoAwesome,
        title = "Workout Templates",
        subtitle = "Coming soon - Pre-built workout templates"
    )
}

@Composable
private fun WorkoutCard(
    workout: RoutineSummary,
    isSelectionMode: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, if (isSelectionMode) PrometheusOrange else Gray700)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = PrometheusOrange.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = PrometheusOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    workout.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PrometheusOrange
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${workout.exerciseCount} exercise${if (workout.exerciseCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray400
                    )
                }
                if (workout.description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        workout.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = PrometheusOrange
            )
        }
    }
}

@Composable
private fun ProgramsContent(
    subTab: Int,
    isLoading: Boolean,
    error: String?,
    isSelectionMode: Boolean,
    onProgramClick: (String) -> Unit
) {
    when (subTab) {
        0 -> {
            // My Programs
            EmptyContent(
                icon = Icons.Default.CalendarViewWeek,
                title = "No programs yet",
                subtitle = "Create or start a program from templates"
            )
        }
        1 -> {
            // Program Templates
            EmptyContent(
                icon = Icons.Default.AutoAwesome,
                title = "Program Templates",
                subtitle = "Coming soon - Multi-week training programs"
            )
        }
    }
}

@Composable
private fun MyExercisesContent(
    exercises: List<ExerciseListItem>,
    isLoading: Boolean,
    error: String?,
    isSelectionMode: Boolean,
    onExerciseClick: (ExerciseListItem) -> Unit,
    onRetry: () -> Unit
) {
    when {
        isLoading -> LoadingContent()
        error != null -> ErrorContent(error = error, onRetry = onRetry)
        exercises.isEmpty() -> EmptyContent(
            icon = Icons.Default.FitnessCenter,
            title = "No custom exercises",
            subtitle = "Exercises you create will appear here"
        )
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(exercises) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        isSelectionMode = isSelectionMode,
                        onClick = { onExerciseClick(exercise) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// UTILITY COMPONENTS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PrometheusOrange)
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = ErrorRed
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(error, color = Gray400)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Gray600
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Gray500
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// BOTTOM SHEET & DIALOGS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CreateNewBottomSheet(
    onCreateProgram: () -> Unit,
    onCreateWorkout: () -> Unit,
    onCreateExercise: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "Create New",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        ListItem(
            headlineContent = {
                Text("Program", color = Color.White, fontWeight = FontWeight.Medium)
            },
            supportingContent = {
                Text("Build a new multi-week training plan", color = Gray400)
            },
            leadingContent = {
                Icon(Icons.Default.CalendarViewWeek, contentDescription = null, tint = PrometheusOrange)
            },
            modifier = Modifier.clickable(onClick = onCreateProgram),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        ListItem(
            headlineContent = {
                Text("Workout", color = Color.White, fontWeight = FontWeight.Medium)
            },
            supportingContent = {
                Text("Assemble a new single-day workout", color = Gray400)
            },
            leadingContent = {
                Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = PrometheusOrange)
            },
            modifier = Modifier.clickable(onClick = onCreateWorkout),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        ListItem(
            headlineContent = {
                Text("Exercise", color = Color.White, fontWeight = FontWeight.Medium)
            },
            supportingContent = {
                Text("Define a new exercise from scratch", color = Gray400)
            },
            leadingContent = {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = PrometheusOrange)
            },
            modifier = Modifier.clickable(onClick = onCreateExercise),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun CreateWorkoutDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?) -> Unit,
    isCreating: Boolean,
    error: String? = null
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Create Workout", color = Color.White) },
        text = {
            Column {
                if (error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(error, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = Gray400) },
                    placeholder = { Text("e.g., Full Body Workout", color = Gray600) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrometheusOrange,
                        unfocusedBorderColor = Gray700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)", color = Gray400) },
                    placeholder = { Text("e.g., A complete workout routine", color = Gray600) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrometheusOrange,
                        unfocusedBorderColor = Gray700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description.ifBlank { null }) },
                enabled = name.isNotBlank() && !isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Gray400)
            }
        }
    )
}
