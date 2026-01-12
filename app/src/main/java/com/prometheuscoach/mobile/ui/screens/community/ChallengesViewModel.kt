package com.prometheuscoach.mobile.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.data.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Challenges Dashboard.
 */
data class ChallengesDashboardState(
    val isLoading: Boolean = false,
    val error: String? = null,
    // Challenges
    val activeChallenges: List<Challenge> = emptyList(),
    val upcomingChallenges: List<Challenge> = emptyList(),
    val completedChallenges: List<Challenge> = emptyList(),
    // Max Out Friday
    val maxOutFriday: MaxOutFridayInfo? = null,
    val maxOutLeaderboard: List<ChallengeEntry> = emptyList(),
    val previousWinners: List<PreviousWinner> = emptyList(),
    // Selected challenge detail
    val selectedChallenge: Challenge? = null,
    val selectedChallengeEntries: List<ChallengeEntry> = emptyList(),
    val clientsInChallenge: List<ChallengeEntry> = emptyList(),
    val isLoadingDetail: Boolean = false,
    // Tab
    val selectedTab: ChallengesDashboardTab = ChallengesDashboardTab.ACTIVE,
    // Filter
    val showOnlyClients: Boolean = false
)

enum class ChallengesDashboardTab {
    ACTIVE,
    MAX_OUT_FRIDAY,
    HISTORY
}

@HiltViewModel
class ChallengesViewModel @Inject constructor(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChallengesDashboardState())
    val state: StateFlow<ChallengesDashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    /**
     * Load all dashboard data.
     */
    fun loadDashboardData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load all challenges
            communityRepository.getAllChallenges()
                .onSuccess { challengesState ->
                    _state.update {
                        it.copy(
                            activeChallenges = challengesState.activeChallenges,
                            upcomingChallenges = challengesState.upcomingChallenges,
                            completedChallenges = challengesState.completedChallenges
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }

            // Load Max Out Friday
            communityRepository.getCurrentMaxOutFriday()
                .onSuccess { maxOut ->
                    _state.update { it.copy(maxOutFriday = maxOut) }
                    // Load leaderboard if available
                    maxOut?.let { loadMaxOutLeaderboard(it.id) }
                }

            // Load previous winners
            communityRepository.getPreviousWinners()
                .onSuccess { winners ->
                    _state.update { it.copy(previousWinners = winners) }
                }

            _state.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Load Max Out Friday leaderboard.
     */
    private fun loadMaxOutLeaderboard(challengeId: String) {
        viewModelScope.launch {
            communityRepository.getMaxOutFridayLeaderboard(challengeId)
                .onSuccess { entries ->
                    _state.update { it.copy(maxOutLeaderboard = entries) }
                }
        }
    }

    /**
     * Select a challenge to view details.
     */
    fun selectChallenge(challenge: Challenge?) {
        if (challenge == null) {
            _state.update {
                it.copy(
                    selectedChallenge = null,
                    selectedChallengeEntries = emptyList(),
                    clientsInChallenge = emptyList()
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    selectedChallenge = challenge,
                    isLoadingDetail = true
                )
            }

            // Load full leaderboard
            communityRepository.getChallengeLeaderboard(challenge.id)
                .onSuccess { entries ->
                    _state.update { it.copy(selectedChallengeEntries = entries) }
                }

            // Load clients in this challenge
            communityRepository.getClientsInChallenge(challenge.id)
                .onSuccess { clients ->
                    _state.update { it.copy(clientsInChallenge = clients) }
                }

            _state.update { it.copy(isLoadingDetail = false) }
        }
    }

    /**
     * Change selected tab.
     */
    fun selectTab(tab: ChallengesDashboardTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    /**
     * Toggle show only clients filter.
     */
    fun toggleShowOnlyClients() {
        _state.update { it.copy(showOnlyClients = !it.showOnlyClients) }
    }

    /**
     * Clear selected challenge.
     */
    fun clearSelectedChallenge() {
        selectChallenge(null)
    }

    /**
     * Clear error.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Refresh data.
     */
    fun refresh() {
        loadDashboardData()
    }
}
