package com.prometheuscoach.mobile.ui.screens.clients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prometheuscoach.mobile.ui.theme.DarkSurface
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange
import com.prometheuscoach.mobile.ui.theme.TextPrimary
import com.prometheuscoach.mobile.ui.theme.TextSecondary

/**
 * Bottom sheet for editing client profile data.
 * Allows coaches to update client name and timezone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClientSheet(
    clientName: String,
    currentTimezone: String?,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, timezone: String?) -> Unit
) {
    var name by remember { mutableStateOf(clientName) }
    var timezone by remember { mutableStateOf(currentTimezone ?: "") }
    var showTimezoneDropdown by remember { mutableStateOf(false) }

    val isNameValid = name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edit Client",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Update profile information",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = PrometheusOrange,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error message
            if (error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
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
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Name field
            Text(
                text = "Name",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Client name") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = PrometheusOrange
                    )
                },
                isError = !isNameValid && name.isEmpty(),
                supportingText = if (!isNameValid && name.isEmpty()) {
                    { Text("Name is required") }
                } else null,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrometheusOrange,
                    unfocusedBorderColor = PrometheusOrange.copy(alpha = 0.5f),
                    cursorColor = PrometheusOrange
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Timezone field
            Text(
                text = "Timezone (optional)",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = showTimezoneDropdown,
                onExpandedChange = { showTimezoneDropdown = it }
            ) {
                OutlinedTextField(
                    value = timezone,
                    onValueChange = { timezone = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    placeholder = { Text("Select timezone") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = PrometheusOrange
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTimezoneDropdown)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrometheusOrange,
                        unfocusedBorderColor = PrometheusOrange.copy(alpha = 0.5f),
                        cursorColor = PrometheusOrange
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = showTimezoneDropdown,
                    onDismissRequest = { showTimezoneDropdown = false }
                ) {
                    commonTimezones.forEach { tz ->
                        DropdownMenuItem(
                            text = { Text(tz) },
                            onClick = {
                                timezone = tz
                                showTimezoneDropdown = false
                            },
                            leadingIcon = {
                                if (timezone == tz) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PrometheusOrange
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    )
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onSave(
                            name.trim(),
                            timezone.ifBlank { null }
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isNameValid && !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrometheusOrange
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isSaving) "Saving..." else "Save")
                }
            }
        }
    }
}

/**
 * Common timezone options for dropdown selection.
 */
private val commonTimezones = listOf(
    "Europe/Berlin",
    "Europe/London",
    "Europe/Paris",
    "Europe/Zurich",
    "Europe/Vienna",
    "America/New_York",
    "America/Chicago",
    "America/Denver",
    "America/Los_Angeles",
    "America/Toronto",
    "Asia/Tokyo",
    "Asia/Shanghai",
    "Asia/Dubai",
    "Australia/Sydney",
    "Pacific/Auckland"
)
