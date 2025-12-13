package com.prometheuscoach.mobile.ui.screens.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.data.repository.ClientRepository
import com.prometheuscoach.mobile.data.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientProgressState(
    val client: Client? = null,
    val isLoading: Boolean = true,
    val error: String? = null,

    // Progress data
    val summary: ProgressSummary? = null,
    val weeklyProgress: List<WeeklyProgress> = emptyList(),
    val recentWorkouts: List<WorkoutLog> = emptyList(),
    val personalBests: List<PersonalBest> = emptyList(),

    // UI state
    val selectedPeriod: String = "month",
    val isLoadingWorkouts: Boolean = false
)

@HiltViewModel
class ClientProgressViewModel @Inject constructor(
    private val clientRepository: ClientRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _progressState = MutableStateFlow(ClientProgressState())
    val progressState: StateFlow<ClientProgressState> = _progressState.asStateFlow()

    private var currentClientId: String? = null

    fun loadClientProgress(clientId: String) {
        if (currentClientId == clientId && _progressState.value.client != null) {
            return // Already loaded
        }

        currentClientId = clientId
        viewModelScope.launch {
            _progressState.update { it.copy(isLoading = true, error = null) }

            // Load client info
            clientRepository.getClientById(clientId)
                .onSuccess { client ->
                    _progressState.update { it.copy(client = client) }
                }
                .onFailure { e ->
                    _progressState.update {
                        it.copy(isLoading = false, error = e.message)
                    }
                    return@launch
                }

            // Load progress data in parallel
            loadProgressData(clientId)
        }
    }

    private suspend fun loadProgressData(clientId: String) {
        val period = _progressState.value.selectedPeriod

        // Load summary
        progressRepository.getProgressSummary(clientId, period)
            .onSuccess { summary ->
                _progressState.update { it.copy(summary = summary) }
            }

        // Load weekly progress
        progressRepository.getWeeklyProgress(clientId)
            .onSuccess { weekly ->
                _progressState.update { it.copy(weeklyProgress = weekly) }
            }

        // Load recent workouts
        progressRepository.getWorkoutLogs(clientId, limit = 10)
            .onSuccess { logs ->
                _progressState.update { it.copy(recentWorkouts = logs) }
            }

        // Load personal bests
        progressRepository.getPersonalBests(clientId)
            .onSuccess { pbs ->
                _progressState.update { it.copy(personalBests = pbs.take(5)) }
            }

        _progressState.update { it.copy(isLoading = false) }
    }

    fun selectPeriod(period: String) {
        if (_progressState.value.selectedPeriod == period) return

        _progressState.update { it.copy(selectedPeriod = period) }

        currentClientId?.let { clientId ->
            viewModelScope.launch {
                progressRepository.getProgressSummary(clientId, period)
                    .onSuccess { summary ->
                        _progressState.update { it.copy(summary = summary) }
                    }
            }
        }
    }

    fun refresh() {
        currentClientId?.let { clientId ->
            viewModelScope.launch {
                _progressState.update { it.copy(isLoading = true) }
                loadProgressData(clientId)
            }
        }
    }
}
