package com.prometheuscoach.mobile.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prometheuscoach.mobile.data.model.Exercise
import com.prometheuscoach.mobile.data.model.TechniqueSection
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.*

// ═══════════════════════════════════════════════════════════════
// DESIGN SYSTEM
// ═══════════════════════════════════════════════════════════════

private val vbtPowerColor = Color(0xFFFFD700) // Gold for Power
private val vbtTechniqueColor = Color(0xFF00D4FF) // Cyan for Technique

// ═══════════════════════════════════════════════════════════════
// MAIN SCREEN
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    GradientBackground {
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
                    onRetry = { viewModel.loadExercise(exerciseId) },
                    onBack = onNavigateBack
                )
            }
            state.exercise != null -> {
                ExerciseDetailContent(
                    exercise = state.exercise!!,
                    onBackClick = onNavigateBack
                )
            }
        }
    }
}

@Composable
private fun ExerciseDetailContent(
    exercise: Exercise,
    onBackClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 48.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Exercise Details",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Exercise Name
            Text(
                text = exercise.name,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            // Badges Row
            BadgesSection(exercise)

            // VBT Measurement Capabilities
            if (exercise.vbtEnabled) {
                MeasurementCapabilitiesSection()
            }

            // Muscle Groups Section
            if (exercise.mainMuscleGroup != null || !exercise.secondaryMuscleGroups.isNullOrEmpty()) {
                MuscleGroupsSection(exercise)
            }

            // Equipment Section
            if (!exercise.equipment.isNullOrEmpty()) {
                EquipmentSection(exercise)
            }

            // Prescription Section (Tempo & Rest)
            if (exercise.tempo != null || exercise.restTimeSeconds != null) {
                PrescriptionSection(exercise)
            }

            // Tracking Parameters Section
            TrackingParametersSection(exercise)

            // Tutorial Section
            if (!exercise.tutorial.isNullOrEmpty()) {
                TutorialSection(exercise.tutorial!!)
            }

            // Technique Guide Section
            if (!exercise.techniqueSections.isNullOrEmpty()) {
                TechniqueGuideSections(exercise.techniqueSections!!)
            }

            // Notes Section
            if (!exercise.notes.isNullOrEmpty()) {
                NotesSection(exercise.notes!!)
            }

            // Video Section
            if (!exercise.videoUrl.isNullOrEmpty()) {
                VideoSection(exercise.videoUrl!!)
            }

            // Bottom Spacer
            Spacer(Modifier.height(100.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// BADGES SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun BadgesSection(exercise: Exercise) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Main Muscle Badge
        exercise.mainMuscleGroup?.let { muscle ->
            CategoryChip(label = muscle, color = PrometheusOrange)
        }

        // Category Badge
        exercise.category?.let { category ->
            CategoryChip(label = category, color = DarkSurface)
        }

        // Equipment Badge (first one)
        exercise.equipment?.firstOrNull()?.let { equip ->
            CategoryChip(label = equip, color = DarkSurface)
        }

        // VBT Badge
        if (exercise.vbtEnabled) {
            CategoryChip(label = "VBT", color = vbtPowerColor)
        }
    }
}

@Composable
private fun CategoryChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = if (color == DarkSurface) 0.8f else 0.2f)
    ) {
        Text(
            text = label,
            color = if (color == DarkSurface) Color.White else color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// MEASUREMENT CAPABILITIES SECTION (VBT)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MeasurementCapabilitiesSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(icon = Icons.Default.Speed, title = "MEASUREMENT CAPABILITIES")

            // Power Score
            MeasurementCapabilityCard(
                icon = Icons.Default.Bolt,
                title = "Power Score (VBT)",
                description = "Measures bar velocity and explosive power output",
                color = vbtPowerColor
            )

            // Technique Score
            MeasurementCapabilityCard(
                icon = Icons.Default.CheckCircle,
                title = "Technique Score",
                description = "Analyzes bar path and movement quality",
                color = vbtTechniqueColor
            )

            // VBT Info Note
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PrometheusOrange.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = PrometheusOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Use your phone's camera to track these metrics during your sets",
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MeasurementCapabilityCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon Badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, color)
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Text Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = Gray400,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MUSCLE GROUPS SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MuscleGroupsSection(exercise: Exercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(icon = Icons.Default.FitnessCenter, title = "MUSCLE GROUPS")

            // Primary Muscle
            exercise.mainMuscleGroup?.let { primary ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Primary:", color = Gray400, fontSize = 14.sp, modifier = Modifier.width(80.dp))
                    Text(primary, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Secondary Muscles
            if (!exercise.secondaryMuscleGroups.isNullOrEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Secondary:", color = Gray400, fontSize = 14.sp, modifier = Modifier.width(80.dp))
                    Text(
                        exercise.secondaryMuscleGroups!!.joinToString(", "),
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// EQUIPMENT SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EquipmentSection(exercise: Exercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(icon = Icons.Default.Build, title = "EQUIPMENT")
            Text(
                exercise.equipment?.joinToString(", ") ?: "",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PRESCRIPTION SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PrescriptionSection(exercise: Exercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(icon = Icons.Default.Settings, title = "PRESCRIPTION")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Tempo
                exercise.tempo?.let { tempo ->
                    PrescriptionItem(
                        icon = Icons.Default.Speed,
                        label = "Tempo",
                        value = tempo
                    )
                }

                // Rest Time
                exercise.restTimeSeconds?.let { rest ->
                    PrescriptionItem(
                        icon = Icons.Default.Timer,
                        label = "Rest",
                        value = "${rest}s"
                    )
                }
            }
        }
    }
}

@Composable
private fun PrescriptionItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = PrometheusOrange,
            modifier = Modifier.size(24.dp)
        )
        Text(
            value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = Gray400,
            fontSize = 12.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// TRACKING PARAMETERS SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TrackingParametersSection(exercise: Exercise) {
    val trackingParams = buildList {
        if (exercise.trackSets) add("Sets")
        if (exercise.trackReps) add("Reps")
        if (exercise.trackWeight) add("Weight")
        if (exercise.trackRpe) add("RPE")
    }

    if (trackingParams.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(icon = Icons.Default.CheckCircle, title = "TRACKING")

            // Tracking Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                trackingParams.forEach { param ->
                    TrackingChip(param)
                }
            }
        }
    }
}

@Composable
private fun TrackingChip(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = PrometheusOrange.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = PrometheusOrange,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = PrometheusOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TUTORIAL SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TutorialSection(tutorial: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "HOW TO PERFORM",
            color = PrometheusOrange,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        val steps = tutorial.split("\n").filter { it.isNotBlank() }

        steps.forEachIndexed { index, step ->
            InstructionCard(
                number = index + 1,
                text = step.trim().removePrefix("${index + 1}. ").removePrefix("${index + 1}.").trim()
            )
        }
    }
}

@Composable
private fun InstructionCard(number: Int, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Number Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PrometheusOrange
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Instruction Text
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TECHNIQUE GUIDE SECTIONS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TechniqueGuideSections(sections: List<TechniqueSection>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = vbtTechniqueColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "TECHNIQUE GUIDE",
                color = vbtTechniqueColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }

        // Each technique section as a card
        sections.forEach { section ->
            TechniqueSectionCard(section)
        }
    }
}

@Composable
private fun TechniqueSectionCard(section: TechniqueSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, vbtTechniqueColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Title
            if (section.title.isNotBlank()) {
                Text(
                    text = section.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bullet Points
            section.bullets.forEach { bullet ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Bullet point indicator
                    Surface(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(6.dp),
                        shape = RoundedCornerShape(3.dp),
                        color = vbtTechniqueColor
                    ) {}

                    // Bullet text
                    Text(
                        text = bullet,
                        color = Gray400,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// NOTES SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun NotesSection(notes: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(icon = Icons.Default.Notes, title = "NOTES")
            Text(
                notes,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// VIDEO SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun VideoSection(videoUrl: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(icon = Icons.Default.PlayCircle, title = "VIDEO")

            // Placeholder for video
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(8.dp),
                color = DarkBackgroundSecondary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = PrometheusOrange,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Video Tutorial",
                            color = Gray400,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// UTILITY COMPONENTS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = PrometheusOrange,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            color = PrometheusOrange,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = ErrorRed
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            error,
            color = Gray400,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onBack) {
                Text("Go Back")
            }
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
            ) {
                Text("Retry")
            }
        }
    }
}