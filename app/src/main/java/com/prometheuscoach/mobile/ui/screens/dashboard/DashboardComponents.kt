package com.prometheuscoach.mobile.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import kotlinx.coroutines.launch
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.ui.components.GlowAvatar
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange
import com.prometheuscoach.mobile.ui.theme.RadiusMedium
import com.prometheuscoach.mobile.ui.theme.RadiusSmall
import com.prometheuscoach.mobile.ui.theme.SuccessGreen

// ═══════════════════════════════════════════════════════════════════════════
// COLOR DEFINITIONS
// ═══════════════════════════════════════════════════════════════════════════

// Alert colors
private val CriticalRed = Color(0xFFFF6B6B)
private val CriticalBackground = Color(0xFF3D1515)
private val WarningOrange = Color(0xFFFFB347)
private val WarningBackground = Color(0xFF3D2E15)
private val NoticeYellow = Color(0xFFFFD93D)
private val NoticeBackground = Color(0xFF2D2D15)

// Win colors
private val WinGreen = Color(0xFF4ADE80)
private val WinBackground = Color(0xFF1A2E1A)

// Dismiss colors
private val DismissRed = Color(0xFFEF4444)

// ═══════════════════════════════════════════════════════════════════════════
// SWIPEABLE ALERT CARD
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableAlertCard(
    alert: ClientAlert,
    onDismiss: () -> Unit,
    onMessageClick: () -> Unit,
    onViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                scope.launch { onDismiss() }
                true
            } else {
                false
            }
        },
        positionalThreshold = { it * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // Dismiss background (swipe left to reveal)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(RadiusMedium))
                    .background(DismissRed)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Dismiss",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White
                    )
                }
            }
        },
        content = {
            AlertCard(
                alert = alert,
                onMessageClick = onMessageClick,
                onViewClick = onViewClick,
                modifier = modifier
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// DASHBOARD HEADER
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun DashboardHeader(
    coachName: String,
    activeClientCount: Int,
    avatarUrl: String?,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = getGreeting() + if (coachName.isNotBlank()) ", $coachName" else "",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "$activeClientCount active clients",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        IconButton(onClick = onSettingsClick) {
            GlowAvatar(
                avatarUrl = avatarUrl,
                name = coachName.ifBlank { "Coach" },
                size = 40.dp,
                borderWidth = 1.5.dp
            )
        }
    }
}

private fun getGreeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION HEADER (Collapsible)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun AlertSectionHeader(
    alertCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "NEEDS ATTENTION",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = if (alertCount > 0) CriticalRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (alertCount > 0) {
            Badge(
                containerColor = CriticalRed,
                contentColor = Color.White
            ) {
                Text(
                    text = alertCount.toString(),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun WinsSectionHeader(
    winCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CLIENT WINS",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = if (winCount > 0) WinGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (winCount > 0) {
            Badge(
                containerColor = WinGreen,
                contentColor = Color.Black
            ) {
                Text(
                    text = winCount.toString(),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ALERT CARD
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun AlertCard(
    alert: ClientAlert,
    onMessageClick: () -> Unit,
    onViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (iconColor, backgroundColor) = when (alert.priority) {
        AlertPriority.CRITICAL -> CriticalRed to CriticalBackground
        AlertPriority.WARNING -> WarningOrange to WarningBackground
        AlertPriority.NOTICE -> NoticeYellow to NoticeBackground
    }

    val icon = when (alert.priority) {
        AlertPriority.CRITICAL -> Icons.Default.Error
        AlertPriority.WARNING -> Icons.Default.Warning
        AlertPriority.NOTICE -> Icons.Default.Info
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMedium),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Icon
            Icon(
                imageVector = icon,
                contentDescription = alert.priority.name,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar
            ClientAvatar(
                avatarUrl = alert.clientAvatar,
                name = alert.clientName,
                size = 40
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.clientName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (alert.subtitle.isNotBlank()) {
                    Text(
                        text = alert.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Button
            TextButton(
                onClick = onMessageClick,
                colors = ButtonDefaults.textButtonColors(contentColor = iconColor)
            ) {
                Text(
                    text = alert.actionLabel,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// WIN CARD
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun WinCard(
    win: ClientWin,
    onCelebrateClick: () -> Unit,
    onViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMedium),
        colors = CardDefaults.cardColors(containerColor = WinBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Trophy Icon
            Icon(
                imageVector = when (win.type) {
                    WinType.STREAK_MILESTONE -> Icons.Default.LocalFireDepartment
                    WinType.PERSONAL_RECORD -> Icons.Default.EmojiEvents
                    WinType.VOLUME_RECORD -> Icons.Default.FitnessCenter
                    WinType.NUTRITION_STREAK -> Icons.Default.Restaurant
                    WinType.CONSISTENCY -> Icons.Default.Star
                },
                contentDescription = null,
                tint = PrometheusOrange,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar
            ClientAvatar(
                avatarUrl = win.clientAvatar,
                name = win.clientName,
                size = 40
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = win.clientName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = win.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WinGreen,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (win.subtitle.isNotBlank()) {
                    Text(
                        text = win.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Buttons
            Column(horizontalAlignment = Alignment.End) {
                if (win.celebratable && !win.celebrated) {
                    TextButton(
                        onClick = onCelebrateClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = WinGreen)
                    ) {
                        Text("Celebrate", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CLIENT AVATAR - Using centralized GlowAvatar component
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ClientAvatar(
    avatarUrl: String?,
    name: String,
    size: Int = 48,
    modifier: Modifier = Modifier
) {
    GlowAvatar(
        avatarUrl = avatarUrl,
        name = name,
        size = size.dp,
        modifier = modifier,
        borderWidth = if (size >= 48) 2.dp else 1.5.dp
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// FEED TAB SELECTOR
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun FeedTabSelector(
    selectedTab: CoachFeedTab,
    onTabSelected: (CoachFeedTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "COMMUNITY",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            FeedTabChip(
                text = "Global",
                icon = Icons.Default.Public,
                isSelected = selectedTab == CoachFeedTab.GLOBAL,
                onClick = { onTabSelected(CoachFeedTab.GLOBAL) }
            )
            FeedTabChip(
                text = "Clients",
                icon = Icons.Default.Group,
                isSelected = selectedTab == CoachFeedTab.MY_CLIENTS,
                onClick = { onTabSelected(CoachFeedTab.MY_CLIENTS) }
            )
        }
    }
}

@Composable
private fun FeedTabChip(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) PrometheusOrange else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// EMPTY STATES
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun EmptyAlertsMessage(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(RadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = SuccessGreen.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "All clients on track!",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = SuccessGreen
            )
        }
    }
}

@Composable
fun EmptyWinsMessage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No recent wins to celebrate",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun NoClientsMessage(
    onAddClientClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = PrometheusOrange
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Add your first client",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Invite clients to start tracking their progress",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddClientClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Client")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// COLLAPSIBLE SECTION
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun CollapsibleSection(
    isExpanded: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// UNREAD MESSAGES SECTION
// ═══════════════════════════════════════════════════════════════════════════

private val MessageBlue = Color(0xFF60A5FA)
private val MessageBackground = Color(0xFF1A2533)

@Composable
fun MessagesSectionHeader(
    unreadCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "NEW MESSAGES",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = if (unreadCount > 0) MessageBlue else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (unreadCount > 0) {
            Badge(
                containerColor = MessageBlue,
                contentColor = Color.White
            ) {
                Text(
                    text = unreadCount.toString(),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun UnreadMessageCard(
    participantName: String,
    participantAvatar: String?,
    lastMessage: String?,
    unreadCount: Int,
    onClick: () -> Unit,
    onQuickReply: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isReplying by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = MessageBackground
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                ClientAvatar(
                    avatarUrl = participantAvatar,
                    name = participantName,
                    size = 44
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = participantName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = lastMessage ?: "New message",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Unread Badge
                if (unreadCount > 0) {
                    Badge(
                        containerColor = MessageBlue,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Quick Reply Button
                if (onQuickReply != null && !isReplying) {
                    IconButton(
                        onClick = { isReplying = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Quick reply",
                            tint = MessageBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open chat",
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }

            // Quick Reply Input
            if (isReplying && onQuickReply != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Quick reply...", color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MessageBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = MessageBlue
                        ),
                        shape = RoundedCornerShape(RadiusSmall),
                        singleLine = true,
                        enabled = !isSending
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                isSending = true
                                onQuickReply(replyText)
                                replyText = ""
                                isReplying = false
                                isSending = false
                            }
                        },
                        enabled = replyText.isNotBlank() && !isSending
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (replyText.isNotBlank()) MessageBlue else Color.White.copy(alpha = 0.3f)
                        )
                    }

                    // Cancel Button
                    IconButton(
                        onClick = {
                            replyText = ""
                            isReplying = false
                        },
                        enabled = !isSending
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
