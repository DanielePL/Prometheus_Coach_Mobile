package com.prometheuscoach.mobile.ui.screens.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange

/**
 * Data class representing a single set configuration.
 */
data class SetConfig(
    val setNumber: Int,
    val targetReps: Int = 10,
    val targetWeight: Double? = null,
    val restSeconds: Int = 90
)

/**
 * Rest time presets matching the client app.
 */
object RestTimePresets {
    val SHORT = 30 to "30s (Cardio)"
    val MEDIUM = 60 to "1min (Hypertrophy)"
    val STANDARD = 90 to "1.5min (Standard)"
    val LONG = 120 to "2min (Strength)"
    val VERY_LONG = 180 to "3min (Heavy)"
    val MAX_STRENGTH = 240 to "4min (Max Strength)"

    val all = listOf(SHORT, MEDIUM, STANDARD, LONG, VERY_LONG, MAX_STRENGTH)
}

/**
 * Dialog for configuring exercise sets (reps, weight, rest time).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetInputDialog(
    exerciseName: String,
    initialSets: Int,
    initialRepsMin: Int?,
    initialRepsMax: Int?,
    initialRestSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (sets: Int, repsMin: Int?, repsMax: Int?, restSeconds: Int) -> Unit,
    isSaving: Boolean = false
) {
    // Initialize sets list
    var sets by remember {
        mutableStateOf(
            List(initialSets) { index ->
                SetConfig(
                    setNumber = index + 1,
                    targetReps = initialRepsMin ?: 10,
                    restSeconds = initialRestSeconds
                )
            }
        )
    }

    // Use rep range or single value
    var useRepRange by remember { mutableStateOf(initialRepsMax != null && initialRepsMax != initialRepsMin) }
    var repsMin by remember { mutableStateOf(initialRepsMin ?: 10) }
    var repsMax by remember { mutableStateOf(initialRepsMax ?: (initialRepsMin ?: 10)) }
    var restSeconds by remember { mutableStateOf(initialRestSeconds) }

    // Rest time picker dialog
    var showRestTimePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Configure Exercise",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Exercise name
                Text(
                    exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = PrometheusOrange,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Sets count
                SetsCountSection(
                    setsCount = sets.size,
                    onAddSet = {
                        sets = sets + SetConfig(
                            setNumber = sets.size + 1,
                            targetReps = repsMin,
                            restSeconds = restSeconds
                        )
                    },
                    onRemoveSet = {
                        if (sets.size > 1) {
                            sets = sets.dropLast(1)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Reps configuration
                RepsSection(
                    repsMin = repsMin,
                    repsMax = repsMax,
                    useRepRange = useRepRange,
                    onRepsMinChange = { repsMin = it },
                    onRepsMaxChange = { repsMax = it },
                    onUseRepRangeChange = { useRepRange = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Rest time
                RestTimeSection(
                    restSeconds = restSeconds,
                    onClick = { showRestTimePicker = true }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Summary
                SummaryCard(
                    sets = sets.size,
                    repsMin = repsMin,
                    repsMax = if (useRepRange) repsMax else null,
                    restSeconds = restSeconds
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onConfirm(
                                sets.size,
                                repsMin,
                                if (useRepRange) repsMax else null,
                                restSeconds
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // Rest Time Picker Dialog
    if (showRestTimePicker) {
        RestTimePickerDialog(
            currentRestSeconds = restSeconds,
            onDismiss = { showRestTimePicker = false },
            onSelect = { seconds ->
                restSeconds = seconds
                showRestTimePicker = false
            }
        )
    }
}

@Composable
private fun SetsCountSection(
    setsCount: Int,
    onAddSet: () -> Unit,
    onRemoveSet: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "SETS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$setsCount sets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = onRemoveSet,
                    enabled = setsCount > 1,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Remove set")
                }

                Text(
                    "$setsCount",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )

                FilledIconButton(
                    onClick = onAddSet,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = PrometheusOrange
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add set",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun RepsSection(
    repsMin: Int,
    repsMax: Int,
    useRepRange: Boolean,
    onRepsMinChange: (Int) -> Unit,
    onRepsMaxChange: (Int) -> Unit,
    onUseRepRangeChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
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
                Text(
                    "REPS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Range",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = useRepRange,
                        onCheckedChange = onUseRepRangeChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = PrometheusOrange)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Min reps
                RepsInput(
                    value = repsMin,
                    onValueChange = onRepsMinChange,
                    label = if (useRepRange) "Min" else "Reps"
                )

                if (useRepRange) {
                    Text(
                        " - ",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    RepsInput(
                        value = repsMax,
                        onValueChange = onRepsMaxChange,
                        label = "Max"
                    )
                }
            }
        }
    }
}

@Composable
private fun RepsInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilledIconButton(
                onClick = { if (value > 1) onValueChange(value - 1) },
                enabled = value > 1,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
            }

            Text(
                "$value",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.Center
            )

            FilledIconButton(
                onClick = { onValueChange(value + 1) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = PrometheusOrange
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Increase",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun RestTimeSection(
    restSeconds: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "REST TIME",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatRestTime(restSeconds),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                tint = PrometheusOrange
            )
        }
    }
}

@Composable
private fun SummaryCard(
    sets: Int,
    repsMin: Int,
    repsMax: Int?,
    restSeconds: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrometheusOrange.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem(
                icon = Icons.Default.Repeat,
                value = "$sets",
                label = "Sets"
            )
            SummaryItem(
                icon = Icons.Default.FitnessCenter,
                value = if (repsMax != null) "$repsMin-$repsMax" else "$repsMin",
                label = "Reps"
            )
            SummaryItem(
                icon = Icons.Default.Timer,
                value = formatRestTimeShort(restSeconds),
                label = "Rest"
            )
        }
    }
}

@Composable
private fun SummaryItem(
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
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RestTimePickerDialog(
    currentRestSeconds: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Rest Time") },
        text = {
            LazyColumn {
                itemsIndexed(RestTimePresets.all) { _, (seconds, label) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        leadingContent = {
                            RadioButton(
                                selected = seconds == currentRestSeconds,
                                onClick = { onSelect(seconds) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PrometheusOrange
                                )
                            )
                        },
                        modifier = Modifier.clickable { onSelect(seconds) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatRestTime(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds % 60 == 0 -> "${seconds / 60}min"
        else -> "${seconds / 60}min ${seconds % 60}s"
    }
}

private fun formatRestTimeShort(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds % 60 == 0 -> "${seconds / 60}m"
        else -> "${seconds / 60}.${(seconds % 60) / 6}m"
    }
}
