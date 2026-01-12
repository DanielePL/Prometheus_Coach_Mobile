package com.prometheuscoach.mobile.ui.screens.gamification

import androidx.compose.animation.core.animateFloatAsState
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
fun GamificationDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToClientGamification: (clientId: String, clientName: String) -> Unit = { _, _ -> },
    viewModel: GamificationDashboardViewModel = hiltViewModel()
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
                            text = "Gamification",
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
                    GamificationTab.entries.forEach { tab ->
                        Tab(
                            selected = state.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            text = {
                                Text(
                                    text = when (tab) {
                                        GamificationTab.OVERVIEW -> "Overview"
                                        GamificationTab.LEADERBOARD -> "Leaderboard"
                                        GamificationTab.CHALLENGES -> "Challenges"
                                        GamificationTab.BADGES -> "Badges"
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
                                Text(state.error!!)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.refresh() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }

                    else -> {
                        when (state.selectedTab) {
                            GamificationTab.OVERVIEW -> OverviewTab(
                                state = state,
                                onClientClick = { client ->
                                    onNavigateToClientGamification(client.clientId, client.displayName)
                                }
                            )
                            GamificationTab.LEADERBOARD -> LeaderboardTab(
                                state = state,
                                onTypeChange = { viewModel.setLeaderboardType(it) },
                                onPeriodChange = { viewModel.setLeaderboardPeriod(it) }
                            )
                            GamificationTab.CHALLENGES -> ChallengesTab(
                                state = state,
                                onCreateChallenge = { viewModel.showCreateChallengeDialog() }
                            )
                            GamificationTab.BADGES -> BadgesTab(state = state)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(
    state: GamificationDashboardState,
    onClientClick: (ClientGamificationStats) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.People,
                    label = "Clients",
                    value = state.clientsStats.size.toString()
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Active Streaks",
                    value = state.activeStreaks.size.toString()
                )
            }
        }

        // Top clients section
        item {
            Text(
                text = "Top Clients",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (state.topClients.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.EmojiEvents,
                    message = "No client stats yet"
                )
            }
        } else {
            items(state.topClients) { client ->
                ClientStatsCard(
                    client = client,
                    onClick = { onClientClick(client) }
                )
            }
        }

        // Active streaks section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Active Streaks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (state.activeStreaks.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.LocalFireDepartment,
                    message = "No active streaks"
                )
            }
        } else {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.activeStreaks.take(10)) { client ->
                        StreakCard(client = client)
                    }
                }
            }
        }

        // Recent personal challenges
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Personal Challenges",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (state.personalChallenges.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Flag,
                    message = "No personal challenges created"
                )
            }
        } else {
            items(state.personalChallenges.take(5)) { challenge ->
                PersonalChallengeCard(challenge = challenge)
            }
        }
    }
}

@Composable
private fun LeaderboardTab(
    state: GamificationDashboardState,
    onTypeChange: (LeaderboardType) -> Unit,
    onPeriodChange: (LeaderboardPeriod) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Type selector
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LeaderboardType.entries) { type ->
                FilterChip(
                    selected = state.leaderboardType == type,
                    onClick = { onTypeChange(type) },
                    label = {
                        Text(
                            when (type) {
                                LeaderboardType.XP -> "XP"
                                LeaderboardType.STREAK -> "Streak"
                                LeaderboardType.VOLUME -> "Volume"
                                LeaderboardType.WORKOUTS -> "Workouts"
                            }
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrometheusOrange.copy(alpha = 0.2f),
                        selectedLabelColor = PrometheusOrange
                    )
                )
            }
        }

        // Leaderboard list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(state.leaderboard) { index, entry ->
                LeaderboardEntryCard(entry = entry, index = index)
            }

            if (state.leaderboard.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Leaderboard,
                        message = "No leaderboard data"
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengesTab(
    state: GamificationDashboardState,
    onCreateChallenge: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active community challenges
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Community Challenges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (state.activeChallenges.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.EmojiEvents,
                    message = "No active community challenges"
                )
            }
        } else {
            items(state.activeChallenges) { challenge ->
                CommunityChallengeCard(challenge = challenge)
            }
        }

        // Personal challenges
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Personal Challenges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onCreateChallenge) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create")
                }
            }
        }

        val activeChallenges = state.personalChallenges.filter {
            it.status == PersonalChallengeStatus.ACTIVE
        }

        if (activeChallenges.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Flag,
                    message = "No active personal challenges"
                )
            }
        } else {
            items(activeChallenges) { challenge ->
                PersonalChallengeCard(challenge = challenge)
            }
        }
    }
}

@Composable
private fun BadgesTab(state: GamificationDashboardState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Group badges by category
        val badgesByCategory = state.allBadges.groupBy { it.category }

        badgesByCategory.forEach { (category, badges) ->
            item {
                Text(
                    text = when (category) {
                        BadgeCategory.ACHIEVEMENT -> "Achievements"
                        BadgeCategory.MILESTONE -> "Milestones"
                        BadgeCategory.STREAK -> "Streak Badges"
                        BadgeCategory.CHALLENGE -> "Challenge Badges"
                        BadgeCategory.SOCIAL -> "Social Badges"
                        BadgeCategory.SPECIAL -> "Special Badges"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(badges) { badge ->
                        BadgeCard(badge = badge)
                    }
                }
            }
        }

        if (state.allBadges.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.MilitaryTech,
                    message = "No badges available"
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// COMPONENT CARDS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrometheusOrange,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ClientStatsCard(
    client: ClientGamificationStats,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrometheusOrange.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (client.avatarUrl != null) {
                    AsyncImage(
                        model = client.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = client.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrometheusOrange
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Level ${client.level}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrometheusOrange
                    )
                    Text(
                        text = " • ${client.totalXp} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                // XP progress bar
                LinearProgressIndicator(
                    progress = { client.xpProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = PrometheusOrange,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Streak indicator
            if (client.streakDays > 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "${client.streakDays}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakCard(client: ClientGamificationStats) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF6B35).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${client.streakDays} days",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = client.displayName.take(10),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LeaderboardEntryCard(entry: GamificationLeaderboardEntry, index: Int) {
    val medalColor = when (index) {
        0 -> Color(0xFFFFD700) // Gold
        1 -> Color(0xFFC0C0C0) // Silver
        2 -> Color(0xFFCD7F32) // Bronze
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
                .padding(16.dp),
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
                    text = "${entry.rank}",
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
                if (entry.avatarUrl != null) {
                    AsyncImage(
                        model = entry.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = entry.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrometheusOrange
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                entry.level?.let {
                    Text(
                        text = "Level $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Text(
                text = entry.value.toInt().toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PrometheusOrange
            )
        }
    }
}

@Composable
private fun CommunityChallengeCard(challenge: GamificationChallenge) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Chip(
                    colors = ChipDefaults.chipColors(
                        containerColor = PrometheusOrange.copy(alpha = 0.2f)
                    ),
                    label = { Text("+${challenge.xpReward} XP") }
                )
            }

            challenge.description?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Target: ${challenge.targetValue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Ends: ${challenge.endDate.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun PersonalChallengeCard(challenge: PersonalChallenge) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    challenge.clientName?.let {
                        Text(
                            text = "For: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrometheusOrange
                        )
                    }
                }
                StatusChip(status = challenge.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${challenge.currentValue} / ${challenge.targetValue}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${(challenge.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrometheusOrange
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { challenge.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PrometheusOrange,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "+${challenge.rewardXp} XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrometheusOrange
                )
                Text(
                    text = "Ends: ${challenge.endDate.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: PersonalChallengeStatus) {
    val (color, text) = when (status) {
        PersonalChallengeStatus.ACTIVE -> PrometheusOrange to "Active"
        PersonalChallengeStatus.COMPLETED -> Color(0xFF4CAF50) to "Completed"
        PersonalChallengeStatus.FAILED -> Color(0xFFF44336) to "Failed"
        PersonalChallengeStatus.CANCELLED -> Color.Gray to "Cancelled"
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun BadgeCard(badge: Badge) {
    val rarityColor = when (badge.rarity) {
        BadgeRarity.COMMON -> Color.Gray
        BadgeRarity.UNCOMMON -> Color(0xFF4CAF50)
        BadgeRarity.RARE -> Color(0xFF2196F3)
        BadgeRarity.EPIC -> Color(0xFF9C27B0)
        BadgeRarity.LEGENDARY -> Color(0xFFFF9800)
    }

    Card(
        modifier = Modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = rarityColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(rarityColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = null,
                    tint = rarityColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = badge.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = badge.rarity.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = rarityColor
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
private fun Chip(
    colors: ChipColors = ChipDefaults.chipColors(),
    label: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.containerColor
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            label()
        }
    }
}

object ChipDefaults {
    @Composable
    fun chipColors(
        containerColor: Color = Color.White.copy(alpha = 0.1f)
    ) = ChipColors(containerColor)
}

data class ChipColors(val containerColor: Color)

// ═══════════════════════════════════════════════════════════════════════════
// CLIENT GAMIFICATION SCREEN
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientGamificationScreen(
    clientId: String,
    clientName: String,
    onNavigateBack: () -> Unit,
    viewModel: ClientGamificationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(clientId, clientName) {
        viewModel.initClient(clientId, clientName)
    }

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
                        Column {
                            Text(
                                text = clientName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Gamification Stats",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.showCreateChallengeSheet() },
                    containerColor = PrometheusOrange
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Challenge"
                    )
                }
            }
        ) { paddingValues ->
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrometheusOrange)
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
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
                            Text(state.error!!)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadClientData(clientId) }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Level and XP Card
                        item {
                            state.stats?.let { stats ->
                                ClientLevelCard(stats = stats)
                            }
                        }

                        // Stats Row
                        item {
                            state.stats?.let { stats ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StatCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.LocalFireDepartment,
                                        label = "Streak",
                                        value = "${stats.streakDays} days"
                                    )
                                    StatCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.FitnessCenter,
                                        label = "Workouts",
                                        value = "${stats.workoutsCompleted}"
                                    )
                                }
                            }
                        }

                        // Badges Section
                        item {
                            Text(
                                text = "Badges Earned",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (state.badges.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Default.MilitaryTech,
                                    message = "No badges earned yet"
                                )
                            }
                        } else {
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(state.badges) { userBadge ->
                                        userBadge.badge?.let { badge ->
                                            BadgeCard(badge = badge)
                                        }
                                    }
                                }
                            }
                        }

                        // Personal Challenges Section
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Personal Challenges",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { viewModel.showCreateChallengeSheet() }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Create")
                                }
                            }
                        }

                        if (state.personalChallenges.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Default.Flag,
                                    message = "No personal challenges"
                                )
                            }
                        } else {
                            items(state.personalChallenges) { challenge ->
                                PersonalChallengeCard(challenge = challenge)
                            }
                        }

                        // Community Challenge Entries Section
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Challenge Participation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (state.challengeEntries.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Default.EmojiEvents,
                                    message = "Not participating in any challenges"
                                )
                            }
                        } else {
                            items(state.challengeEntries) { entry ->
                                ChallengeEntryCard(entry = entry)
                            }
                        }

                        // Bottom spacing
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientLevelCard(stats: ClientGamificationStats) {
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
            // Level badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PrometheusOrange,
                                PrometheusOrange.copy(alpha = 0.5f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stats.level}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Level ${stats.level}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // XP Progress
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${stats.currentXp} XP",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrometheusOrange
                    )
                    Text(
                        text = "${stats.xpToNextLevel} XP to next level",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { stats.xpProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrometheusOrange,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stats.totalXp}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrometheusOrange
                    )
                    Text(
                        text = "Total XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stats.longestStreak}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )
                    Text(
                        text = "Best Streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.0f", stats.totalVolumeKg / 1000),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "Tons Lifted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeEntryCard(entry: GamificationChallengeEntry) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            entry.rank?.let { rank ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when (rank) {
                                1 -> Color(0xFFFFD700)
                                2 -> Color(0xFFC0C0C0)
                                3 -> Color(0xFFCD7F32)
                                else -> Color.White.copy(alpha = 0.1f)
                            }.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Challenge Entry",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Progress: ${entry.currentValue.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (entry.isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
