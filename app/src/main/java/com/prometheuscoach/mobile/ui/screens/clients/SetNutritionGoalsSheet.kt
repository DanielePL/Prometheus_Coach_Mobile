package com.prometheuscoach.mobile.ui.screens.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.prometheuscoach.mobile.data.model.NutritionGoal
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetNutritionGoalsSheet(
    clientName: String,
    currentGoal: NutritionGoal?,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (goalType: String, calories: Float, protein: Float, carbs: Float, fat: Float) -> Unit
) {
    var selectedGoalType by remember { mutableStateOf(currentGoal?.goalType ?: "maintenance") }
    var targetCalories by remember { mutableStateOf(currentGoal?.targetCalories?.toInt()?.toString() ?: "2500") }
    var targetProtein by remember { mutableStateOf(currentGoal?.targetProtein?.toInt()?.toString() ?: "180") }
    var targetCarbs by remember { mutableStateOf(currentGoal?.targetCarbs?.toInt()?.toString() ?: "250") }
    var targetFat by remember { mutableStateOf(currentGoal?.targetFat?.toInt()?.toString() ?: "80") }

    val goalTypes = listOf(
        GoalTypeOption("cutting", "Cutting", "Caloric deficit for fat loss"),
        GoalTypeOption("maintenance", "Maintenance", "Maintain current weight"),
        GoalTypeOption("bulking", "Bulking", "Caloric surplus for muscle gain"),
        GoalTypeOption("performance", "Performance", "Optimized for athletic performance")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                "Set Nutrition Goals",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "for $clientName",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Goal Type Selection
            Text(
                "Goal Type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            goalTypes.forEach { option ->
                GoalTypeCard(
                    option = option,
                    isSelected = selectedGoalType == option.id,
                    onClick = { selectedGoalType = option.id }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Target Calories
            Text(
                "Target Calories",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = targetCalories,
                onValueChange = { targetCalories = it.filter { c -> c.isDigit() } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("2500") },
                suffix = { Text("kcal", color = Color.White.copy(alpha = 0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrometheusOrange,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Macros
            Text(
                "Macronutrients",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Protein
                OutlinedTextField(
                    value = targetProtein,
                    onValueChange = { targetProtein = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.weight(1f),
                    label = { Text("Protein", color = ProteinGreen) },
                    suffix = { Text("g", color = Color.White.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProteinGreen,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = ProteinGreen
                    )
                )

                // Carbs
                OutlinedTextField(
                    value = targetCarbs,
                    onValueChange = { targetCarbs = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.weight(1f),
                    label = { Text("Carbs", color = CarbsBlue) },
                    suffix = { Text("g", color = Color.White.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CarbsBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = CarbsBlue
                    )
                )

                // Fat
                OutlinedTextField(
                    value = targetFat,
                    onValueChange = { targetFat = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.weight(1f),
                    label = { Text("Fat", color = FatYellow) },
                    suffix = { Text("g", color = Color.White.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FatYellow,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = FatYellow
                    )
                )
            }

            // Macro calories preview
            val proteinCals = (targetProtein.toIntOrNull() ?: 0) * 4
            val carbsCals = (targetCarbs.toIntOrNull() ?: 0) * 4
            val fatCals = (targetFat.toIntOrNull() ?: 0) * 9
            val totalMacroCals = proteinCals + carbsCals + fatCals
            val targetCals = targetCalories.toIntOrNull() ?: 0

            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (kotlin.math.abs(totalMacroCals - targetCals) <= 50)
                        ProteinGreen.copy(alpha = 0.1f)
                    else
                        FatYellow.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Macros sum to:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        "$totalMacroCals kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (kotlin.math.abs(totalMacroCals - targetCals) <= 50)
                            ProteinGreen
                        else
                            FatYellow
                    )
                }
            }

            // Error message
            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    onSave(
                        selectedGoalType,
                        targetCalories.toFloatOrNull() ?: 2500f,
                        targetProtein.toFloatOrNull() ?: 180f,
                        targetCarbs.toFloatOrNull() ?: 250f,
                        targetFat.toFloatOrNull() ?: 80f
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && targetCalories.isNotEmpty() && targetProtein.isNotEmpty() && targetCarbs.isNotEmpty() && targetFat.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrometheusOrange
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Goals")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cancel Button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Text("Cancel")
            }
        }
    }
}

private data class GoalTypeOption(
    val id: String,
    val title: String,
    val description: String
)

@Composable
private fun GoalTypeCard(
    option: GoalTypeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrometheusOrange.copy(alpha = 0.2f) else Color.Transparent
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) PrometheusOrange else Color.White.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    option.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) PrometheusOrange else Color.White
                )
                Text(
                    option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = PrometheusOrange,
                    unselectedColor = Color.White.copy(alpha = 0.5f)
                )
            )
        }
    }
}
