package com.prometheuscoach.mobile.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prometheuscoach.mobile.data.billing.Feature
import com.prometheuscoach.mobile.data.billing.FeatureGate
import com.prometheuscoach.mobile.ui.theme.*

private const val PRICING_URL = "https://coach.prometheusapp.io/pricing"

/**
 * Wraps content that requires Pro subscription.
 * Shows an upgrade prompt if user doesn't have Pro access.
 *
 * @param featureGate The FeatureGate instance
 * @param feature The specific feature being gated
 * @param showOverlay If true, shows blurred content with overlay. If false, replaces content completely.
 * @param content The content to show when user has access
 */
@Composable
fun ProFeatureGate(
    featureGate: FeatureGate,
    feature: Feature,
    showOverlay: Boolean = true,
    content: @Composable () -> Unit
) {
    val hasAccess by featureGate.observeFeatureAvailability(feature).collectAsState(initial = false)

    if (hasAccess) {
        content()
    } else {
        if (showOverlay) {
            // Show blurred content with upgrade overlay
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .blur(8.dp)
                        .alpha(0.3f)
                ) {
                    content()
                }
                UpgradeOverlay(feature = feature)
            }
        } else {
            // Replace content with upgrade prompt
            UpgradePromptCard(feature = feature)
        }
    }
}

/**
 * Overlay shown on top of blurred Pro content
 */
@Composable
private fun UpgradeOverlay(feature: Feature) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground.copy(alpha = 0.8f),
                        DarkBackground.copy(alpha = 0.95f)
                    )
                )
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lock icon with gradient
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrometheusOrange, PrometheusOrangeDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "Pro Feature",
                color = PrometheusOrange,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = feature.displayName,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = feature.description,
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRICING_URL))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Upgrade,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upgrade to Pro",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Full card component for upgrade prompt (used when replacing content entirely)
 */
@Composable
fun UpgradePromptCard(
    feature: Feature,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = GlassBase.copy(alpha = 0.6f),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pro badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PrometheusOrange.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PrometheusOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "PRO",
                        color = PrometheusOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = feature.displayName,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = feature.description,
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRICING_URL))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Upgrade,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upgrade to Pro",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Small inline badge to indicate Pro feature
 */
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = PrometheusOrange.copy(alpha = 0.2f)
    ) {
        Text(
            text = "PRO",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = PrometheusOrange,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Button that shows a Pro badge and prompts upgrade when clicked (for Basic users)
 * or executes the action when user has Pro access
 */
@Composable
fun ProFeatureButton(
    featureGate: FeatureGate,
    feature: Feature,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val hasAccess by featureGate.observeFeatureAvailability(feature).collectAsState(initial = false)
    val context = LocalContext.current
    var showUpgradeDialog by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (hasAccess) {
                onClick()
            } else {
                showUpgradeDialog = true
            }
        },
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (hasAccess) PrometheusOrange else GlassBase
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontWeight = FontWeight.SemiBold)
        if (!hasAccess) {
            Spacer(modifier = Modifier.width(8.dp))
            ProBadge()
        }
    }

    // Upgrade dialog
    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            containerColor = DarkSurface,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrometheusOrange.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PrometheusOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = feature.displayName,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "${feature.description}\n\nUpgrade to Pro to unlock this feature and more.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpgradeDialog = false
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRICING_URL))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
                ) {
                    Text("Upgrade to Pro")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeDialog = false }) {
                    Text("Maybe Later", color = TextSecondary)
                }
            }
        )
    }
}