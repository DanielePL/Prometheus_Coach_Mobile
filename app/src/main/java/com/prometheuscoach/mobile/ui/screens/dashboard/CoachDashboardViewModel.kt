package com.prometheuscoach.mobile.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.data.repository.AlertRepository
import com.prometheuscoach.mobile.data.repository.AuthRepository
import com.prometheuscoach.mobile.data.repository.ChatRepository
import com.prometheuscoach.mobile.data.repository.ClientRepository
import com.prometheuscoach.mobile.data.repository.CommunityRepository
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

    // Alerts section
    val alerts: List<ClientAlert> = emptyList(),
    val alertsExpanded: Boolean = true,

    // Wins section
    val wins: List<ClientWin> = emptyList(),
    val winsExpanded: Boolean = true,

    // Community feed
    val feedPosts: List<FeedPost> = emptyList(),
    val selectedFeedTab: CoachFeedTab = CoachFeedTab.MY_CLIENTS,
    val isLoadingFeed: Boolean = false
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
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CoachDashboardState())
    val state: StateFlow<CoachDashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
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

    fun selectFeedTab(tab: CoachFeedTab) {
        if (_state.value.selectedFeedTab != tab) {
            _state.update { it.copy(selectedFeedTab = tab) }
            loadFeed(tab)
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

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE LOADING
    // ═══════════════════════════════════════════════════════════════════════════

    private fun loadDashboard() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Load all data in parallel
                val coachNameJob = launch { loadCoachInfo() }
                val clientCountJob = launch { loadClientCount() }
                val alertsJob = launch { loadAlerts() }
                val winsJob = launch { loadWins() }
                val feedJob = launch { loadFeed(_state.value.selectedFeedTab) }

                // Wait for all
                coachNameJob.join()
                clientCountJob.join()
                alertsJob.join()
                winsJob.join()
                feedJob.join()

                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load dashboard"
                    )
                }
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