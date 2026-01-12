package com.prometheuscoach.mobile.ui.screens.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.ProgramWeekWithWorkouts
import com.prometheuscoach.mobile.data.model.ProgramWithWeeks
import com.prometheuscoach.mobile.data.model.WorkoutSummary
import com.prometheuscoach.mobile.data.repository.ProgramRepository
import com.prometheuscoach.mobile.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramDetailState(
    val program: ProgramWithWeeks? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val availableWorkouts: List<WorkoutSummary> = emptyList(),
    val isAddingWorkout: Boolean = false,
    val addWorkoutError: String? = null,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false
)

@HiltViewModel
class ProgramDetailViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProgramDetailState())
    val state: StateFlow<ProgramDetailState> = _state.asStateFlow()

    fun loadProgram(programId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            programRepository.getProgramWithWeeks(programId)
                .onSuccess { program ->
                    _state.value = _state.value.copy(
                        program = program,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load program"
                    )
                }
        }
    }

    fun loadAvailableWorkouts() {
        viewModelScope.launch {
            workoutRepository.getWorkouts()
                .onSuccess { workouts ->
                    _state.value = _state.value.copy(availableWorkouts = workouts)
                }
        }
    }

    fun addWorkoutToWeek(
        weekId: String,
        workoutId: String,
        dayNumber: Int,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isAddingWorkout = true, addWorkoutError = null)

            programRepository.addWorkoutToWeek(
                programWeekId = weekId,
                workoutId = workoutId,
                dayNumber = dayNumber,
                notes = notes
            )
                .onSuccess {
                    _state.value = _state.value.copy(isAddingWorkout = false)
                    // Reload program to get updated data
                    _state.value.program?.let { loadProgram(it.id) }
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isAddingWorkout = false,
                        addWorkoutError = error.message ?: "Failed to add workout"
                    )
                }
        }
    }

    fun removeWorkoutFromWeek(programWorkoutId: String) {
        viewModelScope.launch {
            programRepository.removeWorkoutFromWeek(programWorkoutId)
                .onSuccess {
                    // Reload program to get updated data
                    _state.value.program?.let { loadProgram(it.id) }
                }
        }
    }

    fun deleteProgram() {
        val programId = _state.value.program?.id ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isDeleting = true)

            programRepository.deleteProgram(programId)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        deleteSuccess = true
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        error = error.message ?: "Failed to delete program"
                    )
                }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null, addWorkoutError = null)
    }
}