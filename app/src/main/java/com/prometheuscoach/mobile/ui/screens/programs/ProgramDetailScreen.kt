package com.prometheuscoach.mobile.ui.screens.programs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.prometheuscoach.mobile.data.model.ProgramWeekWithWorkouts
import com.prometheuscoach.mobile.data.model.ProgramWorkoutDetail
import com.prometheuscoach.mobile.data.model.RoutineSummary
import com.prometheuscoach.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDetailScreen(
    programId: String,
    onNavigateBack: () -> Unit,
    onNavigateToWorkoutDetail: (String) -> Unit = {},
    onAssignToClient: ((String) -> Unit)? = null,
    viewModel: ProgramDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    var selectedWeekIndex by remember { mutableIntStateOf(0) }
    var showAddWorkoutSheet by remember { mutableStateOf(false) }
    var selectedWeekForWorkout by remember { mutableStateOf<ProgramWeekWithWorkouts?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAssignSheet by remember { mutableStateOf(false) }

    // Load program on first composition
    LaunchedEffect(programId) {
        viewModel.loadProgram(programId)
    }

    // Handle delete success
    LaunchedEffect(state.deleteSuccess) {
        if (state.deleteSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.program?.name ?: "Program",
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Assign to client button
                    IconButton(onClick = { showAssignSheet = true }) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = "Assign to Client",
                            tint = PrometheusOrange
                        )
                    }
                    // More options
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = DarkSurface
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Program", color = Color.White) },
                            onClick = {
                                showMenu = false
                                // TODO: Navigate to edit
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, null, tint = Gray400)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate", color = Color.White) },
                            onClick = {
                                showMenu = false
                                // TODO: Duplicate program
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, null, tint = Gray400)
                            }
                        )
                        HorizontalDivider(color = Gray700)
                        DropdownMenuItem(
                            text = { Text("Delete Program", color = ErrorRed) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null, tint = ErrorRed)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkBackground, DarkBackgroundSecondary)
                    )
                )
                .padding(paddingValues)
        ) {
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
                    ErrorContent(
                        error = state.error!!,
                        onRetry = { viewModel.loadProgram(programId) }
                    )
                }
                state.program != null -> {
                    val program = state.program!!

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Program Info Header
                        ProgramInfoHeader(
                            name = program.name,
                            description = program.description,
                            durationWeeks = program.durationWeeks,
                            workoutsPerWeek = program.workoutsPerWeek,
                            difficulty = program.difficulty,
                            status = program.status
                        )

                        // Week Selector
                        if (program.weeks.isNotEmpty()) {
                            WeekSelector(
                                weeks = program.weeks,
                                selectedWeekIndex = selectedWeekIndex,
                                onWeekSelected = { selectedWeekIndex = it }
                            )

                            // Selected Week Content
                            val selectedWeek = program.weeks.getOrNull(selectedWeekIndex)
                            if (selectedWeek != null) {
                                WeekContent(
                                    week = selectedWeek,
                                    workoutsPerWeek = program.workoutsPerWeek,
                                    onWorkoutClick = { workout ->
                                        onNavigateToWorkoutDetail(workout.routineId)
                                    },
                                    onAddWorkout = { dayNumber ->
                                        selectedWeekForWorkout = selectedWeek
                                        viewModel.loadAvailableWorkouts()
                                        showAddWorkoutSheet = true
                                    },
                                    onRemoveWorkout = { workoutId ->
                                        viewModel.removeWorkoutFromWeek(workoutId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Workout Bottom Sheet
    if (showAddWorkoutSheet && selectedWeekForWorkout != null) {
        AddWorkoutBottomSheet(
            weekNumber = selectedWeekForWorkout!!.weekNumber,
            availableWorkouts = state.availableWorkouts,
            isLoading = state.isAddingWorkout,
            onDismiss = {
                showAddWorkoutSheet = false
                selectedWeekForWorkout = null
            },
            onSelectWorkout = { workout, dayNumber ->
                viewModel.addWorkoutToWeek(
                    weekId = selectedWeekForWorkout!!.id,
                    routineId = workout.id,
                    dayNumber = dayNumber
                )
                showAddWorkoutSheet = false
                selectedWeekForWorkout = null
            }
        )
    }

    // Assign to Client Sheet
    if (showAssignSheet) {
        AssignProgramSheet(
            programName = state.program?.name ?: "",
            onDismiss = { showAssignSheet = false },
            onAssign = { clientId, startDate ->
                // TODO: Implement program assignment
                showAssignSheet = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DarkSurface,
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Delete Program?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will permanently delete \"${state.program?.name}\" and all its weekly workouts. This action cannot be undone.",
                    color = Gray400
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteProgram()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    enabled = !state.isDeleting
                ) {
                    if (state.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Gray400)
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// PROGRAM INFO HEADER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProgramInfoHeader(
    name: String,
    description: String?,
    durationWeeks: Int,
    workoutsPerWeek: Int,
    difficulty: String?,
    status: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = status)
                if (difficulty != null) {
                    DifficultyBadge(difficulty = difficulty)
                }
            }

            if (description != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = description,
                    color = Gray400,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.CalendarMonth,
                    value = "$durationWeeks",
                    label = "Weeks"
                )
                StatItem(
                    icon = Icons.Default.FitnessCenter,
                    value = "$workoutsPerWeek",
                    label = "Per Week"
                )
                StatItem(
                    icon = Icons.Default.Timer,
                    value = "${durationWeeks * workoutsPerWeek}",
                    label = "Total"
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "active" -> PrometheusOrange to "ACTIVE"
        "draft" -> Gray500 to "DRAFT"
        "archived" -> Gray600 to "ARCHIVED"
        else -> Gray500 to status.uppercase()
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun DifficultyBadge(difficulty: String) {
    val color = when (difficulty.lowercase()) {
        "beginner" -> SuccessGreen
        "intermediate" -> PrometheusOrange
        "advanced" -> ErrorRed
        else -> Gray500
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Speed,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = difficulty,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = PrometheusOrange,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Gray500,
            fontSize = 12.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// WEEK SELECTOR
// ═══════════════════════════════════════════════════════════════

@Composable
private fun WeekSelector(
    weeks: List<ProgramWeekWithWorkouts>,
    selectedWeekIndex: Int,
    onWeekSelected: (Int) -> Unit
) {
    Column {
        Text(
            text = "PROGRAM WEEKS",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = PrometheusOrange,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(weeks.size) { index ->
                val week = weeks[index]
                val isSelected = index == selectedWeekIndex
                val workoutCount = week.workouts.size

                WeekChip(
                    weekNumber = week.weekNumber,
                    workoutCount = workoutCount,
                    isSelected = isSelected,
                    onClick = { onWeekSelected(index) }
                )
            }
        }
    }
}

@Composable
private fun WeekChip(
    weekNumber: Int,
    workoutCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrometheusOrange else DarkSurface
        ),
        border = if (!isSelected) BorderStroke(1.dp, Gray700) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Week $weekNumber",
                color = if (isSelected) Color.White else Gray300,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$workoutCount workouts",
                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Gray500,
                fontSize = 11.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// WEEK CONTENT
// ═══════════════════════════════════════════════════════════════

@Composable
private fun WeekContent(
    week: ProgramWeekWithWorkouts,
    workoutsPerWeek: Int,
    onWorkoutClick: (ProgramWorkoutDetail) -> Unit,
    onAddWorkout: (Int) -> Unit,
    onRemoveWorkout: (String) -> Unit
) {
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Week description if present
        if (week.description != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PrometheusOrange.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = PrometheusOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = week.description,
                            color = Gray300,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Day slots
        items(7) { dayIndex ->
            val dayNumber = dayIndex + 1
            val dayWorkouts = week.workouts.filter { it.dayNumber == dayNumber }

            DaySlot(
                dayName = dayNames[dayIndex],
                dayNumber = dayNumber,
                workouts = dayWorkouts,
                onWorkoutClick = onWorkoutClick,
                onAddWorkout = { onAddWorkout(dayNumber) },
                onRemoveWorkout = onRemoveWorkout
            )
        }

        // Bottom spacing
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun DaySlot(
    dayName: String,
    dayNumber: Int,
    workouts: List<ProgramWorkoutDetail>,
    onWorkoutClick: (ProgramWorkoutDetail) -> Unit,
    onAddWorkout: () -> Unit,
    onRemoveWorkout: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Day Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                if (workouts.isEmpty()) {
                    TextButton(
                        onClick = onAddWorkout,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = PrometheusOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Add Workout", color = PrometheusOrange, fontSize = 13.sp)
                    }
                }
            }

            if (workouts.isEmpty()) {
                // Rest day indicator
                Text(
                    text = "Rest Day",
                    color = Gray600,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Spacer(Modifier.height(8.dp))
                workouts.forEach { workout ->
                    WorkoutSlotCard(
                        workout = workout,
                        onClick = { onWorkoutClick(workout) },
                        onRemove = { onRemoveWorkout(workout.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutSlotCard(
    workout: ProgramWorkoutDetail,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = PrometheusOrange.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = PrometheusOrange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.routineName,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                if (workout.exerciseCount > 0) {
                    Text(
                        text = "${workout.exerciseCount} exercises",
                        color = Gray500,
                        fontSize = 12.sp
                    )
                }
                if (workout.notes != null) {
                    Text(
                        text = workout.notes,
                        color = Gray500,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = { showRemoveDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Gray500,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            containerColor = DarkSurface,
            title = { Text("Remove Workout?", color = Color.White) },
            text = {
                Text(
                    "Remove \"${workout.routineName}\" from this day?",
                    color = Gray400
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveDialog = false
                        onRemove()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel", color = Gray400)
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// ADD WORKOUT SHEET
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWorkoutBottomSheet(
    weekNumber: Int,
    availableWorkouts: List<RoutineSummary>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSelectWorkout: (RoutineSummary, Int) -> Unit
) {
    var selectedDay by remember { mutableIntStateOf(1) }
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Add Workout to Week $weekNumber",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(16.dp))

            // Day Selector
            Text(
                "SELECT DAY",
                color = PrometheusOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(7) { index ->
                    val dayNumber = index + 1
                    FilterChip(
                        selected = selectedDay == dayNumber,
                        onClick = { selectedDay = dayNumber },
                        label = { Text(dayNames[index]) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrometheusOrange,
                            selectedLabelColor = Color.White,
                            containerColor = Color.Transparent,
                            labelColor = Gray400
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Gray700,
                            selectedBorderColor = PrometheusOrange,
                            enabled = true,
                            selected = selectedDay == dayNumber
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Workout List
            Text(
                "SELECT WORKOUT",
                color = PrometheusOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))

            if (availableWorkouts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = Gray600,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No workouts available",
                            color = Gray500,
                            fontSize = 14.sp
                        )
                        Text(
                            "Create a workout first",
                            color = Gray600,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableWorkouts) { workout ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isLoading) {
                                    onSelectWorkout(workout, selectedDay)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = DarkBackground
                            ),
                            border = BorderStroke(1.dp, Gray700),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = PrometheusOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        workout.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${workout.exerciseCount} exercises",
                                        color = Gray500,
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Gray500
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ASSIGN PROGRAM SHEET
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignProgramSheet(
    programName: String,
    onDismiss: () -> Unit,
    onAssign: (clientId: String, startDate: String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = null,
                tint = PrometheusOrange,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))

            Text(
                "Assign Program",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Assign \"$programName\" to one of your clients",
                color = Gray400,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(24.dp))

            // Placeholder for client selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DarkBackground
                ),
                border = BorderStroke(1.dp, Gray700)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = Gray600,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Select a Client",
                        color = Gray400,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Coming soon - choose a client to assign this program",
                        color = Gray600,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ERROR CONTENT
// ═══════════════════════════════════════════════════════════════

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
