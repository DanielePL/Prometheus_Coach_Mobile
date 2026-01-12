package com.prometheuscoach.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prometheuscoach.mobile.data.model.FeedbackType
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetaFeedbackSheet(
    screenName: String,
    isSubmitting: Boolean,
    onSubmit: (FeedbackType, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(FeedbackType.FEEDBACK) }
    var message by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.Gray, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Feedback,
                    contentDescription = null,
                    tint = PrometheusOrange,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Beta Feedback",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Help us improve Prometheus Coach",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Feedback Type Selection
            Text(
                text = "What kind of feedback?",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeedbackTypeChip(
                    type = FeedbackType.BUG,
                    icon = Icons.Default.BugReport,
                    label = "Bug",
                    isSelected = selectedType == FeedbackType.BUG,
                    onClick = { selectedType = FeedbackType.BUG },
                    modifier = Modifier.weight(1f)
                )
                FeedbackTypeChip(
                    type = FeedbackType.FEEDBACK,
                    icon = Icons.Default.ChatBubble,
                    label = "Feedback",
                    isSelected = selectedType == FeedbackType.FEEDBACK,
                    onClick = { selectedType = FeedbackType.FEEDBACK },
                    modifier = Modifier.weight(1f)
                )
                FeedbackTypeChip(
                    type = FeedbackType.IDEA,
                    icon = Icons.Default.Lightbulb,
                    label = "Idea",
                    isSelected = selectedType == FeedbackType.IDEA,
                    onClick = { selectedType = FeedbackType.IDEA },
                    modifier = Modifier.weight(1f)
                )
            }

            // Message Input
            Text(
                text = "Your message",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                placeholder = {
                    Text(
                        when (selectedType) {
                            FeedbackType.BUG -> "Describe the bug and how to reproduce it..."
                            FeedbackType.FEEDBACK -> "Share your thoughts about the app..."
                            FeedbackType.IDEA -> "Tell us about your idea..."
                        },
                        color = Color.Gray
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrometheusOrange,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = PrometheusOrange
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 6
            )

            // Screen context info
            Text(
                text = "Sending from: $screenName",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = { onSubmit(selectedType, message) },
                enabled = message.isNotBlank() && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrometheusOrange,
                    disabledContainerColor = PrometheusOrange.copy(alpha = 0.4f)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Send Feedback",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackTypeChip(
    type: FeedbackType,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        when (type) {
            FeedbackType.BUG -> Color(0xFF8B0000).copy(alpha = 0.3f)
            FeedbackType.FEEDBACK -> PrometheusOrange.copy(alpha = 0.3f)
            FeedbackType.IDEA -> Color(0xFF1E90FF).copy(alpha = 0.3f)
        }
    } else {
        Color(0xFF2A2A2A)
    }

    val borderColor = if (isSelected) {
        when (type) {
            FeedbackType.BUG -> Color(0xFFFF4444)
            FeedbackType.FEEDBACK -> PrometheusOrange
            FeedbackType.IDEA -> Color(0xFF1E90FF)
        }
    } else {
        Color.Transparent
    }

    val iconTint = when (type) {
        FeedbackType.BUG -> Color(0xFFFF4444)
        FeedbackType.FEEDBACK -> PrometheusOrange
        FeedbackType.IDEA -> Color(0xFF1E90FF)
    }

    Surface(
        modifier = modifier
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) Color.White else Color.Gray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
