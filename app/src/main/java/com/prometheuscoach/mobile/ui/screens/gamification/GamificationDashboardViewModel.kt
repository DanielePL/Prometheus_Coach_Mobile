package com.prometheuscoach.mobile.ui.screens.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.data.repository.GamificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GamificationDashboardViewModel @Inject constructor(
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GamificationDashboardState())
    val state: StateFlow<GamificationDashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    /**
     * Load all dashboard data.
     */
    fun loadDashboardData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load clients stats
            gamificationRepository.getAllClientsGamificationStats()
                .onSuccess { stats ->
                    _state.update { it.copy(clientsStats = stats) }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }

            // Load active challenges
            gamificationRepository.getActiveChallenges()
                .onSuccess { challenges ->
                    _state.update { it.copy(activeChallenges = challenges) }
                }

            // Load all badges
            gamificationRepository.getAllBadges()
                .onSuccess { badges ->
                    _state.update { it.copy(allBadges = badges) }
                }

            // Load personal challenges
            gamificationRepository.getCoachPersonalChallenges()
                .onSuccess { challenges ->
                    _state.update { it.copy(personalChallenges = challenges) }
                }

            // Load leaderboard
            loadLeaderboard()

            _state.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Load leaderboard with current type and period.
     */
    fun loadLeaderboard() {
        viewModelScope.launch {
            gamificationRepository.getClientsLeaderboard(
                type = _state.value.leaderboardType,
                period = _state.value.leaderboardPeriod
            )
                .onSuccess { entries ->
                    _state.update { it.copy(leaderboard = entries) }
                }
        }
    }

    /**
     * Select a client to view details.
     */
    fun selectClient(clientId: String?) {
        viewModelScope.launch {
            if (clientId == null) {
                _state.update {
                    it.copy(
                        selectedClientId = null,
                        selectedClientStats = null,
                        clientBadges = emptyList(),
                        clientChallengeEntries = emptyList()
                    )
                }
                return@launch
            }

            _state.update { it.copy(selectedClientId = clientId) }

            // Load client stats
            gamificationRepository.getClientGamificationStats(clientId)
                .onSuccess { stats ->
                    _state.update { it.copy(selectedClientStats = stats) }
                }

            // Load client badges
            gamificationRepository.getClientBadges(clientId)
                .onSuccess { badges ->
                    _state.update { it.copy(clientBadges = badges) }
                }

            // Load client challenge entries
            gamificationRepository.getClientChallengeEntries(clientId)
                .onSuccess { entries ->
                    _state.update { it.copy(clientChallengeEntries = entries) }
                }
        }
    }

    /**
     * Change selected tab.
     */
    fun selectTab(tab: GamificationTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    /**
     * Change leaderboard type.
     */
    fun setLeaderboardType(type: LeaderboardType) {
        _state.update { it.copy(leaderboardType = type) }
        loadLeaderboard()
    }

    /**
     * Change leaderboard period.
     */
    fun setLeaderboardPeriod(period: LeaderboardPeriod) {
        _state.update { it.copy(leaderboardPeriod = period) }
        loadLeaderboard()
    }

    /**
     * Show create challenge dialog.
     */
    fun showCreateChallengeDialog() {
        _state.update { it.copy(showCreateChallengeDialog = true) }
    }

    /**
     * Hide create challenge dialog.
     */
    fun hideCreateChallengeDialog() {
        _state.update { it.copy(showCreateChallengeDialog = false) }
    }

    /**
     * Create a personal challenge for a client.
     */
    fun createPersonalChallenge(
        clientId: String,
        title: String,
        description: String?,
        targetType: PersonalChallengeType,
        targetValue: Int,
        durationDays: Int,
        rewardXp: Int,
        rewardMessage: String?
    ) {
        viewModelScope.launch {
            gamificationRepository.createPersonalChallenge(
                clientId = clientId,
                title = title,
                description = description,
                targetType = targetType,
                targetValue = targetValue,
                durationDays = durationDays,
                rewardXp = rewardXp,
                rewardMessage = rewardMessage
            )
                .onSuccess { challenge ->
                    _state.update {
                        it.copy(
                            personalChallenges = listOf(challenge) + it.personalChallenges,
                            showCreateChallengeDialog = false
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    /**
     * Cancel a personal challenge.
     */
    fun cancelPersonalChallenge(challengeId: String) {
        viewModelScope.launch {
            gamificationRepository.cancelPersonalChallenge(challengeId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            personalChallenges = it.personalChallenges.map { challenge ->
                                if (challenge.id == challengeId) {
                                    challenge.copy(status = PersonalChallengeStatus.CANCELLED)
                                } else {
                                    challenge
                                }
                            }
                        )
                    }
                }
        }
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

/**
 * ViewModel for client-specific gamification view.
 */
@HiltViewModel
class ClientGamificationViewModel @Inject constructor(
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ClientGamificationState())
    val state: StateFlow<ClientGamificationState> = _state.asStateFlow()

    /**
     * Initialize with client data.
     */
    fun initClient(clientId: String, clientName: String) {
        _state.update { it.copy(clientId = clientId, clientName = clientName) }
        loadClientData(clientId)
    }

    /**
     * Load all data for a client.
     */
    fun loadClientData(clientId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load stats
            gamificationRepository.getClientGamificationStats(clientId)
                .onSuccess { stats ->
                    _state.update { it.copy(stats = stats) }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }

            // Load badges
            gamificationRepository.getClientBadges(clientId)
                .onSuccess { badges ->
                    _state.update { it.copy(badges = badges) }
                }

            // Load challenge entries
            gamificationRepository.getClientChallengeEntries(clientId)
                .onSuccess { entries ->
                    _state.update { it.copy(challengeEntries = entries) }
                }

            // Load personal challenges
            gamificationRepository.getClientPersonalChallenges(clientId)
                .onSuccess { challenges ->
                    _state.update { it.copy(personalChallenges = challenges) }
                }

            _state.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Show create challenge sheet.
     */
    fun showCreateChallengeSheet() {
        _state.update { it.copy(showCreateChallengeSheet = true) }
    }

    /**
     * Hide create challenge sheet.
     */
    fun hideCreateChallengeSheet() {
        _state.update { it.copy(showCreateChallengeSheet = false) }
    }

    /**
     * Create a personal challenge.
     */
    fun createChallenge(
        title: String,
        description: String?,
        targetType: PersonalChallengeType,
        targetValue: Int,
        durationDays: Int,
        rewardXp: Int,
        rewardMessage: String?
    ) {
        viewModelScope.launch {
            gamificationRepository.createPersonalChallenge(
                clientId = _state.value.clientId,
                title = title,
                description = description,
                targetType = targetType,
                targetValue = targetValue,
                durationDays = durationDays,
                rewardXp = rewardXp,
                rewardMessage = rewardMessage
            )
                .onSuccess { challenge ->
                    _state.update {
                        it.copy(
                            personalChallenges = listOf(challenge) + it.personalChallenges,
                            showCreateChallengeSheet = false
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    /**
     * Cancel a personal challenge.
     */
    fun cancelChallenge(challengeId: String) {
        viewModelScope.launch {
            gamificationRepository.cancelPersonalChallenge(challengeId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            personalChallenges = it.personalChallenges.map { challenge ->
                                if (challenge.id == challengeId) {
                                    challenge.copy(status = PersonalChallengeStatus.CANCELLED)
                                } else {
                                    challenge
                                }
                            }
                        )
                    }
                }
        }
    }

    /**
     * Clear error.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
