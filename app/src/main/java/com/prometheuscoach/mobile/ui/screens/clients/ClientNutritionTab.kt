package com.prometheuscoach.mobile.ui.screens.clients

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.min

// Macro colors
val ProteinGreen = Color(0xFF22C55E)
val CarbsBlue = Color(0xFF3B82F6)
val FatYellow = Color(0xFFF59E0B)
val CalorieOrange = Color(0xFFF97316)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientNutritionTab(
    clientId: String,
    clientName: String,
    viewModel: ClientNutritionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showGoalSheet by remember { mutableStateOf(false) }

    LaunchedEffect(clientId) {
        viewModel.loadNutrition(clientId)
    }

    // Show success message when goal is saved
    LaunchedEffect(state.goalSaved) {
        if (state.goalSaved) {
            viewModel.clearGoalSavedFlag()
            showGoalSheet = false
        }
    }

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
                    Text(state.error!!)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refresh(clientId) }) {
                        Text("Retry")
                    }
                }
            }
        }

        state.summaries.isEmpty() -> {
            // No nutrition data yet
            EmptyNutritionState(
                onSetGoals = { showGoalSheet = true }
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Weekly Summary Stats
                item {
                    WeeklySummarySection(
                        weeklySummary = state.weeklySummary,
                        goal = state.goal,
                        isLoading = state.isLoadingWeekly
                    )
                }

                // Today's Progress
                val todaySummary = viewModel.getTodaySummary()
                if (todaySummary != null) {
                    item {
                        TodayProgressCard(summary = todaySummary)
                    }
                }

                // Macro Distribution
                if (todaySummary != null && todaySummary.totalCalories > 0) {
                    item {
                        MacroDistributionCard(summary = todaySummary)
                    }
                }

                // Calorie Trend
                item {
                    CalorieTrendCard(
                        trendData = viewModel.getCalorieTrendData(),
                        goal = state.goal,
                        onEditGoals = { showGoalSheet = true }
                    )
                }

                // Current Goals
                if (state.goal != null) {
                    item {
                        NutritionGoalsCard(goal = state.goal!!)
                    }
                }

                // Recent Logs
                item {
                    RecentLogsSection(
                        logs = viewModel.getRecentLogs(),
                        goal = state.goal
                    )
                }
            }
        }
    }

    // Set Goals Sheet
    if (showGoalSheet) {
        SetNutritionGoalsSheet(
            clientName = clientName,
            currentGoal = state.goal,
            isSaving = state.isSavingGoal,
            error = state.goalSaveError,
            onDismiss = { showGoalSheet = false },
            onSave = { goalType, calories, protein, carbs, fat ->
                viewModel.setNutritionGoal(
                    clientId = clientId,
                    goalType = goalType,
                    targetCalories = calories,
                    targetProtein = protein,
                    targetCarbs = carbs,
                    targetFat = fat
                )
            }
        )
    }
}

@Composable
private fun EmptyNutritionState(onSetGoals: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Restaurant,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = PrometheusOrange.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Nutrition Data Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "This client hasn't logged any meals yet. Set their nutrition goals to help them get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onSetGoals,
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
            ) {
                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set Nutrition Goals")
            }
        }
    }
}

@Composable
private fun WeeklySummarySection(
    weeklySummary: WeeklyNutritionSummary?,
    goal: NutritionGoal?,
    isLoading: Boolean
) {
    if (isLoading) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(4) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrometheusOrange,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
        return
    }

    if (weeklySummary == null || weeklySummary.daysLogged == 0) return

    val stats = listOf(
        StatItem(Icons.Default.LocalFireDepartment, "Avg Calories", "${weeklySummary.avgCalories}", "${weeklySummary.daysLogged} days", CalorieOrange),
        StatItem(Icons.Default.FitnessCenter, "Avg Protein", "${weeklySummary.avgProtein}g", goal?.let { "${(weeklySummary.avgProtein * 100 / it.targetProtein).toInt()}% of target" } ?: "No target", ProteinGreen),
        StatItem(Icons.Default.Grain, "Avg Carbs", "${weeklySummary.avgCarbs}g", goal?.let { "${(weeklySummary.avgCarbs * 100 / it.targetCarbs).toInt()}% of target" } ?: "No target", CarbsBlue),
        StatItem(Icons.Default.WaterDrop, "Avg Fat", "${weeklySummary.avgFat}g", goal?.let { "${(weeklySummary.avgFat * 100 / it.targetFat).toInt()}% of target" } ?: "No target", FatYellow)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stats) { stat ->
            StatCard(stat = stat)
        }
    }
}

private data class StatItem(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val subValue: String,
    val color: Color
)

@Composable
private fun StatCard(stat: StatItem) {
    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = androidx.compose.foundation.BorderStroke(1.dp, stat.color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(stat.color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        stat.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = stat.color
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stat.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stat.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                stat.subValue,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun TodayProgressCard(summary: DailyNutritionSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrometheusOrange)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrometheusOrange.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Today,
                        contentDescription = null,
                        tint = PrometheusOrange
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Today's Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "${summary.mealsCount} meals logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calories
            MacroProgressBar(
                label = "Calories",
                current = summary.totalCalories,
                target = summary.targetCalories,
                progress = summary.calorieProgress,
                color = CalorieOrange,
                unit = ""
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Protein
            MacroProgressBar(
                label = "Protein",
                current = summary.totalProtein,
                target = summary.targetProtein,
                progress = summary.proteinProgress,
                color = ProteinGreen,
                unit = "g"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Carbs
            MacroProgressBar(
                label = "Carbs",
                current = summary.totalCarbs,
                target = summary.targetCarbs,
                progress = summary.carbsProgress,
                color = CarbsBlue,
                unit = "g"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fat
            MacroProgressBar(
                label = "Fat",
                current = summary.totalFat,
                target = summary.targetFat,
                progress = summary.fatProgress,
                color = FatYellow,
                unit = "g"
            )
        }
    }
}

@Composable
private fun MacroProgressBar(
    label: String,
    current: Float,
    target: Float?,
    progress: Float,
    color: Color,
    unit: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = color
            )
            Text(
                "${current.toInt()}$unit / ${target?.toInt() ?: "–"}$unit",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { min(progress / 100f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun MacroDistributionCard(summary: DailyNutritionSummary) {
    val proteinCals = summary.totalProtein * 4
    val carbsCals = summary.totalCarbs * 4
    val fatCals = summary.totalFat * 9
    val totalCals = proteinCals + carbsCals + fatCals

    if (totalCals <= 0) return

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrometheusOrange.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PieChart,
                        contentDescription = null,
                        tint = PrometheusOrange
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Macro Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Today's breakdown",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pie Chart
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MacroPieChart(
                        protein = proteinCals / totalCals,
                        carbs = carbsCals / totalCals,
                        fat = fatCals / totalCals
                    )
                    Text(
                        "${totalCals.toInt()}\nkcal",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroLegendItem(
                        color = ProteinGreen,
                        label = "Protein",
                        value = "${summary.totalProtein.toInt()}g"
                    )
                    MacroLegendItem(
                        color = CarbsBlue,
                        label = "Carbs",
                        value = "${summary.totalCarbs.toInt()}g"
                    )
                    MacroLegendItem(
                        color = FatYellow,
                        label = "Fat",
                        value = "${summary.totalFat.toInt()}g"
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroPieChart(protein: Float, carbs: Float, fat: Float) {
    Canvas(modifier = Modifier.size(120.dp)) {
        val strokeWidth = 20f
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        var startAngle = -90f

        // Protein
        drawArc(
            color = ProteinGreen,
            startAngle = startAngle,
            sweepAngle = protein * 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += protein * 360f

        // Carbs
        drawArc(
            color = CarbsBlue,
            startAngle = startAngle,
            sweepAngle = carbs * 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += carbs * 360f

        // Fat
        drawArc(
            color = FatYellow,
            startAngle = startAngle,
            sweepAngle = fat * 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun MacroLegendItem(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "$label: $value",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
    }
}

@Composable
private fun CalorieTrendCard(
    trendData: List<DailyNutritionSummary>,
    goal: NutritionGoal?,
    onEditGoals: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrometheusOrange.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = PrometheusOrange
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Calorie Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Last 14 days",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                OutlinedButton(
                    onClick = onEditGoals,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrometheusOrange),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (goal != null) "Edit Goals" else "Set Goals", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (trendData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Not enough data for trend chart",
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            } else {
                // Simple bar chart
                SimpleCalorieChart(
                    data = trendData,
                    targetCalories = goal?.targetCalories
                )
            }
        }
    }
}

@Composable
private fun SimpleCalorieChart(
    data: List<DailyNutritionSummary>,
    targetCalories: Float?
) {
    val maxCalories = maxOf(
        data.maxOfOrNull { it.totalCalories } ?: 0f,
        targetCalories ?: 0f,
        1f
    )

    Column {
        // Chart area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { day ->
                val heightFraction = day.totalCalories / maxCalories
                val isOnTarget = targetCalories?.let {
                    day.totalCalories >= it * 0.9f && day.totalCalories <= it * 1.1f
                } ?: false

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .fillMaxHeight(heightFraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (isOnTarget) ProteinGreen else PrometheusOrange
                                )
                        )
                    }
                }
            }
        }

        // Date labels
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            data.forEach { day ->
                val date = try {
                    LocalDate.parse(day.date).format(DateTimeFormatter.ofPattern("d"))
                } catch (e: Exception) {
                    ""
                }
                Text(
                    text = date,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Target line indicator
        if (targetCalories != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Target: ${targetCalories.toInt()} kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun NutritionGoalsCard(goal: NutritionGoal) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrometheusOrange.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        tint = PrometheusOrange
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Nutrition Goals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PrometheusOrange.copy(alpha = 0.2f)
                    ) {
                        Text(
                            goal.goalType.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = PrometheusOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GoalValueCard(
                    modifier = Modifier.weight(1f),
                    label = "Calories",
                    value = "${goal.targetCalories.toInt()}",
                    color = CalorieOrange
                )
                GoalValueCard(
                    modifier = Modifier.weight(1f),
                    label = "Protein",
                    value = "${goal.targetProtein.toInt()}g",
                    color = ProteinGreen
                )
                GoalValueCard(
                    modifier = Modifier.weight(1f),
                    label = "Carbs",
                    value = "${goal.targetCarbs.toInt()}g",
                    color = CarbsBlue
                )
                GoalValueCard(
                    modifier = Modifier.weight(1f),
                    label = "Fat",
                    value = "${goal.targetFat.toInt()}g",
                    color = FatYellow
                )
            }
        }
    }
}

@Composable
private fun GoalValueCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun RecentLogsSection(
    logs: List<NutritionLog>,
    goal: NutritionGoal?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrometheusOrange.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = PrometheusOrange
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Recent Logs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Last ${logs.size} days",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (logs.isEmpty()) {
                Text(
                    "No recent logs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            } else {
                logs.forEach { log ->
                    RecentLogItem(log = log, targetCalories = goal?.targetCalories)
                    if (log != logs.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentLogItem(
    log: NutritionLog,
    targetCalories: Float?
) {
    val allItems = log.meals?.flatMap { it.mealItems ?: emptyList() } ?: emptyList()
    val dayTotal = allItems.sumOf { it.calories.toDouble() }.toFloat()
    val mealsCount = log.meals?.size ?: 0

    val isOnTarget = targetCalories?.let {
        dayTotal >= it * 0.9f && dayTotal <= it * 1.1f
    } ?: false

    val dateFormatted = try {
        LocalDate.parse(log.date).format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    } catch (e: Exception) {
        log.date
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    dateFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    "$mealsCount meals logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${dayTotal.toInt()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (targetCalories != null) {
                    val percentage = (dayTotal / targetCalories * 100).toInt()
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isOnTarget) ProteinGreen.copy(alpha = 0.2f) else FatYellow.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isOnTarget) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = ProteinGreen
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            Text(
                                "$percentage% of target",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOnTarget) ProteinGreen else FatYellow
                            )
                        }
                    }
                }
            }
        }
    }
}
