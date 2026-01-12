package com.prometheuscoach.mobile.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.data.repository.AlertRepository
import com.prometheuscoach.mobile.data.repository.AuthRepository
import com.prometheuscoach.mobile.data.repository.ChatRepository
import com.prometheuscoach.mobile.data.repository.ClientRepository
import com.prometheuscoach.mobile.data.repository.CommunityRepository
import com.prometheuscoach.mobile.data.repository.Connection
import com.prometheuscoach.mobile.data.repository.ConnectionManager
import com.prometheuscoach.mobile.data.repository.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// DASHBOARD STATE
// ═══════════════════════════════════════════════════════════════════════════

data class CoachDashboardState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,

    // Coach info
    val coachName: String = "",
    val coachAvatar: String? = null,
    val activeClientCount: Int = 0,

    // ═══════════════════════════════════════════════════════════════════════════
    // PENDING CONNECTION REQUESTS - Most important business feature!
    // ═══════════════════════════════════════════════════════════════════════════
    val pendingRequests: List<Connection> = emptyList(),
    val isRespondingToRequest: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.Idle,

    // ═══════════════════════════════════════════════════════════════════════════
    // UNREAD MESSAGES - Show coach new client messages
    // ═══════════════════════════════════════════════════════════════════════════
    val unreadConversations: List<ConversationWithDetails> = emptyList(),
    val totalUnreadCount: Int = 0,
    val messagesExpanded: Boolean = true,

    // Alerts section
    val alerts: List<ClientAlert> = emptyList(),
    val alertsExpanded: Boolean = false,

    // Wins section
    val wins: List<ClientWin> = emptyList(),
    val winsExpanded: Boolean = true,

    // Community feed
    val feedPosts: List<FeedPost> = emptyList(),
    val selectedFeedTab: CoachFeedTab = CoachFeedTab.MY_CLIENTS,
    val isLoadingFeed: Boolean = false,
    val postsBeingLiked: Set<String> = emptySet() // Prevent double-clicks
)

enum class CoachFeedTab {
    GLOBAL,
    MY_CLIENTS
}

// ═══════════════════════════════════════════════════════════════════════════
// VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class CoachDashboardViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val clientRepository: ClientRepository,
    private val communityRepository: CommunityRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _state = MutableStateFlow(CoachDashboardState())
    val state: StateFlow<CoachDashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
        startConnectionListening()
        observeConnections()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONNECTION MANAGEMENT - Realtime updates for pending requests
    // ═══════════════════════════════════════════════════════════════════════════

    private fun startConnectionListening() {
        connectionManager.startListening()
    }

    private fun observeConnections() {
        viewModelScope.launch {
            connectionManager.connections.collect { connections ->
                val pending = connections.filter { it.status == "pending" }
                _state.update { it.copy(pendingRequests = pending) }
            }
        }

        viewModelScope.launch {
            connectionManager.connectionState.collect { connState ->
                _state.update { it.copy(connectionState = connState) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectionManager.stopListening()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            // Refresh connections first (for pending requests)
            connectionManager.refreshConnections()
            loadDashboard()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun toggleAlertsExpanded() {
        _state.update { it.copy(alertsExpanded = !it.alertsExpanded) }
    }

    fun toggleWinsExpanded() {
        _state.update { it.copy(winsExpanded = !it.winsExpanded) }
    }

    fun toggleMessagesExpanded() {
        _state.update { it.copy(messagesExpanded = !it.messagesExpanded) }
    }

    fun selectFeedTab(tab: CoachFeedTab) {
        if (_state.value.selectedFeedTab != tab) {
            _state.update { it.copy(selectedFeedTab = tab) }
            loadFeed(tab)
        }
    }

    /**
     * Accept a pending connection request.
     * Uses the unified ConnectionManager RPC function.
     */
    fun acceptConnectionRequest(connectionId: String) {
        android.util.Log.d("CoachDashboard", "🔄 Accepting connection: $connectionId")
        viewModelScope.launch {
            _state.update { it.copy(isRespondingToRequest = true) }

            connectionManager.respondToConnection(connectionId, accept = true)
                .onSuccess { status ->
                    android.util.Log.d("CoachDashboard", "✅ Connection accepted! Status: $status")
                    // Refresh client count after accepting
                    loadClientCount()
                    _state.update { it.copy(isRespondingToRequest = false, error = null) }
                }
                .onFailure { exception ->
                    android.util.Log.e("CoachDashboard", "❌ Failed to accept connection: ${exception.message}", exception)
                    _state.update {
                        it.copy(
                            isRespondingToRequest = false,
                            error = "Failed to accept: ${exception.message}"
                        )
                    }
                }
        }
    }

    /**
     * Decline a pending connection request.
     * Uses the unified ConnectionManager RPC function.
     */
    fun declineConnectionRequest(connectionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isRespondingToRequest = true) }

            connectionManager.respondToConnection(connectionId, accept = false)
                .onSuccess {
                    _state.update { it.copy(isRespondingToRequest = false) }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isRespondingToRequest = false,
                            error = exception.message
                        )
                    }
                }
        }
    }

    fun dismissAlert(alertId: String) {
        viewModelScope.launch {
            alertRepository.dismissAlert(alertId)
            // Remove from local state
            _state.update { state ->
                state.copy(alerts = state.alerts.filter { it.id != alertId })
            }
        }
    }

    fun celebrateWin(winId: String) {
        viewModelScope.launch {
            alertRepository.markWinCelebrated(winId)
            // Update local state
            _state.update { state ->
                state.copy(wins = state.wins.map { win ->
                    if (win.id == winId) win.copy(celebrated = true) else win
                })
            }
        }
    }

    /**
     * Get or create a conversation with a client and optionally send a message.
     * Returns the conversation ID which can be used for navigation to ChatScreen.
     */
    suspend fun getOrCreateConversationAndSendMessage(clientId: String, message: String? = null): String? {
        val conversationId = chatRepository.findOrCreateConversation(clientId).getOrNull()
        if (conversationId != null && !message.isNullOrBlank()) {
            chatRepository.sendMessage(conversationId, message)
        }
        return conversationId
    }

    /**
     * Send a quick reply from the dashboard.
     * After sending, removes the conversation from unread list.
     */
    fun sendQuickReply(conversationId: String, message: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(conversationId, message)
                .onSuccess {
                    android.util.Log.d("CoachDashboard", "Quick reply sent to $conversationId")
                    // Mark as read and remove from unread list
                    chatRepository.markMessagesAsRead(conversationId)
                    // Remove from unread conversations
                    _state.update { state ->
                        val updated = state.unreadConversations.filter { it.conversationId != conversationId }
                        state.copy(
                            unreadConversations = updated,
                            totalUnreadCount = updated.sumOf { it.unreadCount }
                        )
                    }
                }
                .onFailure { e ->
                    android.util.Log.e("CoachDashboard", "Failed to send quick reply: ${e.message}")
                }
        }
    }

    /**
     * Toggle like on a feed post.
     * Uses optimistic update for instant feedback.
     * Prevents double-clicks with postsBeingLiked tracking.
     */
    fun toggleLike(postId: String) {
        // Prevent double-clicks
        if (_state.value.postsBeingLiked.contains(postId)) {
            android.util.Log.d("CoachDashboard", "toggleLike: Ignoring - already processing $postId")
            return
        }

        val currentPosts = _state.value.feedPosts
        val post = currentPosts.find { it.id == postId } ?: return
        val wasLiked = post.isLiked

        android.util.Log.d("CoachDashboard", "toggleLike: postId=$postId, wasLiked=$wasLiked, currentCount=${post.likesCount}")

        // Mark as being processed and do optimistic update
        _state.update { state ->
            state.copy(
                postsBeingLiked = state.postsBeingLiked + postId,
                feedPosts = state.feedPosts.map { p ->
                    if (p.id == postId) {
                        val newCount = if (wasLiked) {
                            maxOf(0, p.likesCount - 1) // Never go below 0
                        } else {
                            p.likesCount + 1
                        }
                        p.copy(
                            isLiked = !wasLiked,
                            likesCount = newCount
                        )
                    } else p
                }
            )
        }

        // Perform the actual API call
        viewModelScope.launch {
            val result = if (wasLiked) {
                communityRepository.unlikePost(postId)
            } else {
                communityRepository.likePost(postId)
            }

            result.onSuccess {
                android.util.Log.d("CoachDashboard", "toggleLike: Success for $postId")
            }

            result.onFailure { e ->
                android.util.Log.e("CoachDashboard", "Failed to ${if (wasLiked) "unlike" else "like"} post: ${e.message}")
                // Revert optimistic update on failure
                _state.update { state ->
                    state.copy(
                        feedPosts = state.feedPosts.map { p ->
                            if (p.id == postId) {
                                val revertedCount = if (wasLiked) {
                                    p.likesCount + 1
                                } else {
                                    maxOf(0, p.likesCount - 1)
                                }
                                p.copy(
                                    isLiked = wasLiked,
                                    likesCount = revertedCount
                                )
                            } else p
                        }
                    )
                }
            }

            // Always remove from processing set when done
            _state.update { state ->
                state.copy(postsBeingLiked = state.postsBeingLiked - postId)
            }
        }
    }

    /**
     * Add a comment to a feed post inline from the dashboard.
     * Updates the local state with the new comment for instant feedback.
     */
    fun addComment(postId: String, content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            communityRepository.addComment(postId, content)
                .onSuccess { newComment ->
                    android.util.Log.d("CoachDashboard", "Comment added to post $postId")
                    // Update local state with the new comment
                    _state.update { state ->
                        state.copy(
                            feedPosts = state.feedPosts.map { post ->
                                if (post.id == postId) {
                                    post.copy(
                                        commentsCount = post.commentsCount + 1,
                                        previewComments = (post.previewComments + newComment).takeLast(2)
                                    )
                                } else post
                            }
                        )
                    }
                }
                .onFailure { e ->
                    android.util.Log.e("CoachDashboard", "Failed to add comment: ${e.message}")
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE LOADING
    // ═══════════════════════════════════════════════════════════════════════════

    private fun loadDashboard() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // CRITICAL: Load only essential data first, then show UI immediately
                val coachNameJob = launch {
                    try { loadCoachInfo() }
                    catch (e: Exception) { android.util.Log.e("CoachDashboard", "loadCoachInfo failed", e) }
                }
                val clientCountJob = launch {
                    try { loadClientCount() }
                    catch (e: Exception) { android.util.Log.e("CoachDashboard", "loadClientCount failed", e) }
                }

                // Wait ONLY for essential data (fast queries)
                coachNameJob.join()
                clientCountJob.join()

                // Show UI immediately - don't wait for slow queries
                _state.update { it.copy(isLoading = false) }

                // Load remaining data in background (UI already visible)
                launch {
                    try { loadAlerts() }
                    catch (e: Exception) { android.util.Log.e("CoachDashboard", "loadAlerts failed", e) }
                }
                launch {
                    try { loadWins() }
                    catch (e: Exception) { android.util.Log.e("CoachDashboard", "loadWins failed", e) }
                }
                launch {
                    try { loadFeed(_state.value.selectedFeedTab) }
                    catch (e: Exception) { android.util.Log.e("CoachDashboard", "loadFeed failed", e) }
                }
                launch {
                    try { loadUnreadMessages() }
                    catch (e: Exception) { android.util.Log.e("CoachDashboard", "loadUnreadMessages failed", e) }
                }
            } catch (e: Exception) {
                android.util.Log.e("CoachDashboard", "Dashboard loading failed", e)
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load dashboard") }
            }
        }
    }

    private suspend fun loadCoachInfo() {
        authRepository.getCurrentUserProfile()
            .onSuccess { profile ->
                _state.update {
                    it.copy(
                        coachName = profile.fullName ?: "",
                        coachAvatar = profile.avatarUrl
                    )
                }
            }
    }

    private suspend fun loadClientCount() {
        clientRepository.getClientCount()
            .onSuccess { count ->
                _state.update { it.copy(activeClientCount = count) }
            }
    }

    private suspend fun loadAlerts() {
        alertRepository.getClientAlerts()
            .onSuccess { alerts ->
                _state.update { it.copy(alerts = alerts) }
            }
            .onFailure { e ->
                android.util.Log.e("CoachDashboard", "Failed to load alerts", e)
            }
    }

    private suspend fun loadWins() {
        alertRepository.getClientWins()
            .onSuccess { wins ->
                _state.update { it.copy(wins = wins) }
            }
            .onFailure { e ->
                android.util.Log.e("CoachDashboard", "Failed to load wins", e)
            }
    }

    private suspend fun loadUnreadMessages() {
        try {
            chatRepository.getConversations()
                .onSuccess { conversations ->
                    // Only show conversations with unread messages
                    val unread = conversations.filter { it.unreadCount > 0 }
                    val totalUnread = unread.sumOf { it.unreadCount }
                    _state.update {
                        it.copy(
                            unreadConversations = unread,
                            totalUnreadCount = totalUnread
                        )
                    }
                }
                .onFailure { e ->
                    android.util.Log.w("CoachDashboard", "Failed to load messages: ${e.message}")
                    // Don't fail - just show empty
                    _state.update {
                        it.copy(unreadConversations = emptyList(), totalUnreadCount = 0)
                    }
                }
        } catch (e: Exception) {
            android.util.Log.w("CoachDashboard", "Exception loading messages: ${e.message}")
            _state.update {
                it.copy(unreadConversations = emptyList(), totalUnreadCount = 0)
            }
        }
    }

    /**
     * Refresh messages section only - called after returning from chat.
     */
    fun refreshMessages() {
        viewModelScope.launch {
            loadUnreadMessages()
        }
    }

    private fun loadFeed(tab: CoachFeedTab) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingFeed = true) }

            // TODO: Add tab filtering support to CommunityRepository when needed
            // For now, load all feed posts regardless of tab
            communityRepository.getFeed()
                .onSuccess { posts ->
                    _state.update {
                        it.copy(
                            feedPosts = posts,
                            isLoadingFeed = false
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingFeed = false) }
                    android.util.Log.e("CoachDashboard", "Failed to load feed", e)
                }
        }
    }
}