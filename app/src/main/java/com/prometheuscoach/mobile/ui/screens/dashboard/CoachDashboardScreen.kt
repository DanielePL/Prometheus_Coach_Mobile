package com.prometheuscoach.mobile.ui.screens.dashboard

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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
import androidx.compose.foundation.BorderStroke
import com.prometheuscoach.mobile.data.model.CommunityComment
import com.prometheuscoach.mobile.data.model.FeedPost
import com.prometheuscoach.mobile.data.model.getCelebrationTemplate
import com.prometheuscoach.mobile.data.model.getSuggestedMessage
import com.prometheuscoach.mobile.data.repository.Connection
import com.prometheuscoach.mobile.ui.components.GlowAvatar
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange
import com.prometheuscoach.mobile.ui.theme.glassPremium
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
    onNavigateToChallenges: () -> Unit = {},
    onNavigateToGamification: () -> Unit = {},
    viewModel: CoachDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-refresh messages when returning from chat (screen resumes)
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Refresh messages every time the screen becomes visible
            viewModel.refreshMessages()
        }
    }

    // Message dialog state
    var showMessageDialog by remember { mutableStateOf(false) }
    var selectedClientId by remember { mutableStateOf("") }
    var prefillMessage by remember { mutableStateOf("") }

    // Celebrate dialog state
    var showCelebrateDialog by remember { mutableStateOf(false) }
    var celebrateWinId by remember { mutableStateOf("") }
    var celebrateMessage by remember { mutableStateOf("") }

    // Show error in snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
        }
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
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

                    // ═══════════════════════════════════════════════════════════════════
                    // PENDING CONNECTION REQUESTS - Most important business feature!
                    // Shows immediately after header when there are pending requests
                    // ═══════════════════════════════════════════════════════════════════
                    if (state.pendingRequests.isNotEmpty()) {
                        item {
                            PendingRequestsSection(
                                pendingRequests = state.pendingRequests,
                                isResponding = state.isRespondingToRequest,
                                onAccept = { connectionId ->
                                    viewModel.acceptConnectionRequest(connectionId)
                                },
                                onDecline = { connectionId ->
                                    viewModel.declineConnectionRequest(connectionId)
                                }
                            )
                        }
                    }

                    // ═══════════════════════════════════════════════════════════════════
                    // UNREAD MESSAGES SECTION - Show new client messages
                    // ═══════════════════════════════════════════════════════════════════
                    if (state.totalUnreadCount > 0) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            MessagesSectionHeader(
                                unreadCount = state.totalUnreadCount,
                                isExpanded = state.messagesExpanded,
                                onToggle = { viewModel.toggleMessagesExpanded() }
                            )
                        }

                        item {
                            CollapsibleSection(isExpanded = state.messagesExpanded) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    state.unreadConversations.forEach { conversation ->
                                        UnreadMessageCard(
                                            participantName = conversation.participantName,
                                            participantAvatar = conversation.participantAvatar,
                                            lastMessage = conversation.lastMessage,
                                            unreadCount = conversation.unreadCount,
                                            onClick = { onNavigateToChat(conversation.conversationId) },
                                            onQuickReply = { message ->
                                                viewModel.sendQuickReply(conversation.conversationId, message)
                                            }
                                        )
                                    }
                                }
                            }
                        }
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
                    // QUICK ACCESS SECTION (Challenges & Gamification)
                    // ═══════════════════════════════════════════════════════════════════
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        QuickAccessSection(
                            onChallengesClick = onNavigateToChallenges,
                            onGamificationClick = onNavigateToGamification
                        )
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
                                onLikeClick = { viewModel.toggleLike(post.id) },
                                onAddComment = { comment -> viewModel.addComment(post.id, comment) },
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
    onLikeClick: () -> Unit,
    onAddComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var commentText by remember { mutableStateOf("") }
    var isAddingComment by remember { mutableStateOf(false) }
    var showCommentInput by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus comment input when shown
    LaunchedEffect(showCommentInput) {
        if (showCommentInput) {
            kotlinx.coroutines.delay(100)
            focusRequester.requestFocus()
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .glassPremium(cornerRadius = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // User info row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with glow ring
                GlowAvatar(
                    avatarUrl = post.userAvatarUrl,
                    name = post.userDisplayName ?: "?",
                    size = 40.dp,
                    showGlow = post.isCoach, // Glow for coaches
                    borderWidth = 1.5.dp
                )

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
                // Like button - using IconButton to ensure it captures clicks properly
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current,
                            onClick = { onLikeClick() }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (post.isLiked) "Unlike" else "Like",
                        tint = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = post.likesCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (post.isLiked) Color.Red.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Comment button - toggles inline input
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showCommentInput = !showCommentInput }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = if (showCommentInput) PrometheusOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = post.commentsCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (showCommentInput) PrometheusOrange.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Comments preview
            if (post.previewComments.isNotEmpty() || post.commentsCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))

                // "View all X comments" link
                if (post.commentsCount > post.previewComments.size) {
                    Text(
                        text = "View all ${post.commentsCount} comments",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Show preview comments
                post.previewComments.forEach { comment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = comment.userDisplayName ?: "User",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = comment.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Inline comment input (Instagram-style - only shows when comment button clicked)
            if (showCommentInput) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = {
                            Text(
                                "Add a comment...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            cursorColor = PrometheusOrange,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(22.dp),
                        singleLine = true,
                        enabled = !isAddingComment
                    )

                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank() && !isAddingComment) {
                                isAddingComment = true
                                onAddComment(commentText)
                                commentText = ""
                                showCommentInput = false
                                isAddingComment = false
                            }
                        },
                        enabled = commentText.isNotBlank() && !isAddingComment,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send comment",
                            tint = if (commentText.isNotBlank()) PrometheusOrange else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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

// ═══════════════════════════════════════════════════════════════════════════
// QUICK ACCESS SECTION
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun QuickAccessSection(
    onChallengesClick: () -> Unit,
    onGamificationClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Quick Access",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Challenges Card
            QuickAccessCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEvents,
                title = "Challenges",
                subtitle = "Max Out Friday & more",
                accentColor = Color(0xFFFFD700),
                onClick = onChallengesClick
            )

            // Gamification Card
            QuickAccessCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Leaderboard,
                title = "Gamification",
                subtitle = "XP & Leaderboards",
                accentColor = PrometheusOrange,
                onClick = onGamificationClick
            )
        }
    }
}

@Composable
private fun QuickAccessCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PENDING CONNECTION REQUESTS SECTION
// Most important business feature - displayed prominently on dashboard
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PendingRequestsSection(
    pendingRequests: List<Connection>,
    isResponding: Boolean,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrometheusOrange.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with notification badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrometheusOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "New Connection Requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${pendingRequests.size} athlete${if (pendingRequests.size != 1) "s" else ""} waiting",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                // Notification badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PrometheusOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${pendingRequests.size}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Pending request cards
            pendingRequests.forEach { request ->
                PendingRequestCard(
                    request = request,
                    isResponding = isResponding,
                    onAccept = { onAccept(request.connectionId) },
                    onDecline = { onDecline(request.connectionId) }
                )
                if (request != pendingRequests.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PendingRequestCard(
    request: Connection,
    isResponding: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                GlowAvatar(
                    avatarUrl = request.userAvatar,
                    name = request.userName,
                    size = 48.dp,
                    showGlow = false,
                    borderWidth = 2.dp
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.userName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Wants to train with you",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    enabled = !isResponding,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Decline")
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    enabled = !isResponding,
                    colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isResponding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Accept")
                    }
                }
            }
        }
    }
}
