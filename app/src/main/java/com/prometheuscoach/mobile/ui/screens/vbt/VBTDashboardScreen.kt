package com.prometheuscoach.mobile.ui.screens.vbt

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VBTDashboardScreen(
    clientId: String,
    clientName: String,
    onNavigateBack: () -> Unit,
    viewModel: VBTDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

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
                                "VBT Dashboard",
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
                        // Live/History toggle
                        LiveModeToggle(
                            isLive = state.isLiveMode,
                            onToggle = { viewModel.toggleLiveMode(it) }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
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
                            Text(
                                state.error!!,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrometheusOrange
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                state.availableExercises.isEmpty() && !state.isLiveMode -> {
                    EmptyVBTState(modifier = Modifier.padding(paddingValues))
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        // Live Mode Banner
                        if (state.isLiveMode) {
                            item {
                                LiveModeBanner(session = state.liveSession)
                            }
                        }

                        // Exercise Selector
                        if (state.availableExercises.isNotEmpty()) {
                            item {
                                ExerciseSelector(
                                    exercises = state.availableExercises,
                                    selectedId = state.selectedExerciseId,
                                    onSelect = { viewModel.selectExercise(it) }
                                )
                            }
                        }

                        // Readiness Card
                        state.readiness?.let { readiness ->
                            item {
                                ReadinessCard(readiness = readiness)
                            }
                        }

                        // Live velocity & fatigue row
                        if (state.isLiveMode && state.liveSession != null) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    VelocityCard(
                                        velocity = state.liveSession?.lastVelocity,
                                        peakVelocity = state.liveSession?.bestVelocity,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FatigueCard(
                                        fatigue = state.currentFatigue,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // 1RM Predictions
                        if (state.oneRmPredictions.isNotEmpty()) {
                            item {
                                OneRMPredictionsCard(
                                    predictions = state.oneRmPredictions
                                        .groupBy { it.exerciseId }
                                        .mapValues { it.value.maxByOrNull { p -> p.calculatedAt } }
                                        .values
                                        .filterNotNull()
                                )
                            }
                        }

                        // L-V Profile Card
                        viewModel.getSelectedProfile()?.let { profile ->
                            item {
                                LoadVelocityProfileCard(profile = profile)
                            }
                        }

                        // Velocity History
                        if (state.velocityHistory.isNotEmpty()) {
                            item {
                                VelocityHistoryCard(
                                    entries = viewModel.getSelectedVelocityHistory().take(20)
                                )
                            }
                        }

                        // Bottom spacer
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveModeToggle(
    isLive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface.copy(alpha = 0.8f))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // History button
        Surface(
            onClick = { onToggle(false) },
            shape = RoundedCornerShape(16.dp),
            color = if (!isLive) PrometheusOrange else Color.Transparent
        ) {
            Text(
                "History",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (!isLive) Color.White else TextSecondary
            )
        }
        // Live button
        Surface(
            onClick = { onToggle(true) },
            shape = RoundedCornerShape(16.dp),
            color = if (isLive) PrometheusOrange else Color.Transparent
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    "Live",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLive) Color.White else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun LiveModeBanner(session: LiveVBTSession?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrometheusOrange.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "LIVE SESSION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrometheusOrange,
                    letterSpacing = 1.sp
                )
                if (session?.isActive == true) {
                    Text(
                        session.exerciseName ?: "Waiting for data...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                } else {
                    Text(
                        "Waiting for workout to start...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            if (session?.isActive == true) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Set ${session.currentSetNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        "Rep ${session.currentRepNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrometheusOrange
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseSelector(
    exercises: List<ExerciseVBTSummary>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = exercises.find { it.exerciseId == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected?.exerciseName ?: "Select Exercise",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = PrometheusOrange.copy(alpha = 0.5f),
                focusedBorderColor = PrometheusOrange,
                unfocusedContainerColor = DarkSurface.copy(alpha = 0.5f),
                focusedContainerColor = DarkSurface.copy(alpha = 0.7f)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            exercises.forEach { exercise ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(exercise.exerciseName)
                            if (exercise.hasProfile) {
                                Icon(
                                    Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = "Has profile",
                                    modifier = Modifier.size(16.dp),
                                    tint = PrometheusOrange
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelect(exercise.exerciseId)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ReadinessCard(readiness: ClientReadiness) {
    VBTCard(
        title = "READINESS",
        icon = Icons.Default.BatteryChargingFull
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    readiness.readinessLevel.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(android.graphics.Color.parseColor(readiness.indicatorColor))
                )
                Text(
                    readiness.recommendation,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                readiness.formattedDeviation,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(android.graphics.Color.parseColor(readiness.indicatorColor))
            )
        }
    }
}

@Composable
private fun VelocityCard(
    velocity: Double?,
    peakVelocity: Double?,
    modifier: Modifier = Modifier
) {
    VBTCard(
        title = "VELOCITY",
        icon = Icons.Default.Speed,
        modifier = modifier
    ) {
        Column {
            Text(
                velocity?.let { String.format("%.2f", it) } ?: "-",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = PrometheusOrange
            )
            Text(
                "m/s",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            peakVelocity?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = PrometheusOrange
                    )
                    Text(
                        " Peak: ${String.format("%.2f", it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun FatigueCard(
    fatigue: FatigueIndex?,
    modifier: Modifier = Modifier
) {
    VBTCard(
        title = "FATIGUE",
        icon = Icons.Default.Battery4Bar,
        modifier = modifier
    ) {
        if (fatigue != null) {
            Column {
                Text(
                    fatigue.formattedLoss,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(android.graphics.Color.parseColor(fatigue.indicatorColor))
                )
                Text(
                    fatigue.fatigueLevel.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(android.graphics.Color.parseColor(fatigue.indicatorColor))
                )
                if (fatigue.shouldStopSet) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "STOP SET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
            }
        } else {
            Text(
                "-",
                style = MaterialTheme.typography.headlineLarge,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun OneRMPredictionsCard(predictions: List<OneRMPrediction>) {
    VBTCard(
        title = "1RM PREDICTIONS",
        icon = Icons.Default.FitnessCenter
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            predictions.take(5).forEach { prediction ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        prediction.exerciseName ?: prediction.exerciseId,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            prediction.formattedValue,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrometheusOrange
                        )
                        prediction.change?.let { change ->
                            Spacer(modifier = Modifier.width(8.dp))
                            val changeColor = if (change >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                            val changeSign = if (change >= 0) "+" else ""
                            Text(
                                "$changeSign${String.format("%.1f", change)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = changeColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadVelocityProfileCard(profile: LoadVelocityProfile) {
    VBTCard(
        title = "LOAD-VELOCITY PROFILE",
        icon = Icons.AutoMirrored.Filled.TrendingUp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Profile quality
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Quality", color = TextSecondary)
                Text(
                    "${profile.profileQuality} (R² = ${String.format("%.2f", profile.rSquared)})",
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            // MVT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("MVT", color = TextSecondary)
                Text(
                    "${String.format("%.2f", profile.mvt)} m/s",
                    color = PrometheusOrange,
                    fontWeight = FontWeight.Bold
                )
            }

            // Predicted 1RM
            profile.predict1RM()?.let { predicted1RM ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Estimated 1RM", color = TextSecondary)
                    Text(
                        "${String.format("%.1f", predicted1RM)} kg",
                        color = PrometheusOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Data points
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Data points", color = TextSecondary)
                Text("${profile.dataPoints}", color = TextPrimary)
            }
        }
    }
}

@Composable
private fun VelocityHistoryCard(entries: List<VelocityEntry>) {
    VBTCard(
        title = "RECENT VELOCITY",
        icon = Icons.Default.History
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "${String.format("%.1f", entry.loadKg)} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            entry.recordedAt.take(10),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Text(
                        entry.velocityFormatted,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrometheusOrange
                    )
                }
                if (entries.indexOf(entry) < entries.lastIndex) {
                    HorizontalDivider(color = DarkSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun VBTCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
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
        border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrometheusOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }
            content()
        }
    }
}

@Composable
private fun EmptyVBTState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Speed,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = PrometheusOrange.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No VBT Data",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "This client doesn't have any velocity-based training data yet. VBT data is recorded when they use a MOSSE barbell tracker during workouts.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
