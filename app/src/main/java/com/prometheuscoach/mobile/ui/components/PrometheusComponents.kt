package com.prometheuscoach.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prometheuscoach.mobile.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════
// PROMETHEUS BRANDED COMPONENTS
// Centralized UI components following Branding Guide v1.0
// ═══════════════════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════════════════
// CARDS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Standard Prometheus Card with glass background
 * Uses glassBackground() modifier with 16dp radius
 *
 * @param modifier Optional modifier
 * @param onClick Optional click handler (null = non-clickable)
 * @param content Card content
 */
@Composable
fun PrometheusCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = modifier
        .fillMaxWidth()
        .glassBackground(RadiusMedium)
        .then(
            if (onClick != null) {
                Modifier
                    .clip(RoundedCornerShape(RadiusMedium))
                    .clickable(onClick = onClick)
            } else {
                Modifier
            }
        )

    Column(
        modifier = baseModifier.padding(16.dp),
        content = content
    )
}

/**
 * Featured Prometheus Card with orange accent border
 * Uses glassCardAccent() modifier with 16dp radius
 */
@Composable
fun PrometheusCardAccent(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = modifier
        .fillMaxWidth()
        .glassCardAccent(RadiusMedium)
        .then(
            if (onClick != null) {
                Modifier
                    .clip(RoundedCornerShape(RadiusMedium))
                    .clickable(onClick = onClick)
            } else {
                Modifier
            }
        )

    Column(
        modifier = baseModifier.padding(16.dp),
        content = content
    )
}

/**
 * Elevated Prometheus Card with shadow
 * Uses glassElevated() modifier with 16dp radius
 */
@Composable
fun PrometheusCardElevated(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = modifier
        .fillMaxWidth()
        .glassElevated(RadiusMedium)
        .then(
            if (onClick != null) {
                Modifier
                    .clip(RoundedCornerShape(RadiusMedium))
                    .clickable(onClick = onClick)
            } else {
                Modifier
            }
        )

    Column(
        modifier = baseModifier.padding(16.dp),
        content = content
    )
}

/**
 * Card with glow effect
 * Uses glassWithOrangeGlow() modifier with 16dp radius
 */
@Composable
fun PrometheusCardGlow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = modifier
        .fillMaxWidth()
        .glassWithOrangeGlow(RadiusMedium)
        .then(
            if (onClick != null) {
                Modifier
                    .clip(RoundedCornerShape(RadiusMedium))
                    .clickable(onClick = onClick)
            } else {
                Modifier
            }
        )

    Column(
        modifier = baseModifier.padding(16.dp),
        content = content
    )
}

/**
 * Row-based card variant for horizontal layouts
 */
@Composable
fun PrometheusCardRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val baseModifier = modifier
        .fillMaxWidth()
        .glassBackground(RadiusMedium)
        .then(
            if (onClick != null) {
                Modifier
                    .clip(RoundedCornerShape(RadiusMedium))
                    .clickable(onClick = onClick)
            } else {
                Modifier
            }
        )

    Row(
        modifier = baseModifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// BUTTONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Primary Prometheus Button
 * Orange background, white text, 12dp radius
 */
@Composable
fun PrometheusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = if (fullWidth) modifier.fillMaxWidth().height(52.dp) else modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(RadiusSmall),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrometheusOrange,
            contentColor = Color.White,
            disabledContainerColor = PrometheusOrange.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Secondary Prometheus Button
 * Glass background, orange text, 12dp radius
 */
@Composable
fun PrometheusButtonSecondary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = if (fullWidth) modifier.fillMaxWidth().height(52.dp) else modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(RadiusSmall),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) PrometheusOrange.copy(alpha = 0.5f) else PrometheusOrange.copy(alpha = 0.2f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = GlassBase.copy(alpha = 0.5f),
            contentColor = PrometheusOrange,
            disabledContentColor = PrometheusOrange.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Outlined Prometheus Button
 * Transparent background, orange border and text, 12dp radius
 */
@Composable
fun PrometheusButtonOutlined(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = if (fullWidth) modifier.fillMaxWidth().height(52.dp) else modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(RadiusSmall),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) PrometheusOrange else PrometheusOrange.copy(alpha = 0.3f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = PrometheusOrange,
            disabledContentColor = PrometheusOrange.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Ghost/Text Button
 * No background, orange text
 */
@Composable
fun PrometheusButtonGhost(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = PrometheusOrange,
            disabledContentColor = PrometheusOrange.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Small Prometheus Button
 * Compact version for inline actions
 */
@Composable
fun PrometheusButtonSmall(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        enabled = enabled,
        shape = RoundedCornerShape(RadiusExtraSmall),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrometheusOrange,
            contentColor = Color.White,
            disabledContainerColor = PrometheusOrange.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION CONTAINERS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Section with title and content
 * Common pattern for dashboard sections
 */
@Composable
fun PrometheusSection(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            action?.invoke()
        }
        content()
    }
}
