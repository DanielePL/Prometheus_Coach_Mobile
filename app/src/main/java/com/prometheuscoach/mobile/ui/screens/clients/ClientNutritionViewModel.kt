package com.prometheuscoach.mobile.ui.screens.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.data.repository.NutritionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientNutritionState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val logs: List<NutritionLog> = emptyList(),
    val summaries: List<DailyNutritionSummary> = emptyList(),
    val goal: NutritionGoal? = null,
    val weeklySummary: WeeklyNutritionSummary? = null,
    val isLoadingWeekly: Boolean = false,
    val isSavingGoal: Boolean = false,
    val goalSaveError: String? = null,
    val goalSaved: Boolean = false
)

@HiltViewModel
class ClientNutritionViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ClientNutritionState())
    val state: StateFlow<ClientNutritionState> = _state.asStateFlow()

    private var currentClientId: String? = null

    fun loadNutrition(clientId: String) {
        if (currentClientId == clientId && _state.value.logs.isNotEmpty()) {
            return // Already loaded
        }
        currentClientId = clientId

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load main nutrition data
            nutritionRepository.getClientNutrition(clientId, 30)
                .onSuccess { data ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            logs = data.logs,
                            summaries = data.summaries,
                            goal = data.goal
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load nutrition data"
                        )
                    }
                }

            // Load weekly summary in parallel
            loadWeeklySummary(clientId)
        }
    }

    private fun loadWeeklySummary(clientId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingWeekly = true) }

            nutritionRepository.getClientNutritionWeekly(clientId)
                .onSuccess { summary ->
                    _state.update {
                        it.copy(
                            isLoadingWeekly = false,
                            weeklySummary = summary
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoadingWeekly = false) }
                }
        }
    }

    fun setNutritionGoal(
        clientId: String,
        goalType: String,
        targetCalories: Float,
        targetProtein: Float,
        targetCarbs: Float,
        targetFat: Float
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSavingGoal = true, goalSaveError = null, goalSaved = false) }

            nutritionRepository.setClientNutritionGoal(
                clientId = clientId,
                goalType = goalType,
                targetCalories = targetCalories,
                targetProtein = targetProtein,
                targetCarbs = targetCarbs,
                targetFat = targetFat
            )
                .onSuccess { newGoal ->
                    _state.update {
                        it.copy(
                            isSavingGoal = false,
                            goal = newGoal,
                            goalSaved = true
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isSavingGoal = false,
                            goalSaveError = e.message ?: "Failed to save goal"
                        )
                    }
                }
        }
    }

    fun clearGoalSavedFlag() {
        _state.update { it.copy(goalSaved = false) }
    }

    fun refresh(clientId: String) {
        currentClientId = null
        loadNutrition(clientId)
    }

    // Get today's summary
    fun getTodaySummary(): DailyNutritionSummary? {
        return _state.value.summaries.firstOrNull()
    }

    // Get calorie trend data (last 14 days, reversed for chart)
    fun getCalorieTrendData(): List<DailyNutritionSummary> {
        return _state.value.summaries.take(14).reversed()
    }

    // Get recent logs (last 7 days)
    fun getRecentLogs(): List<NutritionLog> {
        return _state.value.logs.take(7)
    }
}