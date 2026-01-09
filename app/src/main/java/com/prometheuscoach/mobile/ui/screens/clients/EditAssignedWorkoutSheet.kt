package com.prometheuscoach.mobile.ui.screens.clients

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prometheuscoach.mobile.data.model.AssignedExerciseInfo
import com.prometheuscoach.mobile.data.model.AssignedWorkout
import com.prometheuscoach.mobile.data.model.ExerciseSetInfo
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange

// Design System - Matching V1 Glassmorphism Style
private val orangePrimary = PrometheusOrange
private val orangeGlow = Color(0xFFFF8C42)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.4f)
private val cardBorder = orangeGlow.copy(alpha = 0.5f)
private val dialogBackground = Color(0xFF1a1410).copy(alpha = 0.95f)

/**
 * Mutable set info for editing.
 */
data class EditableSetInfo(
    val id: String,
    val setNumber: Int,
    var targetReps: Int,
    var targetWeight: Double,
    var restSeconds: Int,
    val isNew: Boolean = false // For newly added sets
)

/**
 * Dialog for viewing and editing an assigned workout.
 * V1 compatible design with full exercise and set details.
 */
@Composable
fun EditAssignedWorkoutSheet(
    assignment: AssignedWorkout,
    isUpdating: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (notes: String?, scheduledDate: String?, status: String, exerciseSets: Map<String, List<EditableSetInfo>>) -> Unit,
    onEditWorkout: (workoutId: String) -> Unit
) {
    var notes by remember(assignment) { mutableStateOf(assignment.notes ?: "") }
    var scheduledDate by remember(assignment) { mutableStateOf(assignment.scheduledDate ?: "") }
    var selectedStatus by remember(assignment) { mutableStateOf(assignment.status) }
    var showStatusMenu by remember { mutableStateOf(false) }

    // Editable sets state - keyed by workoutExerciseId
    val editableSets = remember(assignment) {
        mutableStateMapOf<String, MutableList<EditableSetInfo>>().apply {
            assignment.exercises.forEach { exercise ->
                this[exercise.workoutExerciseId] = exercise.sets.map { set ->
                    EditableSetInfo(
                        id = set.id,
                        setNumber = set.setNumber,
                        targetReps = set.targetReps,
                        targetWeight = set.targetWeight,
                        restSeconds = set.restSeconds
                    )
                }.toMutableList()
            }
        }
    }

    // Track expanded exercises
    var expandedExerciseId by remember { mutableStateOf<String?>(null) }

    val totalSets = editableSets.values.sumOf { it.size }
    val statusColors = mapOf(
        "active" to orangePrimary,
        "completed" to Color(0xFF4CAF50),
        "cancelled" to Color.Gray
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            colors = CardDefaults.cardColors(containerColor = dialogBackground),
            border = BorderStroke(1.dp, cardBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = assignment.workoutName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = textWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        assignment.workoutDescription?.let { desc ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = textGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textGray
                        )
                    }
                }

                HorizontalDivider(color = cardBorder)

                // Exercise List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stats Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = orangeGlow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${assignment.exercises.size} Exercises",
                                    color = textGray,
                                    fontSize = 14.sp
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Layers,
                                    contentDescription = null,
                                    tint = orangeGlow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "$totalSets Total Sets",
                                    color = orangeGlow,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Hint
                    item {
                        Text(
                            text = "Tap an exercise to edit sets",
                            color = textGray.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    // Exercise Cards with edit mode
                    items(assignment.exercises.sortedBy { it.orderIndex }) { exercise ->
                        val sets = editableSets[exercise.workoutExerciseId] ?: mutableListOf()
                        val isExpanded = expandedExerciseId == exercise.workoutExerciseId

                        EditableExerciseCard(
                            exercise = exercise,
                            sets = sets,
                            isExpanded = isExpanded,
                            onExpandToggle = {
                                expandedExerciseId = if (isExpanded) null else exercise.workoutExerciseId
                            },
                            onSetChange = { index, updatedSet ->
                                editableSets[exercise.workoutExerciseId]?.let { list ->
                                    if (index < list.size) {
                                        list[index] = updatedSet
                                    }
                                }
                            },
                            onAddSet = {
                                editableSets[exercise.workoutExerciseId]?.let { list ->
                                    val lastSet = list.lastOrNull()
                                    val newSetNumber = (lastSet?.setNumber ?: 0) + 1
                                    list.add(
                                        EditableSetInfo(
                                            id = "new_${System.currentTimeMillis()}",
                                            setNumber = newSetNumber,
                                            targetReps = lastSet?.targetReps ?: 10,
                                            targetWeight = lastSet?.targetWeight ?: 0.0,
                                            restSeconds = lastSet?.restSeconds ?: 90,
                                            isNew = true
                                        )
                                    )
                                }
                            },
                            onRemoveSet = { index ->
                                editableSets[exercise.workoutExerciseId]?.let { list ->
                                    if (list.size > 1 && index < list.size) {
                                        list.removeAt(index)
                                        // Renumber sets
                                        list.forEachIndexed { i, set ->
                                            list[i] = set.copy(setNumber = i + 1)
                                        }
                                    }
                                }
                            }
                        )
                    }

                    // Status Selector
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.labelLarge,
                            color = textWhite,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("active", "completed", "cancelled").forEach { status ->
                                val isSelected = selectedStatus == status
                                val color = statusColors[status] ?: orangePrimary
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedStatus = status },
                                    label = {
                                        Text(
                                            text = status.replaceFirstChar { it.uppercase() },
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            when (status) {
                                                "completed" -> Icons.Default.CheckCircle
                                                "cancelled" -> Icons.Default.Cancel
                                                else -> Icons.Default.PlayArrow
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = color.copy(alpha = 0.2f),
                                        selectedLabelColor = color,
                                        selectedLeadingIconColor = color
                                    ),
                                    border = if (isSelected) BorderStroke(1.dp, color) else null
                                )
                            }
                        }
                    }

                    // Scheduled Date
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scheduled Date",
                            style = MaterialTheme.typography.labelLarge,
                            color = textWhite,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = scheduledDate,
                            onValueChange = { scheduledDate = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("YYYY-MM-DD (optional)") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = orangePrimary
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = orangePrimary,
                                unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                                focusedTextColor = textWhite,
                                unfocusedTextColor = textWhite
                            ),
                            singleLine = true
                        )
                    }

                    // Notes
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notes for Client",
                            style = MaterialTheme.typography.labelLarge,
                            color = textWhite,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Add notes or instructions...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = orangePrimary,
                                unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                                focusedTextColor = textWhite,
                                unfocusedTextColor = textWhite
                            ),
                            minLines = 2,
                            maxLines = 4
                        )
                    }

                    // Error message
                    if (error != null) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                HorizontalDivider(color = cardBorder)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Save Button
                    Button(
                        onClick = {
                            onSave(
                                notes.takeIf { it.isNotBlank() },
                                scheduledDate.takeIf { it.isNotBlank() },
                                selectedStatus,
                                editableSets.mapValues { it.value.toList() }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isUpdating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = orangePrimary,
                            contentColor = textWhite
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = textWhite,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isUpdating) "Saving..." else "Save Changes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Editable exercise card with collapsible set editing (V1 style).
 */
@Composable
private fun EditableExerciseCard(
    exercise: AssignedExerciseInfo,
    sets: List<EditableSetInfo>,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onSetChange: (index: Int, set: EditableSetInfo) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (index: Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) cardBackground.copy(alpha = 0.6f) else cardBackground
        ),
        border = BorderStroke(
            1.dp,
            if (isExpanded) orangePrimary else orangePrimary.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandToggle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Exercise Name
                    Text(
                        text = exercise.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textWhite
                    )

                    // Muscle Group & Equipment
                    if (exercise.muscleGroup != null || exercise.equipment != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = listOfNotNull(exercise.muscleGroup, exercise.equipment)
                                .joinToString(" • "),
                            color = orangeGlow,
                            fontSize = 12.sp
                        )
                    }
                }

                // Expand indicator
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand to edit",
                    tint = orangePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Collapsed view - shows summary
            if (!isExpanded && sets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${sets.size} sets • ${sets.sumOf { it.targetReps }} total reps",
                    color = textGray,
                    fontSize = 13.sp
                )
            }

            // Expanded view - editable sets
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Set editing rows
                    sets.forEachIndexed { index, set ->
                        SetEditRow(
                            setNumber = set.setNumber,
                            set = set,
                            onSetChange = { updatedSet -> onSetChange(index, updatedSet) },
                            onRemove = if (sets.size > 1) {
                                { onRemoveSet(index) }
                            } else null
                        )
                        if (index < sets.size - 1) {
                            HorizontalDivider(
                                color = orangePrimary.copy(alpha = 0.2f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // Add Set button
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onAddSet,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = orangePrimary
                        ),
                        border = BorderStroke(1.dp, orangePrimary.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Set")
                    }
                }
            }
        }
    }
}

/**
 * V1-style row for editing a single set.
 * Only shows Reps and Weight - rest timer is at workout level.
 */
@Composable
private fun SetEditRow(
    setNumber: Int,
    set: EditableSetInfo,
    onSetChange: (EditableSetInfo) -> Unit,
    onRemove: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Set number
        Text(
            text = "Set $setNumber",
            color = textWhite,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.width(48.dp)
        )

        // Reps input
        CompactNumberField(
            value = set.targetReps.toString(),
            onValueChange = { value ->
                value.toIntOrNull()?.let { reps ->
                    onSetChange(set.copy(targetReps = reps))
                }
            },
            label = "Reps",
            modifier = Modifier.weight(1f)
        )

        // Weight input
        CompactNumberField(
            value = if (set.targetWeight > 0) formatWeight(set.targetWeight) else "",
            onValueChange = { value ->
                val weight = value.toDoubleOrNull() ?: 0.0
                onSetChange(set.copy(targetWeight = weight))
            },
            label = "kg",
            modifier = Modifier.weight(1f),
            allowDecimal = true
        )

        // Remove button
        if (onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove set",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(32.dp))
        }
    }
}

/**
 * Compact text field for numeric input.
 */
@Composable
private fun CompactNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    allowDecimal: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Filter to only valid numeric characters
            val filtered = if (allowDecimal) {
                newValue.filter { it.isDigit() || it == '.' }
            } else {
                newValue.filter { it.isDigit() }
            }
            onValueChange(filtered)
        },
        modifier = modifier.height(56.dp),
        label = { Text(label, fontSize = 10.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = orangePrimary,
            unfocusedBorderColor = textGray.copy(alpha = 0.3f),
            focusedTextColor = textWhite,
            unfocusedTextColor = textWhite,
            focusedLabelColor = orangePrimary,
            unfocusedLabelColor = textGray
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
    )
}

private fun formatWeight(weight: Double): String {
    return if (weight % 1.0 == 0.0) {
        weight.toInt().toString()
    } else {
        weight.toString()
    }
}
