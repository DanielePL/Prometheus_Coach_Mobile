package com.prometheuscoach.mobile.ui.screens.workouts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.data.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplateDetailState(
    val template: TemplateWithExercises? = null,
    val selectedLevel: FitnessLevel = FitnessLevel.INTERMEDIATE,
    val scalingPercentage: Int = 100,
    val isLoading: Boolean = true,
    val error: String? = null,

    // Clone state
    val isCloning: Boolean = false,
    val cloneError: String? = null,
    val clonedWorkoutId: String? = null
)

@HiltViewModel
class TemplateDetailViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val templateId: String = checkNotNull(savedStateHandle["templateId"])

    private val _state = MutableStateFlow(TemplateDetailState())
    val state: StateFlow<TemplateDetailState> = _state.asStateFlow()

    init {
        loadTemplate()
    }

    fun loadTemplate() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            templateRepository.getTemplateWithExercises(templateId)
                .onSuccess { template ->
                    _state.value = _state.value.copy(
                        template = template,
                        selectedLevel = template.template.level,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load template"
                    )
                }
        }
    }

    fun selectLevel(level: FitnessLevel) {
        _state.value = _state.value.copy(selectedLevel = level)
    }

    fun setScalingPercentage(percentage: Int) {
        _state.value = _state.value.copy(scalingPercentage = percentage.coerceIn(50, 100))
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            templateRepository.toggleFavorite(templateId)
                .onSuccess { isFavorite ->
                    _state.value.template?.let { current ->
                        _state.value = _state.value.copy(
                            template = current.copy(isFavorite = isFavorite)
                        )
                    }
                }
        }
    }

    suspend fun cloneToWorkout(customName: String? = null): Result<String> {
        _state.value = _state.value.copy(isCloning = true, cloneError = null)

        val config = TemplateCloneConfig(
            templateId = templateId,
            targetLevel = _state.value.selectedLevel,
            scalingPercentage = _state.value.scalingPercentage,
            customName = customName
        )

        return templateRepository.cloneTemplateToWorkout(config)
            .onSuccess { workoutId ->
                _state.value = _state.value.copy(
                    isCloning = false,
                    clonedWorkoutId = workoutId
                )
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isCloning = false,
                    cloneError = error.message ?: "Failed to clone template"
                )
            }
    }

    fun clearClonedWorkout() {
        _state.value = _state.value.copy(clonedWorkoutId = null)
    }
}
