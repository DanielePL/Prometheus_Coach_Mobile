package com.prometheuscoach.mobile.ui.screens.workouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWorkoutDetail: (String) -> Unit,
    onNavigateToTemplateDetail: (String) -> Unit = {},
    viewModel: WorkoutsViewModel = hiltViewModel(),
    templatesViewModel: TemplatesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val libraryState by viewModel.libraryState.collectAsState()
    val templatesState by templatesViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Workouts", "Library", "Templates")

    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Load content when tab changes
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            1 -> if (libraryState.exercises.isEmpty()) viewModel.loadExerciseLibrary()
            2 -> if (templatesState.systemTemplates.isEmpty()) templatesViewModel.loadTemplates()
        }
    }

    val filteredWorkouts = remember(state.workouts, searchQuery) {
        if (searchQuery.isBlank()) {
            state.workouts
        } else {
            state.workouts.filter { workout ->
                workout.name.contains(searchQuery, ignoreCase = true) ||
                    workout.description?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Workouts", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        if (selectedTab == 0) {
                            IconButton(onClick = { showCreateDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Create Workout", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                when (selectedTab) {
                    0 -> FloatingActionButton(
                        onClick = { showCreateDialog = true },
                        containerColor = PrometheusOrange
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Workout", tint = Color.White)
                    }
                    2 -> if (templatesState.selectedSubTab == TemplateSubTab.MY_TEMPLATES) {
                        FloatingActionButton(
                            onClick = { showCreateTemplateDialog = true },
                            containerColor = PrometheusOrange
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Create Template", tint = Color.White)
                        }
                    }
                }
            }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface.copy(alpha = 0.7f),
                contentColor = PrometheusOrange,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrometheusOrange
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) PrometheusOrange else TextSecondary
                            )
                        },
                        selectedContentColor = PrometheusOrange,
                        unselectedContentColor = TextSecondary
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                    if (selectedTab == 1) {
                        viewModel.searchExercises(query)
                    }
                },
                placeholder = {
                    Text(
                        if (selectedTab == 0) "Search workouts..." else "Search exercises...",
                        color = TextSecondary
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            if (selectedTab == 1) {
                                viewModel.searchExercises("")
                            }
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurface.copy(alpha = 0.5f),
                    unfocusedContainerColor = DarkSurface.copy(alpha = 0.5f),
                    focusedBorderColor = PrometheusOrange,
                    unfocusedBorderColor = DarkSurface,
                    cursorColor = PrometheusOrange
                )
            )

            // Content based on selected tab
            when (selectedTab) {
                0 -> MyWorkoutsTab(
                    workouts = filteredWorkouts,
                    isLoading = state.isLoading,
                    error = state.error,
                    searchQuery = searchQuery,
                    onRetry = { viewModel.loadWorkouts() },
                    onWorkoutClick = onNavigateToWorkoutDetail,
                    onCreateClick = { showCreateDialog = true }
                )
                1 -> ExerciseLibraryTab(
                    exercises = libraryState.exercises,
                    categories = libraryState.categories,
                    selectedCategory = libraryState.selectedCategory,
                    isLoading = libraryState.isLoading,
                    error = libraryState.error,
                    onCategoryChange = { viewModel.setLibraryCategoryFilter(it) },
                    onRetry = { viewModel.loadExerciseLibrary() }
                )
                2 -> TemplatesTab(
                    state = templatesState,
                    viewModel = templatesViewModel,
                    onTemplateClick = onNavigateToTemplateDetail,
                    onUseTemplate = { templateId, templateName ->
                        templatesViewModel.showCloneDialog(templateId, templateName)
                    },
                    snackbarHostState = snackbarHostState
                )
            }
        }
        }
    }

    // Create Workout Dialog
    if (showCreateDialog) {
        CreateWorkoutDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description ->
                viewModel.clearCreateError() // Clear previous error
                scope.launch {
                    viewModel.createWorkout(name, description)
                        .onSuccess { workoutId ->
                            showCreateDialog = false
                            onNavigateToWorkoutDetail(workoutId)
                        }
                        .onFailure {
                            // Error will be shown in dialog
                        }
                }
            },
            isCreating = state.isCreating,
            error = state.createError
        )
    }

    // Create Template Dialog
    if (showCreateTemplateDialog) {
        CreateTemplateDialog(
            categories = templatesState.categories,
            onDismiss = { showCreateTemplateDialog = false },
            onConfirm = { name, description, categoryId ->
                scope.launch {
                    templatesViewModel.createTemplate(name, description, categoryId)
                        .onSuccess { template ->
                            showCreateTemplateDialog = false
                            snackbarHostState.showSnackbar("Template erstellt!")
                            onNavigateToTemplateDetail(template.id)
                        }
                        .onFailure { error ->
                            snackbarHostState.showSnackbar("Fehler: ${error.message}")
                        }
                }
            }
        )
    }
}

@Composable
private fun MyWorkoutsTab(
    workouts: List<WorkoutSummary>,
    isLoading: Boolean,
    error: String?,
    searchQuery: String,
    onRetry: () -> Unit,
    onWorkoutClick: (String) -> Unit,
    onCreateClick: () -> Unit
) {
    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrometheusOrange)
            }
        }

        error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
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
                    Text(error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }

        workouts.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (searchQuery.isNotEmpty()) "No workouts found" else "No workouts yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        if (searchQuery.isNotEmpty()) "Try a different search" else "Create your first workout to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    if (searchQuery.isEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onCreateClick,
                            colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Workout")
                        }
                    }
                }
            }
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
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(workouts) { workout ->
                    WorkoutCard(
                        workout = workout,
                        onClick = { onWorkoutClick(workout.id) }
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

@Composable
private fun ExerciseLibraryTab(
    exercises: List<ExerciseListItem>,
    categories: List<String>,
    selectedCategory: String?,
    isLoading: Boolean,
    error: String?,
    onCategoryChange: (String?) -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Category Filter Chips
        if (categories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrometheusOrange)
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                        Text(error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
            }

            exercises.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No exercises found",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            "Try adjusting your filters",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            else -> {
                // Results count
                Text(
                    "${exercises.size} exercise${if (exercises.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Exercise Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(exercises) { exercise ->
                        ExerciseLibraryCard(exercise = exercise)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseLibraryCard(
    exercise: ExerciseListItem
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassPremium(cornerRadius = RadiusMedium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Exercise Icon
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterHorizontally),
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

            // Exercise Name
            Text(
                exercise.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally)
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
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Equipment
            val equipment = exercise.equipment?.firstOrNull()
            if (equipment != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    equipment,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: WorkoutSummary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassPremium(cornerRadius = RadiusMedium)
            .clickable(onClick = onClick)
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
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                if (workout.description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        workout.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1
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
        title = { Text("Create Workout") },
        text = {
            Column {
                if (error != null) {
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
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g., Full Body Workout") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("e.g., A complete workout routine") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
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
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==================== TEMPLATES TAB ====================

@Composable
private fun TemplatesTab(
    state: TemplatesState,
    viewModel: TemplatesViewModel,
    onTemplateClick: (String) -> Unit,
    onUseTemplate: (templateId: String, templateName: String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-tabs for System / My Templates / Favorites
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TemplateSubTab.entries.forEach { tab ->
                FilterChip(
                    selected = state.selectedSubTab == tab,
                    onClick = { viewModel.selectSubTab(tab) },
                    label = {
                        Text(
                            when (tab) {
                                TemplateSubTab.SYSTEM -> "System"
                                TemplateSubTab.MY_TEMPLATES -> "Meine"
                                TemplateSubTab.FAVORITES -> "Favoriten"
                            }
                        )
                    },
                    leadingIcon = if (state.selectedSubTab == tab) {
                        {
                            Icon(
                                when (tab) {
                                    TemplateSubTab.SYSTEM -> Icons.Default.Public
                                    TemplateSubTab.MY_TEMPLATES -> Icons.Default.Person
                                    TemplateSubTab.FAVORITES -> Icons.Default.Star
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrometheusOrange.copy(alpha = 0.2f),
                        selectedLabelColor = PrometheusOrange,
                        selectedLeadingIconColor = PrometheusOrange
                    )
                )
            }
        }

        // Level Filter (for system templates)
        if (state.selectedSubTab == TemplateSubTab.SYSTEM) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedLevel == null,
                        onClick = { viewModel.selectLevel(null) },
                        label = { Text("Alle Level") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrometheusOrange.copy(alpha = 0.2f),
                            selectedLabelColor = PrometheusOrange
                        )
                    )
                }
                items(FitnessLevel.entries.size) { index ->
                    val level = FitnessLevel.entries[index]
                    FilterChip(
                        selected = state.selectedLevel == level,
                        onClick = { viewModel.selectLevel(level) },
                        label = { Text(level.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrometheusOrange.copy(alpha = 0.2f),
                            selectedLabelColor = PrometheusOrange
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Content
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrometheusOrange)
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                        Text(state.error, color = TextPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            else -> {
                when (state.selectedSubTab) {
                    TemplateSubTab.SYSTEM -> SystemTemplatesContent(
                        templates = viewModel.getFilteredSystemTemplates(),
                        onTemplateClick = onTemplateClick,
                        onUseTemplate = onUseTemplate,
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )

                    TemplateSubTab.MY_TEMPLATES -> CoachTemplatesContent(
                        templates = viewModel.getFilteredCoachTemplates(),
                        onTemplateClick = onTemplateClick,
                        onUseTemplate = onUseTemplate,
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )

                    TemplateSubTab.FAVORITES -> FavoritesContent(
                        templates = viewModel.getFilteredFavorites(),
                        onTemplateClick = onTemplateClick,
                        onUseTemplate = onUseTemplate,
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                }
            }
        }
    }

    // Clone Dialog
    if (state.showCloneDialog && state.cloneTemplateId != null) {
        CloneTemplateDialog(
            templateName = state.cloneTemplateName ?: "",
            isCloning = state.isCloning,
            error = state.cloneError,
            onDismiss = { viewModel.dismissCloneDialog() },
            onConfirm = { level, scaling, customName ->
                scope.launch {
                    viewModel.cloneTemplate(level, scaling, customName)
                        .onSuccess { workoutId ->
                            snackbarHostState.showSnackbar("Workout erstellt!")
                        }
                }
            }
        )
    }
}

@Composable
private fun SystemTemplatesContent(
    templates: Map<TemplateCategory, List<TemplateSummary>>,
    onTemplateClick: (String) -> Unit,
    onUseTemplate: (String, String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    if (templates.isEmpty()) {
        EmptyTemplatesState(message = "Keine System-Templates verfügbar")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            templates.forEach { (category, categoryTemplates) ->
                item {
                    Text(
                        category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrometheusOrange
                    )
                }
                items(categoryTemplates.size) { index ->
                    TemplateCard(
                        template = categoryTemplates[index],
                        onClick = { onTemplateClick(categoryTemplates[index].id) },
                        onUse = { onUseTemplate(categoryTemplates[index].id, categoryTemplates[index].name) },
                        onToggleFavorite = { onToggleFavorite(categoryTemplates[index].id) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun CoachTemplatesContent(
    templates: List<TemplateSummary>,
    onTemplateClick: (String) -> Unit,
    onUseTemplate: (String, String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    if (templates.isEmpty()) {
        EmptyTemplatesState(message = "Noch keine eigenen Templates")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "${templates.size} Template${if (templates.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            items(templates.size) { index ->
                TemplateCard(
                    template = templates[index],
                    onClick = { onTemplateClick(templates[index].id) },
                    onUse = { onUseTemplate(templates[index].id, templates[index].name) },
                    onToggleFavorite = { onToggleFavorite(templates[index].id) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun FavoritesContent(
    templates: List<TemplateSummary>,
    onTemplateClick: (String) -> Unit,
    onUseTemplate: (String, String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    if (templates.isEmpty()) {
        EmptyTemplatesState(message = "Keine Favoriten")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(templates.size) { index ->
                TemplateCard(
                    template = templates[index],
                    onClick = { onTemplateClick(templates[index].id) },
                    onUse = { onUseTemplate(templates[index].id, templates[index].name) },
                    onToggleFavorite = { onToggleFavorite(templates[index].id) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun EmptyTemplatesState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: TemplateSummary,
    onClick: () -> Unit,
    onUse: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassPremium(cornerRadius = RadiusMedium)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Icon based on type
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = when (template.templateType) {
                            TemplateType.SYSTEM -> PrometheusOrange.copy(alpha = 0.2f)
                            TemplateType.COACH -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            TemplateType.AI -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when (template.templateType) {
                                    TemplateType.SYSTEM -> Icons.Default.Public
                                    TemplateType.COACH -> Icons.Default.Person
                                    TemplateType.AI -> Icons.Default.AutoAwesome
                                },
                                contentDescription = null,
                                tint = when (template.templateType) {
                                    TemplateType.SYSTEM -> PrometheusOrange
                                    TemplateType.COACH -> Color(0xFF4CAF50)
                                    TemplateType.AI -> Color(0xFF2196F3)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            template.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Level badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (template.level) {
                                    FitnessLevel.BEGINNER -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    FitnessLevel.INTERMEDIATE -> PrometheusOrange.copy(alpha = 0.2f)
                                    FitnessLevel.ADVANCED -> Color(0xFFF44336).copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    template.level.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (template.level) {
                                        FitnessLevel.BEGINNER -> Color(0xFF4CAF50)
                                        FitnessLevel.INTERMEDIATE -> PrometheusOrange
                                        FitnessLevel.ADVANCED -> Color(0xFFF44336)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                "${template.exerciseCount} Übungen",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Actions
                Row {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (template.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorit",
                            tint = if (template.isFavorite) Color(0xFFFFD700) else TextSecondary
                        )
                    }
                }
            }

            // Description
            if (template.description != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Target muscles
            if (template.targetMuscles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(template.targetMuscles.size) { index ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DarkSurface
                        ) {
                            Text(
                                template.targetMuscles[index],
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Use button
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onUse,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Template verwenden")
            }
        }
    }
}

@Composable
private fun CloneTemplateDialog(
    templateName: String,
    isCloning: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (level: FitnessLevel, scaling: Int, customName: String?) -> Unit
) {
    var selectedLevel by remember { mutableStateOf(FitnessLevel.INTERMEDIATE) }
    var scalingPercentage by remember { mutableFloatStateOf(100f) }
    var customName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Template verwenden") },
        text = {
            Column {
                if (error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            error,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    "Template: $templateName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Level Selection
                Text("Fitness-Level", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FitnessLevel.entries.forEach { level ->
                        FilterChip(
                            selected = selectedLevel == level,
                            onClick = { selectedLevel = level },
                            label = { Text(level.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrometheusOrange.copy(alpha = 0.2f),
                                selectedLabelColor = PrometheusOrange
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scaling Slider
                Text(
                    "Volumen-Skalierung: ${scalingPercentage.toInt()}%",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = scalingPercentage,
                    onValueChange = { scalingPercentage = it },
                    valueRange = 50f..100f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = PrometheusOrange,
                        activeTrackColor = PrometheusOrange
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Name (optional)
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Name (optional)") },
                    placeholder = { Text("$templateName (${selectedLevel.displayName})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedLevel,
                        scalingPercentage.toInt(),
                        customName.ifBlank { null }
                    )
                },
                enabled = !isCloning,
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
            ) {
                if (isCloning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Workout erstellen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTemplateDialog(
    categories: List<TemplateCategory>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?, categoryId: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TemplateCategory?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neues Template erstellen") },
        text = {
            Column {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("z.B. Mein Push-Workout") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung (optional)") },
                    placeholder = { Text("z.B. Mein persönliches Push-Workout") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Dropdown
                Text("Kategorie (optional)", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Keine Kategorie",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Keine Kategorie") },
                            onClick = {
                                selectedCategory = null
                                expanded = false
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,
                        description.ifBlank { null },
                        selectedCategory?.id
                    )
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
            ) {
                Text("Erstellen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
