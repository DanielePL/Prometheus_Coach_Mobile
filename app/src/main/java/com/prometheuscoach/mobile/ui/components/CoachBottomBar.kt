package com.prometheuscoach.mobile.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange

// Navigation destinations for the bottom bar
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : BottomNavItem(
        route = "dashboard",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    data object Clients : BottomNavItem(
        route = "clients",
        title = "Clients",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People
    )
    // FAB placeholder - not a real destination
    data object Calendar : BottomNavItem(
        route = "calendar",
        title = "Calendar",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )
    data object Library : BottomNavItem(
        route = "library",
        title = "Library",
        selectedIcon = Icons.Filled.LocalLibrary,
        unselectedIcon = Icons.Outlined.LocalLibrary
    )
}

@Composable
fun CoachBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onQuickAction: (QuickAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showQuickActions by remember { mutableStateOf(false) }

    // Glassmorphism colors
    val glassBackground = Color(0xFF1A1A1A).copy(alpha = 0.95f)
    val glassBorderTop = Brush.horizontalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.05f),
            PrometheusOrange.copy(alpha = 0.4f),
            Color.White.copy(alpha = 0.05f)
        )
    )

    val navItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Clients,
        // FAB in the middle
        BottomNavItem.Calendar,
        BottomNavItem.Library
    )

    Box(modifier = modifier) {
        // Scrim when Quick Actions is open
        if (showQuickActions) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showQuickActions = false }
            )
        }

        // Quick Actions Modal
        AnimatedVisibility(
            visible = showQuickActions,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        ) {
            QuickActionsMenu(
                onDismiss = { showQuickActions = false },
                onAction = { action ->
                    showQuickActions = false
                    onQuickAction(action)
                }
            )
        }

        // Bottom Navigation Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(glassBackground)
                .border(
                    width = 1.dp,
                    brush = glassBorderTop,
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavBarItem(
                        icon = if (currentRoute == BottomNavItem.Home.route)
                            BottomNavItem.Home.selectedIcon else BottomNavItem.Home.unselectedIcon,
                        label = BottomNavItem.Home.title,
                        selected = currentRoute == BottomNavItem.Home.route,
                        onClick = {
                            showQuickActions = false
                            onNavigate(BottomNavItem.Home.route)
                        }
                    )
                }

                // Clients
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavBarItem(
                        icon = if (currentRoute == BottomNavItem.Clients.route)
                            BottomNavItem.Clients.selectedIcon else BottomNavItem.Clients.unselectedIcon,
                        label = BottomNavItem.Clients.title,
                        selected = currentRoute == BottomNavItem.Clients.route,
                        onClick = {
                            showQuickActions = false
                            onNavigate(BottomNavItem.Clients.route)
                        }
                    )
                }

                // Central FAB
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .offset(y = (-6).dp),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingActionButton(
                        onClick = { showQuickActions = !showQuickActions },
                        containerColor = if (showQuickActions) Color.Gray else PrometheusOrange,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (showQuickActions) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Quick Actions",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Calendar
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavBarItem(
                        icon = if (currentRoute == BottomNavItem.Calendar.route)
                            BottomNavItem.Calendar.selectedIcon else BottomNavItem.Calendar.unselectedIcon,
                        label = BottomNavItem.Calendar.title,
                        selected = currentRoute == BottomNavItem.Calendar.route,
                        onClick = {
                            showQuickActions = false
                            onNavigate(BottomNavItem.Calendar.route)
                        }
                    )
                }

                // Library
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavBarItem(
                        icon = if (currentRoute == BottomNavItem.Library.route)
                            BottomNavItem.Library.selectedIcon else BottomNavItem.Library.unselectedIcon,
                        label = BottomNavItem.Library.title,
                        selected = currentRoute == BottomNavItem.Library.route,
                        onClick = {
                            showQuickActions = false
                            onNavigate(BottomNavItem.Library.route)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) PrometheusOrange else Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = color
        )
    }
}

// Quick Action types for Coach app
enum class QuickAction {
    NEW_POST,
    ADD_CLIENT,
    NEW_WORKOUT,
    NEW_PROGRAM,
    SCHEDULE_EVENT,
    SEND_MESSAGE
}

@Composable
private fun QuickActionsMenu(
    onDismiss: () -> Unit,
    onAction: (QuickAction) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF2A2A2A),
        shadowElevation = 16.dp,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "QUICK ACTIONS",
                color = Color(0xFFAAAAAA),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            QuickActionItem(
                icon = Icons.Default.Edit,
                title = "New Post",
                subtitle = "Share with the community",
                onClick = { onAction(QuickAction.NEW_POST) }
            )

            QuickActionItem(
                icon = Icons.Default.PersonAdd,
                title = "Add Client",
                subtitle = "Invite a new client",
                onClick = { onAction(QuickAction.ADD_CLIENT) }
            )

            QuickActionItem(
                icon = Icons.Default.FitnessCenter,
                title = "New Workout",
                subtitle = "Create a workout routine",
                onClick = { onAction(QuickAction.NEW_WORKOUT) }
            )

            QuickActionItem(
                icon = Icons.Default.CalendarViewWeek,
                title = "New Program",
                subtitle = "Build a training program",
                onClick = { onAction(QuickAction.NEW_PROGRAM) }
            )

            QuickActionItem(
                icon = Icons.Default.Event,
                title = "Schedule Event",
                subtitle = "Add to calendar",
                onClick = { onAction(QuickAction.SCHEDULE_EVENT) }
            )

            QuickActionItem(
                icon = Icons.AutoMirrored.Filled.Message,
                title = "Send Message",
                subtitle = "Message a client",
                onClick = { onAction(QuickAction.SEND_MESSAGE) }
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF3A3A3A),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = PrometheusOrange.copy(alpha = 0.25f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = PrometheusOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = Color(0xFFB0B0B0),
                    fontSize = 12.sp
                )
            }
        }
    }
}
