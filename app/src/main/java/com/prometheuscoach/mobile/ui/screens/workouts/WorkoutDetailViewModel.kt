package com.prometheuscoach.mobile.ui.screens.workouts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.ExerciseListItem
import com.prometheuscoach.mobile.data.model.WorkoutExerciseDetail
import com.prometheuscoach.mobile.data.model.WorkoutWithExercises
import com.prometheuscoach.mobile.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutDetailState(
    val workout: WorkoutWithExercises? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null
)

data class ExercisePickerState(
    val exercises: List<ExerciseListItem> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val categories: List<String> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutDetailState())
    val state: StateFlow<WorkoutDetailState> = _state.asStateFlow()

    private val _pickerState = MutableStateFlow(ExercisePickerState())
    val pickerState: StateFlow<ExercisePickerState> = _pickerState.asStateFlow()

    private var currentWorkoutId: String? = null

    init {
        savedStateHandle.get<String>("workoutId")?.let { workoutId ->
            loadWorkout(workoutId)
        }
    }

    fun loadWorkout(workoutId: String) {
        currentWorkoutId = workoutId
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            workoutRepository.getWorkoutWithExercises(workoutId)
                .onSuccess { workoutWithExercises ->
                    _state.value = _state.value.copy(
                        workout = workoutWithExercises,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load workout"
                    )
                }
        }
    }

    fun refresh() {
        currentWorkoutId?.let { loadWorkout(it) }
    }

    // ==================== WORKOUT OPERATIONS ====================

    /**
     * Update workout name and description.
     */
    suspend fun updateWorkout(name: String, description: String?): Result<Unit> {
        val workoutId = currentWorkoutId ?: return Result.failure(Exception("No workout loaded"))

        _state.value = _state.value.copy(isSaving = true, saveError = null)

        return workoutRepository.updateWorkout(workoutId, name, description)
            .onSuccess {
                _state.value = _state.value.copy(isSaving = false)
                loadWorkout(workoutId) // Refresh
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveError = error.message ?: "Failed to update workout"
                )
            }
    }

    // ==================== EXERCISE OPERATIONS ====================

    /**
     * Add an exercise to the workout.
     */
    suspend fun addExercise(
        exerciseId: String,
        sets: Int = 3,
        targetReps: Int? = null,
        notes: String? = null
    ): Result<Unit> {
        val workoutId = currentWorkoutId ?: return Result.failure(Exception("No workout loaded"))
        val currentExercises = _state.value.workout?.exercises ?: emptyList()
        val nextOrderIndex = currentExercises.maxOfOrNull { it.orderIndex + 1 } ?: 0

        _state.value = _state.value.copy(isSaving = true, saveError = null)

        return workoutRepository.addExerciseToWorkout(
            workoutId = workoutId,
            exerciseId = exerciseId,
            orderIndex = nextOrderIndex,
            sets = sets,
            targetReps = targetReps,
            notes = notes
        ).map { }
            .onSuccess {
                _state.value = _state.value.copy(isSaving = false)
                loadWorkout(workoutId) // Refresh
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveError = error.message ?: "Failed to add exercise"
                )
            }
    }

    /**
     * Remove an exercise from the workout.
     */
    suspend fun removeExercise(workoutExerciseId: String): Result<Unit> {
        val workoutId = currentWorkoutId ?: return Result.failure(Exception("No workout loaded"))

        _state.value = _state.value.copy(isSaving = true, saveError = null)

        return workoutRepository.removeExerciseFromWorkout(workoutExerciseId, workoutId)
            .onSuccess {
                _state.value = _state.value.copy(isSaving = false)
                loadWorkout(workoutId) // Refresh
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveError = error.message ?: "Failed to remove exercise"
                )
            }
    }

    /**
     * Update exercise parameters.
     */
    suspend fun updateExercise(
        workoutExerciseId: String,
        sets: Int? = null,
        targetReps: Int? = null,
        notes: String? = null
    ): Result<Unit> {
        val workoutId = currentWorkoutId ?: return Result.failure(Exception("No workout loaded"))

        _state.value = _state.value.copy(isSaving = true, saveError = null)

        return workoutRepository.updateWorkoutExercise(
            workoutExerciseId = workoutExerciseId,
            sets = sets,
            targetReps = targetReps,
            notes = notes
        ).onSuccess {
            _state.value = _state.value.copy(isSaving = false)
            loadWorkout(workoutId) // Refresh
        }.onFailure { error ->
            _state.value = _state.value.copy(
                isSaving = false,
                saveError = error.message ?: "Failed to update exercise"
            )
        }
    }

    /**
     * Reorder exercises (after drag & drop).
     */
    suspend fun reorderExercises(exercises: List<WorkoutExerciseDetail>): Result<Unit> {
        val workoutId = currentWorkoutId ?: return Result.failure(Exception("No workout loaded"))
        val orderedIds = exercises.map { it.workoutExerciseId }

        _state.value = _state.value.copy(isSaving = true, saveError = null)

        return workoutRepository.reorderExercises(workoutId, orderedIds)
            .onSuccess {
                _state.value = _state.value.copy(isSaving = false)
                // Update local state immediately without refetch for smooth UX
                _state.value.workout?.let { workout ->
                    val reorderedExercises = exercises.mapIndexed { index, exercise ->
                        exercise.copy(orderIndex = index)
                    }
                    _state.value = _state.value.copy(
                        workout = workout.copy(exercises = reorderedExercises)
                    )
                }
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveError = error.message ?: "Failed to reorder exercises"
                )
            }
    }

    // ==================== EXERCISE PICKER ====================

    /**
     * Load exercises for the picker.
     */
    fun loadExercisesForPicker(query: String = "") {
        viewModelScope.launch {
            _pickerState.value = _pickerState.value.copy(
                isLoading = true,
                searchQuery = query,
                error = null
            )

            workoutRepository.searchExercises(query, limit = 100)
                .onSuccess { exercises ->
                    // Extract unique categories from exercises
                    val categories = exercises
                        .mapNotNull { it.category ?: it.mainMuscleGroup }
                        .distinct()
                        .sorted()

                    // Filter by selected category if any
                    val filteredExercises = _pickerState.value.selectedCategory?.let { category ->
                        exercises.filter {
                            (it.category ?: it.mainMuscleGroup) == category
                        }
                    } ?: exercises

                    _pickerState.value = _pickerState.value.copy(
                        exercises = filteredExercises,
                        categories = categories,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _pickerState.value = _pickerState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load exercises"
                    )
                }
        }
    }

    /**
     * Set category filter for exercise picker.
     */
    fun setExerciseCategoryFilter(category: String?) {
        _pickerState.value = _pickerState.value.copy(selectedCategory = category)
        // Reload with current search query
        loadExercisesForPicker(_pickerState.value.searchQuery)
    }

    fun clearSaveError() {
        _state.value = _state.value.copy(saveError = null)
    }
}
