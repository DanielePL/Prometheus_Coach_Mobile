package com.prometheuscoach.mobile.ui.screens.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prometheuscoach.mobile.data.model.FeedTab
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onNavigateToCreatePost: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val feedState by viewModel.feedState.collectAsState()
    val listState = rememberLazyListState()

    // Load more when reaching end
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= feedState.posts.size - 3 && feedState.hasMore && !feedState.isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Community",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
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
            // Feed type tabs
            FeedTypeSelector(
                currentTab = feedState.currentTab,
                onTabSelected = { viewModel.switchTab(it) }
            )

            PullToRefreshBox(
                isRefreshing = feedState.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    feedState.error != null && feedState.posts.isEmpty() -> {
                        ErrorState(
                            message = feedState.error ?: "Unknown error",
                            onRetry = { viewModel.refresh() }
                        )
                    }
                    feedState.posts.isEmpty() && !feedState.isLoading -> {
                        EmptyFeedState()
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = feedState.posts,
                                key = { it.id }
                            ) { post ->
                                FeedPostCard(
                                    post = post,
                                    onUserClick = onNavigateToUserProfile,
                                    onLikeClick = { viewModel.toggleLike(post.id) },
                                    onCommentClick = { onNavigateToPostDetail(post.id) },
                                    onPostClick = { onNavigateToPostDetail(post.id) },
                                    onAddComment = { content -> viewModel.addComment(post.id, content) },
                                    onMenuClick = { action ->
                                        when (action) {
                                            FeedPostMenuAction.DELETE -> viewModel.deletePost(post.id)
                                            FeedPostMenuAction.REPORT -> { /* TODO: Report */ }
                                        }
                                    },
                                    isOwnPost = viewModel.isCurrentUser(post.userId)
                                )
                            }

                            // Loading more indicator
                            if (feedState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = PrometheusOrange
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun FeedTypeSelector(
    currentTab: FeedTab,
    onTabSelected: (FeedTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // My Clients tab - shows posts from connected clients
        FilterChip(
            selected = currentTab == FeedTab.FOLLOWING,
            onClick = { onTabSelected(FeedTab.FOLLOWING) },
            label = { Text("My Clients") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrometheusOrange.copy(alpha = 0.2f),
                selectedLabelColor = PrometheusOrange,
                selectedLeadingIconColor = PrometheusOrange
            )
        )

        // Discover tab - shows all public community posts
        FilterChip(
            selected = currentTab == FeedTab.DISCOVER,
            onClick = { onTabSelected(FeedTab.DISCOVER) },
            label = { Text("Discover") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrometheusOrange.copy(alpha = 0.2f),
                selectedLabelColor = PrometheusOrange,
                selectedLeadingIconColor = PrometheusOrange
            )
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
        ) {
            Text("Retry")
        }
    }
}
