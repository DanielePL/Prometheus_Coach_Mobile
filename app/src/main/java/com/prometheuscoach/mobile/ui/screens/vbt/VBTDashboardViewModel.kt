package com.prometheuscoach.mobile.ui.screens.vbt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.data.repository.VBTRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VBTDashboardViewModel @Inject constructor(
    private val vbtRepository: VBTRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VBTDashboardState())
    val state: StateFlow<VBTDashboardState> = _state.asStateFlow()

    init {
        // Observe live session updates
        viewModelScope.launch {
            vbtRepository.liveSession.collect { session ->
                _state.update { it.copy(liveSession = session) }
            }
        }
        viewModelScope.launch {
            vbtRepository.liveVelocityEntries.collect { entries ->
                _state.update { it.copy(liveVelocityEntries = entries) }
                // Recalculate fatigue if in live mode
                if (_state.value.isLiveMode && entries.isNotEmpty()) {
                    val fatigue = vbtRepository.calculateFatigueIndex(
                        entries = entries,
                        clientId = _state.value.clientId,
                        exerciseId = _state.value.selectedExerciseId
                    )
                    _state.update { it.copy(currentFatigue = fatigue) }
                }
            }
        }
    }

    /**
     * Initialize the dashboard for a client.
     */
    fun loadClient(clientId: String, clientName: String) {
        _state.update {
            it.copy(
                clientId = clientId,
                clientName = clientName,
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            // Load available exercises with VBT data
            loadVBTExercises(clientId)

            // Load historical data
            loadVelocityHistory(clientId)
            loadProfiles(clientId)
            load1RMPredictions(clientId)

            _state.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Load exercises that have VBT data.
     */
    private suspend fun loadVBTExercises(clientId: String) {
        vbtRepository.getVBTExercises(clientId)
            .onSuccess { exercises ->
                _state.update {
                    it.copy(
                        availableExercises = exercises,
                        selectedExerciseId = exercises.firstOrNull()?.exerciseId
                    )
                }
            }
            .onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
    }

    /**
     * Load velocity history.
     */
    private suspend fun loadVelocityHistory(clientId: String, exerciseId: String? = null) {
        vbtRepository.getVelocityHistory(
            clientId = clientId,
            exerciseId = exerciseId,
            limit = 200
        )
            .onSuccess { entries ->
                _state.update { it.copy(velocityHistory = entries) }
            }
            .onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
    }

    /**
     * Load L-V profiles.
     */
    private suspend fun loadProfiles(clientId: String) {
        vbtRepository.getLoadVelocityProfiles(clientId)
            .onSuccess { profiles ->
                _state.update { it.copy(loadVelocityProfiles = profiles) }
            }
    }

    /**
     * Load 1RM predictions.
     */
    private suspend fun load1RMPredictions(clientId: String) {
        vbtRepository.get1RMPredictions(clientId)
            .onSuccess { predictions ->
                _state.update { it.copy(oneRmPredictions = predictions) }
            }
    }

    /**
     * Toggle between live and history mode.
     */
    fun toggleLiveMode(enabled: Boolean) {
        _state.update { it.copy(isLiveMode = enabled) }

        if (enabled) {
            startLiveSession()
        } else {
            stopLiveSession()
        }
    }

    /**
     * Start live session for the current client.
     */
    private fun startLiveSession() {
        val state = _state.value
        vbtRepository.startLiveSession(state.clientId, state.clientName)
    }

    /**
     * Stop the live session.
     */
    private fun stopLiveSession() {
        vbtRepository.stopLiveSession()
    }

    /**
     * Select a different exercise to view.
     */
    fun selectExercise(exerciseId: String) {
        _state.update { it.copy(selectedExerciseId = exerciseId) }

        // Reload data for selected exercise
        viewModelScope.launch {
            loadVelocityHistory(_state.value.clientId, exerciseId)
        }
    }

    /**
     * Calculate readiness from a warmup velocity.
     */
    fun calculateReadiness(warmupVelocity: Double) {
        val state = _state.value
        val exerciseId = state.selectedExerciseId ?: return

        viewModelScope.launch {
            val readiness = vbtRepository.calculateReadiness(
                clientId = state.clientId,
                exerciseId = exerciseId,
                warmupVelocity = warmupVelocity
            )
            _state.update { it.copy(readiness = readiness) }
        }
    }

    /**
     * Refresh all data.
     */
    fun refresh() {
        val state = _state.value
        if (state.clientId.isNotBlank()) {
            loadClient(state.clientId, state.clientName)
        }
    }

    /**
     * Get L-V profile for the currently selected exercise.
     */
    fun getSelectedProfile(): LoadVelocityProfile? {
        val selectedId = _state.value.selectedExerciseId ?: return null
        return _state.value.loadVelocityProfiles.find { it.exerciseId == selectedId }
    }

    /**
     * Get 1RM prediction for the currently selected exercise.
     */
    fun getSelected1RMPrediction(): OneRMPrediction? {
        val selectedId = _state.value.selectedExerciseId ?: return null
        return _state.value.oneRmPredictions
            .filter { it.exerciseId == selectedId }
            .maxByOrNull { it.calculatedAt }
    }

    /**
     * Get velocity history for the currently selected exercise.
     */
    fun getSelectedVelocityHistory(): List<VelocityEntry> {
        val selectedId = _state.value.selectedExerciseId ?: return _state.value.velocityHistory
        return _state.value.velocityHistory.filter { it.exerciseId == selectedId }
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveSession()
    }
}
