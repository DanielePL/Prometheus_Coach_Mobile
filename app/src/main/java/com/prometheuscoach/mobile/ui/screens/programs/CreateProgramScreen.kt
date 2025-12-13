package com.prometheuscoach.mobile.ui.screens.programs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prometheuscoach.mobile.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProgramScreen(
    onNavigateBack: () -> Unit,
    onProgramCreated: (String) -> Unit = {},
    viewModel: CreateProgramViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    var programName by remember { mutableStateOf("") }
    var programDescription by remember { mutableStateOf("") }
    var durationWeeks by remember { mutableStateOf("12") }
    var workoutsPerWeek by remember { mutableStateOf("4") }
    var difficulty by remember { mutableStateOf("Intermediate") }
    var durationUnit by remember { mutableStateOf("Weeks") }

    var showSaveDialog by remember { mutableStateOf(false) }

    val isFormValid = programName.isNotBlank() &&
            durationWeeks.isNotBlank() &&
            workoutsPerWeek.isNotBlank() &&
            (durationWeeks.toIntOrNull() ?: 0) > 0 &&
            (workoutsPerWeek.toIntOrNull() ?: 0) in 1..7

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Program", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showSaveDialog = true },
                        enabled = isFormValid && !state.isCreating
                    ) {
                        Text(
                            text = "SAVE",
                            color = if (isFormValid) PrometheusOrange else Gray500,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                // Error message
                if (state.error != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, null, tint = ErrorRed)
                                Spacer(Modifier.width(8.dp))
                                Text(state.error!!, color = ErrorRed)
                            }
                        }
                    }
                }

                // Header
                item {
                    SectionHeader("BASIC INFORMATION")
                }

                // Program Name
                item {
                    FormCard {
                        FormLabel("PROGRAM NAME")
                        OutlinedTextField(
                            value = programName,
                            onValueChange = { programName = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("e.g. Strength & Hypertrophy", color = Gray600)
                            },
                            colors = textFieldColors(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Description
                item {
                    FormCard {
                        FormLabel("DESCRIPTION (OPTIONAL)")
                        OutlinedTextField(
                            value = programDescription,
                            onValueChange = { programDescription = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            placeholder = {
                                Text("Describe your program...", color = Gray600)
                            },
                            colors = textFieldColors(),
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Duration Section
                item {
                    SectionHeader("DURATION")
                }

                // Duration in Weeks
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = durationWeeks,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() } && it.length <= 3) {
                                    durationWeeks = it
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Duration", color = Gray400) },
                            placeholder = { Text("12", color = Gray600) },
                            colors = textFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Unit Selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unit",
                                color = Gray400,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                UnitButton(
                                    text = "Weeks",
                                    isSelected = durationUnit == "Weeks",
                                    onClick = { durationUnit = "Weeks" },
                                    modifier = Modifier.weight(1f)
                                )
                                UnitButton(
                                    text = "Months",
                                    isSelected = durationUnit == "Months",
                                    onClick = { durationUnit = "Months" },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Workouts Per Week
                item {
                    OutlinedTextField(
                        value = workoutsPerWeek,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 1) {
                                workoutsPerWeek = it
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Workouts Per Week", color = Gray400) },
                        placeholder = { Text("4", color = Gray600) },
                        colors = textFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            Text("1-7 workouts per week", color = Gray500, fontSize = 12.sp)
                        }
                    )
                }

                // Difficulty Level
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("DIFFICULTY LEVEL")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Beginner", "Intermediate", "Advanced").forEach { level ->
                                DifficultyChip(
                                    text = level,
                                    isSelected = difficulty == level,
                                    onClick = { difficulty = level },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = PrometheusOrange.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PrometheusOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "You'll be able to add workouts to each week after creating the program.",
                                color = Gray400,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // Save Confirmation Dialog
    if (showSaveDialog) {
        val weeks = if (durationUnit == "Months") {
            (durationWeeks.toIntOrNull() ?: 0) * 4
        } else {
            durationWeeks.toIntOrNull() ?: 0
        }

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = DarkSurface,
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarViewWeek,
                        contentDescription = null,
                        tint = PrometheusOrange,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Create Program?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "You're about to create:",
                        color = Gray400,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        programName,
                        color = PrometheusOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "• $weeks weeks\n• $workoutsPerWeek workouts per week\n• $difficulty level",
                        color = Gray400,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.createProgram(
                                name = programName,
                                description = programDescription.ifBlank { null },
                                durationWeeks = weeks,
                                workoutsPerWeek = workoutsPerWeek.toIntOrNull() ?: 4,
                                difficulty = difficulty
                            ).onSuccess { programId ->
                                showSaveDialog = false
                                onProgramCreated(programId)
                                onNavigateBack()
                            }
                        }
                    },
                    enabled = !state.isCreating,
                    colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("CREATE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = Gray400)
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = PrometheusOrange,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        color = Gray400,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 1.sp
    )
}

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.5.dp, PrometheusOrange.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = PrometheusOrange,
    unfocusedBorderColor = Gray700,
    cursorColor = PrometheusOrange,
    focusedLabelColor = PrometheusOrange,
    unfocusedLabelColor = Gray400
)

@Composable
private fun UnitButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrometheusOrange else Gray700,
            contentColor = if (isSelected) Color.White else Gray400
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun DifficultyChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrometheusOrange.copy(alpha = 0.2f) else DarkSurface
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isSelected) PrometheusOrange else Gray700
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) PrometheusOrange else Gray400,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
    }
}
