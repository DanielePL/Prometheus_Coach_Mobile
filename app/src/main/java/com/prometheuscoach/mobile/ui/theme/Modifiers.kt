package com.prometheuscoach.mobile.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════════════════
// PROMETHEUS DESIGN MODIFIERS
// Glassmorphism & Glow Effects based on Branding Guide v1.0
// ═══════════════════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════════════════
// SHAPE CONSTANTS
// ═══════════════════════════════════════════════════════════════════════════

val RadiusExtraSmall = 8.dp      // Chips, tags, small elements
val RadiusSmall = 12.dp          // Buttons, small cards
val RadiusMedium = 16.dp         // Standard cards, inputs
val RadiusLarge = 24.dp          // Large cards, modals
val RadiusExtraLarge = 28.dp     // Bottom sheets, full-screen

val RadiusBase = 24.dp           // Coaching Software standard (1.5rem)

// ═══════════════════════════════════════════════════════════════════════════
// GLOW EFFECTS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Subtle orange glow effect
 * Radius: 20dp, Alpha: 0.2
 */
fun Modifier.prometheusGlowSubtle(
    color: Color = PrometheusOrangeGlow,
    radius: Dp = 20.dp,
    alpha: Float = 0.2f
): Modifier = this.shadow(
    elevation = radius,
    shape = RoundedCornerShape(RadiusLarge),
    ambientColor = color.copy(alpha = alpha),
    spotColor = color.copy(alpha = alpha)
)

/**
 * Standard orange glow effect
 * Radius: 30dp, Alpha: 0.3
 */
fun Modifier.prometheusGlow(
    color: Color = PrometheusOrangeGlow,
    radius: Dp = 30.dp,
    alpha: Float = 0.3f
): Modifier = this.shadow(
    elevation = radius,
    shape = RoundedCornerShape(RadiusLarge),
    ambientColor = color.copy(alpha = alpha),
    spotColor = color.copy(alpha = alpha)
)

/**
 * Intense orange glow effect
 * Radius: 40dp, Alpha: 0.4
 */
fun Modifier.prometheusGlowIntense(
    color: Color = PrometheusOrangeGlow,
    radius: Dp = 40.dp,
    alpha: Float = 0.4f
): Modifier = this.shadow(
    elevation = radius,
    shape = RoundedCornerShape(RadiusLarge),
    ambientColor = color.copy(alpha = alpha),
    spotColor = color.copy(alpha = alpha)
)

// ═══════════════════════════════════════════════════════════════════════════
// GLASSMORPHISM EFFECTS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Standard glass background
 * Frosted glass with vertical gradient and white border
 */
fun Modifier.glassBackground(
    cornerRadius: Dp = RadiusMedium
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                GlassBase.copy(alpha = 0.85f),
                GlassBase.copy(alpha = 0.95f)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                GlassBorder.copy(alpha = 0.18f),
                GlassBorder.copy(alpha = 0.10f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Premium glass effect
 * Enhanced glass with inner glow and radial gradient overlay
 * Matches V1 Prometheus styling
 */
fun Modifier.glassPremium(
    cornerRadius: Dp = RadiusMedium
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .drawBehind {
        // Inner shadow/glow for depth
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.03f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.3f, size.height * 0.2f),
                radius = size.maxDimension * 0.8f
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    }
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF252525).copy(alpha = 0.85f),
                Color(0xFF1C1C1C).copy(alpha = 0.75f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.12f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Glass card with orange accent border
 * Used for featured/highlighted cards
 */
fun Modifier.glassCardAccent(
    cornerRadius: Dp = RadiusMedium
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                GlassBase.copy(alpha = 0.85f),
                GlassBase.copy(alpha = 0.95f)
            )
        )
    )
    .border(
        width = 1.dp,
        color = PrometheusOrange.copy(alpha = 0.15f),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Glass with orange glow effect
 * Combines glass + glow for prominent elements
 */
fun Modifier.glassWithOrangeGlow(
    cornerRadius: Dp = RadiusMedium
): Modifier = this
    .prometheusGlowSubtle(radius = 25.dp, alpha = 0.25f)
    .glassBackground(cornerRadius)

/**
 * Elevated glass with shadow
 * Higher elevation appearance for modal-like elements
 */
fun Modifier.glassElevated(
    cornerRadius: Dp = RadiusMedium
): Modifier = this
    .shadow(
        elevation = 8.dp,
        shape = RoundedCornerShape(cornerRadius),
        ambientColor = Color.Black.copy(alpha = 0.3f),
        spotColor = Color.Black.copy(alpha = 0.3f)
    )
    .glassBackground(cornerRadius)

// ═══════════════════════════════════════════════════════════════════════════
// CARD STYLES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Standard card background (solid, not glass)
 * For regular content cards
 */
fun Modifier.cardBackground(
    cornerRadius: Dp = RadiusMedium
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(DarkSurface)
    .border(
        width = 1.dp,
        color = DarkBorder,
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Surface variant card
 * Slightly lighter than standard card
 */
fun Modifier.cardSurfaceVariant(
    cornerRadius: Dp = RadiusMedium
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(DarkSurfaceVariant)
    .border(
        width = 1.dp,
        color = DarkBorder,
        shape = RoundedCornerShape(cornerRadius)
    )

// ═══════════════════════════════════════════════════════════════════════════
// CHAT-SPECIFIC GLASS EFFECTS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * User message glass - Orange tint
 */
fun Modifier.glassUserMessage(
    cornerRadius: Dp = RadiusMedium
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        brush = Brush.horizontalGradient(
            colors = listOf(
                PrometheusOrange.copy(alpha = 0.15f),
                PrometheusOrangeDark.copy(alpha = 0.10f)
            )
        )
    )
    .border(
        width = 1.dp,
        color = PrometheusOrange.copy(alpha = 0.20f),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * AI/Coach message glass - Dark frosted
 */
fun Modifier.glassAiMessage(
    cornerRadius: Dp = RadiusMedium
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(GlassBase.copy(alpha = 0.6f))
    .border(
        width = 1.dp,
        color = GlassBorder.copy(alpha = 0.08f),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Input area glass - Premium panel style
 */
fun Modifier.glassInputArea(
    cornerRadius: Dp = RadiusMedium
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(DarkSurfaceVariant.copy(alpha = 0.95f))
    .border(
        width = 1.dp,
        color = DarkBorder,
        shape = RoundedCornerShape(cornerRadius)
    )

// ═══════════════════════════════════════════════════════════════════════════
// BACKGROUND GRADIENT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * App background gradient (fallback when image not used)
 */
val AppBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        DarkBackgroundSecondary,  // Warm orange-tint (top)
        DarkBackground,           // Main background (middle)
        DarkBackground            // Solid dark (bottom)
    )
)

/**
 * Modifier for background gradient
 */
fun Modifier.appBackgroundGradient(): Modifier = this
    .background(brush = AppBackgroundGradient)