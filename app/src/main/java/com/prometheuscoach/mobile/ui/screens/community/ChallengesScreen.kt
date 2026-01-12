package com.prometheuscoach.mobile.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChallengeDetail: (challengeId: String) -> Unit = {},
    viewModel: ChallengesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F0F1A)
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Challenges",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tabs
                TabRow(
                    selectedTabIndex = state.selectedTab.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = PrometheusOrange
                ) {
                    ChallengesDashboardTab.entries.forEach { tab ->
                        Tab(
                            selected = state.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            text = {
                                Text(
                                    text = when (tab) {
                                        ChallengesDashboardTab.ACTIVE -> "Active"
                                        ChallengesDashboardTab.MAX_OUT_FRIDAY -> "Max Out"
                                        ChallengesDashboardTab.HISTORY -> "History"
                                    }
                                )
                            }
                        )
                    }
                }

                // Content
                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrometheusOrange)
                        }
                    }

                    state.error != null -> {
                        ErrorContent(
                            error = state.error!!,
                            onRetry = { viewModel.refresh() }
                        )
                    }

                    state.selectedChallenge != null -> {
                        ChallengeDetailContent(
                            challenge = state.selectedChallenge!!,
                            entries = if (state.showOnlyClients) state.clientsInChallenge else state.selectedChallengeEntries,
                            clientsCount = state.clientsInChallenge.size,
                            isLoading = state.isLoadingDetail,
                            showOnlyClients = state.showOnlyClients,
                            onToggleFilter = { viewModel.toggleShowOnlyClients() },
                            onBack = { viewModel.clearSelectedChallenge() }
                        )
                    }

                    else -> {
                        when (state.selectedTab) {
                            ChallengesDashboardTab.ACTIVE -> ActiveChallengesTab(
                                activeChallenges = state.activeChallenges,
                                upcomingChallenges = state.upcomingChallenges,
                                onChallengeClick = { viewModel.selectChallenge(it) }
                            )
                            ChallengesDashboardTab.MAX_OUT_FRIDAY -> MaxOutFridayTab(
                                maxOutFriday = state.maxOutFriday,
                                leaderboard = state.maxOutLeaderboard,
                                previousWinners = state.previousWinners,
                                showOnlyClients = state.showOnlyClients,
                                onToggleFilter = { viewModel.toggleShowOnlyClients() }
                            )
                            ChallengesDashboardTab.HISTORY -> HistoryTab(
                                completedChallenges = state.completedChallenges,
                                onChallengeClick = { viewModel.selectChallenge(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveChallengesTab(
    activeChallenges: List<Challenge>,
    upcomingChallenges: List<Challenge>,
    onChallengeClick: (Challenge) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active challenges section
        item {
            Text(
                text = "Active Challenges",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (activeChallenges.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.EmojiEvents,
                    message = "No active challenges"
                )
            }
        } else {
            items(activeChallenges) { challenge ->
                ChallengeCard(
                    challenge = challenge,
                    onClick = { onChallengeClick(challenge) }
                )
            }
        }

        // Upcoming challenges section
        if (upcomingChallenges.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Upcoming Challenges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(upcomingChallenges) { challenge ->
                ChallengeCard(
                    challenge = challenge,
                    onClick = { onChallengeClick(challenge) },
                    isUpcoming = true
                )
            }
        }
    }
}

@Composable
private fun MaxOutFridayTab(
    maxOutFriday: MaxOutFridayInfo?,
    leaderboard: List<ChallengeEntry>,
    previousWinners: List<PreviousWinner>,
    showOnlyClients: Boolean,
    onToggleFilter: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Max Out Friday
        item {
            if (maxOutFriday != null) {
                MaxOutFridayCard(maxOut = maxOutFriday)
            } else {
                EmptyStateCard(
                    icon = Icons.Default.FitnessCenter,
                    message = "No Max Out Friday this week"
                )
            }
        }

        // Filter toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Leaderboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FilterChip(
                    selected = showOnlyClients,
                    onClick = onToggleFilter,
                    label = { Text("My Clients") },
                    leadingIcon = if (showOnlyClients) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrometheusOrange.copy(alpha = 0.2f),
                        selectedLabelColor = PrometheusOrange
                    )
                )
            }
        }

        // Leaderboard entries
        if (leaderboard.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Leaderboard,
                    message = "No entries yet"
                )
            }
        } else {
            itemsIndexed(leaderboard.take(20)) { index, entry ->
                LeaderboardEntryCard(entry = entry, rank = index + 1)
            }
        }

        // Previous winners section
        if (previousWinners.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Previous Winners",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(previousWinners) { winner ->
                        PreviousWinnerCard(winner = winner)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(
    completedChallenges: List<Challenge>,
    onChallengeClick: (Challenge) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (completedChallenges.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.History,
                    message = "No completed challenges yet"
                )
            }
        } else {
            items(completedChallenges) { challenge ->
                ChallengeCard(
                    challenge = challenge,
                    onClick = { onChallengeClick(challenge) },
                    isCompleted = true
                )
            }
        }
    }
}

@Composable
private fun ChallengeDetailContent(
    challenge: Challenge,
    entries: List<ChallengeEntry>,
    clientsCount: Int,
    isLoading: Boolean,
    showOnlyClients: Boolean,
    onToggleFilter: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${challenge.participantsCount} participants",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Leaderboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (clientsCount > 0) {
                    Text(
                        text = "$clientsCount clients",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrometheusOrange,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                FilterChip(
                    selected = showOnlyClients,
                    onClick = onToggleFilter,
                    label = { Text("My Clients") },
                    leadingIcon = if (showOnlyClients) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrometheusOrange.copy(alpha = 0.2f),
                        selectedLabelColor = PrometheusOrange
                    )
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrometheusOrange)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (entries.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Leaderboard,
                            message = if (showOnlyClients) "None of your clients have entered" else "No entries yet"
                        )
                    }
                } else {
                    itemsIndexed(entries) { index, entry ->
                        LeaderboardEntryCard(
                            entry = entry,
                            rank = entry.rank ?: (index + 1)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// COMPONENT CARDS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ChallengeCard(
    challenge: Challenge,
    onClick: () -> Unit,
    isUpcoming: Boolean = false,
    isCompleted: Boolean = false
) {
    val (icon, iconColor) = when (challenge.challengeType) {
        ChallengeType.MAX_OUT_FRIDAY -> Icons.Default.FitnessCenter to PrometheusOrange
        ChallengeType.VOLUME -> Icons.Default.BarChart to Color(0xFF4CAF50)
        ChallengeType.STREAK -> Icons.Default.LocalFireDepartment to Color(0xFFFF6B35)
        ChallengeType.CUSTOM -> Icons.Default.Flag to Color(0xFF2196F3)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                Color.White.copy(alpha = 0.03f)
            else
                Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isUpcoming) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF2196F3).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Upcoming",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2196F3)
                            )
                        }
                    }
                    if (isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Gray.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Completed",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                challenge.description?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.People,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${challenge.participantsCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "Ends: ${challenge.endDate.take(10)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun MaxOutFridayCard(maxOut: MaxOutFridayInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = PrometheusOrange.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = PrometheusOrange,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "MAX OUT FRIDAY",
                style = MaterialTheme.typography.labelLarge,
                color = PrometheusOrange,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = maxOut.exerciseName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${maxOut.participantsCount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrometheusOrange
                    )
                    Text(
                        text = "Participants",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = maxOut.endDate.take(10),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ends",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardEntryCard(entry: ChallengeEntry, rank: Int) {
    val medalColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> null
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (medalColor != null)
                medalColor.copy(alpha = 0.1f)
            else
                Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(medalColor?.copy(alpha = 0.3f) ?: Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = medalColor ?: MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrometheusOrange.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (entry.userAvatar != null) {
                    AsyncImage(
                        model = entry.userAvatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = (entry.userName ?: "?").take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrometheusOrange
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.userName ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (entry.isPr) {
                    Text(
                        text = "PR!",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrometheusOrange
                    )
                }
            }

            // Value
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.valueKg?.toInt() ?: 0} kg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (rank <= 3) medalColor ?: PrometheusOrange else PrometheusOrange
                )
                entry.valueReps?.let {
                    Text(
                        text = "$it reps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviousWinnerCard(winner: PreviousWinner) {
    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFD700).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Crown icon
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrometheusOrange.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (winner.winnerAvatar != null) {
                    AsyncImage(
                        model = winner.winnerAvatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = winner.winnerName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrometheusOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = winner.winnerName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = winner.exerciseName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Text(
                text = "${winner.winningWeightKg.toInt()} kg",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
