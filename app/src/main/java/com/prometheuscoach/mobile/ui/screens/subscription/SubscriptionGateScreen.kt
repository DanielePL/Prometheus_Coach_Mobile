package com.prometheuscoach.mobile.ui.screens.subscription

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prometheuscoach.mobile.data.model.SubscriptionStatus
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.*

// Web tool URL for subscription management
private const val WEB_TOOL_URL = "https://coach.prometheusapp.io"
private const val PRICING_URL = "https://coach.prometheusapp.io/pricing"

@Composable
fun SubscriptionGateScreen(
    viewModel: SubscriptionViewModel = hiltViewModel(),
    onRetry: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    GradientBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> {
                    LoadingContent()
                }
                state.error != null -> {
                    ErrorContent(
                        error = state.error!!,
                        onRetry = {
                            viewModel.checkSubscription()
                            onRetry()
                        }
                    )
                }
                else -> {
                    SubscriptionContent(
                        status = state.subscriptionInfo.status,
                        title = viewModel.getStatusTitle(),
                        message = viewModel.getStatusMessage(),
                        trialDaysRemaining = state.subscriptionInfo.trialDaysRemaining,
                        onOpenWebTool = {
                            val url = when (state.subscriptionInfo.status) {
                                SubscriptionStatus.NONE -> PRICING_URL
                                else -> WEB_TOOL_URL
                            }
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        onRetry = {
                            viewModel.checkSubscription()
                            onRetry()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            color = PrometheusOrange,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Checking subscription...",
            color = TextSecondary,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    GlassCard {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusIcon(
                icon = Icons.Default.Error,
                backgroundColor = ErrorRed.copy(alpha = 0.2f),
                iconColor = ErrorRed
            )

            Text(
                text = "Connection Error",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = error,
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrometheusOrange
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Retry",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SubscriptionContent(
    status: SubscriptionStatus,
    title: String,
    message: String,
    trialDaysRemaining: Int?,
    onOpenWebTool: () -> Unit,
    onRetry: () -> Unit
) {
    val (icon, iconBg, iconColor) = getStatusIconConfig(status)

    GlassCard {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            StatusIcon(
                icon = icon,
                backgroundColor = iconBg,
                iconColor = iconColor
            )

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = message,
                color = TextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            // Trial remaining badge
            if (status == SubscriptionStatus.TRIALING && trialDaysRemaining != null && trialDaysRemaining > 0) {
                TrialBadge(daysRemaining = trialDaysRemaining)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary action button
            Button(
                onClick = onOpenWebTool,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrometheusOrange
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = getButtonText(status),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            // Hint text
            Text(
                text = "Manage your subscription at coach.prometheusapp.io",
                color = TextTertiary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            // Retry button
            TextButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Check again",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun GlassCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GlassBase.copy(alpha = 0.8f),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        )
    ) {
        content()
    }
}

@Composable
private fun StatusIcon(
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun TrialBadge(daysRemaining: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PrometheusOrange.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = PrometheusOrange,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "$daysRemaining days remaining",
                color = PrometheusOrange,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class StatusIconConfig(
    val icon: ImageVector,
    val backgroundColor: Color,
    val iconColor: Color
)

private fun getStatusIconConfig(status: SubscriptionStatus): StatusIconConfig {
    return when (status) {
        SubscriptionStatus.NONE -> StatusIconConfig(
            icon = Icons.Default.CreditCard,
            backgroundColor = PrometheusOrange.copy(alpha = 0.2f),
            iconColor = PrometheusOrange
        )
        SubscriptionStatus.TRIALING -> StatusIconConfig(
            icon = Icons.Default.Timer,
            backgroundColor = InfoBlue.copy(alpha = 0.2f),
            iconColor = InfoBlue
        )
        SubscriptionStatus.PAST_DUE, SubscriptionStatus.UNPAID -> StatusIconConfig(
            icon = Icons.Default.Warning,
            backgroundColor = WarningYellow.copy(alpha = 0.2f),
            iconColor = WarningYellow
        )
        SubscriptionStatus.CANCELED -> StatusIconConfig(
            icon = Icons.Default.Cancel,
            backgroundColor = TextSecondary.copy(alpha = 0.2f),
            iconColor = TextSecondary
        )
        SubscriptionStatus.ACTIVE -> StatusIconConfig(
            icon = Icons.Default.CheckCircle,
            backgroundColor = SuccessGreen.copy(alpha = 0.2f),
            iconColor = SuccessGreen
        )
    }
}

private fun getButtonText(status: SubscriptionStatus): String {
    return when (status) {
        SubscriptionStatus.NONE -> "View Plans"
        SubscriptionStatus.TRIALING -> "Subscribe Now"
        SubscriptionStatus.PAST_DUE, SubscriptionStatus.UNPAID -> "Update Payment"
        SubscriptionStatus.CANCELED -> "Resubscribe"
        SubscriptionStatus.ACTIVE -> "Manage Subscription"
    }
}
