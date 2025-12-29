package com.prometheuscoach.mobile.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.prometheuscoach.mobile.data.model.FeedPost
import com.prometheuscoach.mobile.data.model.getCelebrationTemplate
import com.prometheuscoach.mobile.data.model.getSuggestedMessage
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachDashboardScreen(
    onNavigateToClientDetail: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToAddClient: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    viewModel: CoachDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    // Message dialog state
    var showMessageDialog by remember { mutableStateOf(false) }
    var selectedClientId by remember { mutableStateOf("") }
    var prefillMessage by remember { mutableStateOf("") }

    // Celebrate dialog state
    var showCelebrateDialog by remember { mutableStateOf(false) }
    var celebrateWinId by remember { mutableStateOf("") }
    var celebrateMessage by remember { mutableStateOf("") }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent
        ) { paddingValues ->
            PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && !state.isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrometheusOrange)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Header
                    item {
                        DashboardHeader(
                            coachName = state.coachName.split(" ").firstOrNull() ?: "",
                            activeClientCount = state.activeClientCount,
                            avatarUrl = state.coachAvatar,
                            onSettingsClick = onNavigateToSettings
                        )
                    }

                    // No Clients State
                    if (state.activeClientCount == 0) {
                        item {
                            NoClientsMessage(onAddClientClick = onNavigateToAddClient)
                        }
                    } else {
                        // ═══════════════════════════════════════════════════════════════════
                        // ALERTS SECTION
                        // ═══════════════════════════════════════════════════════════════════
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            AlertSectionHeader(
                                alertCount = state.alerts.size,
                                isExpanded = state.alertsExpanded,
                                onToggle = { viewModel.toggleAlertsExpanded() }
                            )
                        }

                        item {
                            CollapsibleSection(isExpanded = state.alertsExpanded) {
                                if (state.alerts.isEmpty()) {
                                    EmptyAlertsMessage()
                                } else {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        state.alerts.forEach { alert ->
                                            SwipeableAlertCard(
                                                alert = alert,
                                                onDismiss = {
                                                    viewModel.dismissAlert(alert.id)
                                                },
                                                onMessageClick = {
                                                    // Prepare message dialog
                                                    selectedClientId = alert.clientId
                                                    prefillMessage = getSuggestedMessage(alert)
                                                    showMessageDialog = true
                                                },
                                                onViewClick = {
                                                    onNavigateToClientDetail(alert.clientId)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ═══════════════════════════════════════════════════════════════════
                        // WINS SECTION
                        // ═══════════════════════════════════════════════════════════════════
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            WinsSectionHeader(
                                winCount = state.wins.size,
                                isExpanded = state.winsExpanded,
                                onToggle = { viewModel.toggleWinsExpanded() }
                            )
                        }

                        item {
                            CollapsibleSection(isExpanded = state.winsExpanded) {
                                if (state.wins.isEmpty()) {
                                    EmptyWinsMessage()
                                } else {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        state.wins.forEach { win ->
                                            WinCard(
                                                win = win,
                                                onCelebrateClick = {
                                                    celebrateWinId = win.id
                                                    celebrateMessage = getCelebrationTemplate(win)
                                                    selectedClientId = win.clientId
                                                    showCelebrateDialog = true
                                                },
                                                onViewClick = {
                                                    onNavigateToClientDetail(win.clientId)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ═══════════════════════════════════════════════════════════════════
                    // COMMUNITY FEED SECTION
                    // ═══════════════════════════════════════════════════════════════════
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        FeedTabSelector(
                            selectedTab = state.selectedFeedTab,
                            onTabSelected = { viewModel.selectFeedTab(it) }
                        )
                    }

                    // Feed Posts
                    if (state.isLoadingFeed) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = PrometheusOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else if (state.feedPosts.isEmpty()) {
                        item {
                            EmptyFeedMessage(
                                tab = state.selectedFeedTab,
                                onCreatePost = onNavigateToCreatePost
                            )
                        }
                    } else {
                        items(state.feedPosts) { post ->
                            FeedPostCard(
                                post = post,
                                onClick = { onNavigateToPostDetail(post.id) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Bottom spacing for bottom nav
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
        }
    }

    // Message Dialog
    if (showMessageDialog) {
        MessageActionDialog(
            prefillMessage = prefillMessage,
            onDismiss = { showMessageDialog = false },
            onSendMessage = { message ->
                showMessageDialog = false
                // Get or create conversation, send message, then navigate to chat
                scope.launch {
                    val conversationId = viewModel.getOrCreateConversationAndSendMessage(selectedClientId, message)
                    if (conversationId != null) {
                        onNavigateToChat(conversationId)
                    }
                }
            }
        )
    }

    // Celebrate Dialog
    if (showCelebrateDialog) {
        CelebrateActionDialog(
            prefillMessage = celebrateMessage,
            onDismiss = { showCelebrateDialog = false },
            onSendCelebration = { message ->
                showCelebrateDialog = false
                viewModel.celebrateWin(celebrateWinId)
                // Get or create conversation, send celebration message, then navigate to chat
                scope.launch {
                    val conversationId = viewModel.getOrCreateConversationAndSendMessage(selectedClientId, message)
                    if (conversationId != null) {
                        onNavigateToChat(conversationId)
                    }
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FEED POST CARD
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun FeedPostCard(
    post: FeedPost,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // User info row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                if (post.userAvatarUrl != null) {
                    AsyncImage(
                        model = post.userAvatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrometheusOrange.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.userDisplayName?.firstOrNull()?.uppercase() ?: "?",
                            fontWeight = FontWeight.Bold,
                            color = PrometheusOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.userDisplayName ?: "Anonymous",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.isCoach) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PrometheusOrange.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "COACH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrometheusOrange,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (post.workoutName != null) {
                        Text(
                            text = post.workoutName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Post content
            if (post.caption?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Workout stats
            if (post.totalVolumeKg != null || post.prsAchieved != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (post.totalVolumeKg != null && post.totalVolumeKg > 0) {
                        StatChip(icon =Icons.Default.FitnessCenter, text = "${post.totalVolumeKg}kg")
                    }
                    if (post.totalSets != null && post.totalSets > 0) {
                        StatChip(icon = Icons.Default.BarChart, text = "${post.totalSets} sets")
                    }
                    if (post.prsAchieved != null && post.prsAchieved > 0) {
                        StatChip(
                            icon = Icons.Default.EmojiEvents,
                            text = "${post.prsAchieved} PR${if (post.prsAchieved > 1) "s" else ""}",
                            highlight = true
                        )
                    }
                }
            }

            // Engagement row
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Likes",
                    tint = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = post.likesCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Comments",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = post.commentsCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    text: String,
    highlight: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (highlight) PrometheusOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (highlight) PrometheusOrange else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
                color = if (highlight) PrometheusOrange else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// EMPTY FEED MESSAGE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyFeedMessage(
    tab: CoachFeedTab,
    onCreatePost: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (tab == CoachFeedTab.MY_CLIENTS) Icons.Default.Inbox else Icons.Default.Public,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (tab == CoachFeedTab.MY_CLIENTS)
                "No posts from your clients yet"
            else
                "Discover posts from the community",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onCreatePost,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrometheusOrange)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Post")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ACTION DIALOGS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun MessageActionDialog(
    prefillMessage: String,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var message by remember { mutableStateOf(prefillMessage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Send Message")
        },
        text = {
            Column {
                Text(
                    text = "Suggested message:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("Type your message...") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendMessage(message) },
                colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
            ) {
                Text("Open Chat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CelebrateActionDialog(
    prefillMessage: String,
    onDismiss: () -> Unit,
    onSendCelebration: (String) -> Unit
) {
    var message by remember { mutableStateOf(prefillMessage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    tint = PrometheusOrange,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Celebrate!")
            }
        },
        text = {
            Column {
                Text(
                    text = "Send a celebration message:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("Type your celebration message...") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendCelebration(message) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ADE80))
            ) {
                Text("Send Celebration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
