package com.prometheuscoach.mobile.ui.screens.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.ExerciseListItem
import com.prometheuscoach.mobile.data.model.WorkoutSummary
import com.prometheuscoach.mobile.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutsState(
    val workouts: List<WorkoutSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCreating: Boolean = false,
    val createError: String? = null
)

data class ExerciseLibraryState(
    val exercises: List<ExerciseListItem> = emptyList(),
    val allExercises: List<ExerciseListItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WorkoutsViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutsState())
    val state: StateFlow<WorkoutsState> = _state.asStateFlow()

    private val _libraryState = MutableStateFlow(ExerciseLibraryState())
    val libraryState: StateFlow<ExerciseLibraryState> = _libraryState.asStateFlow()

    init {
        loadWorkouts()
    }

    fun loadWorkouts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            workoutRepository.getWorkouts()
                .onSuccess { workouts ->
                    _state.value = _state.value.copy(
                        workouts = workouts,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load workouts"
                    )
                }
        }
    }

    fun refresh() {
        loadWorkouts()
    }

    /**
     * Create a new workout.
     * Returns the new workout's ID on success.
     */
    suspend fun createWorkout(name: String, description: String?): Result<String> {
        _state.value = _state.value.copy(isCreating = true, createError = null)

        return workoutRepository.createWorkout(name, description)
            .map { workout ->
                _state.value = _state.value.copy(isCreating = false)
                loadWorkouts() // Refresh list
                workout.id
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isCreating = false,
                    createError = error.message ?: "Failed to create workout"
                )
            }
    }

    /**
     * Delete a workout.
     */
    suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        return workoutRepository.deleteWorkout(workoutId)
            .onSuccess {
                loadWorkouts() // Refresh list
            }
    }

    fun clearCreateError() {
        _state.value = _state.value.copy(createError = null)
    }

    // ==================== EXERCISE LIBRARY ====================

    /**
     * Load exercise library from Supabase.
     */
    fun loadExerciseLibrary() {
        viewModelScope.launch {
            _libraryState.value = _libraryState.value.copy(isLoading = true, error = null)

            workoutRepository.searchExercises("", limit = 500)
                .onSuccess { exercises ->
                    // Extract unique categories
                    val categories = exercises
                        .mapNotNull { it.category ?: it.mainMuscleGroup }
                        .distinct()
                        .sorted()

                    _libraryState.value = _libraryState.value.copy(
                        exercises = exercises,
                        allExercises = exercises,
                        categories = categories,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _libraryState.value = _libraryState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load exercises"
                    )
                }
        }
    }

    /**
     * Filter library by category.
     */
    fun setLibraryCategoryFilter(category: String?) {
        val allExercises = _libraryState.value.allExercises
        val searchQuery = _libraryState.value.searchQuery

        val filtered = allExercises.filter { exercise ->
            val matchesCategory = category == null ||
                (exercise.category ?: exercise.mainMuscleGroup) == category
            val matchesSearch = searchQuery.isBlank() ||
                exercise.name.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        _libraryState.value = _libraryState.value.copy(
            exercises = filtered,
            selectedCategory = category
        )
    }

    /**
     * Search exercises in library.
     */
    fun searchExercises(query: String) {
        val allExercises = _libraryState.value.allExercises
        val selectedCategory = _libraryState.value.selectedCategory

        val filtered = allExercises.filter { exercise ->
            val matchesCategory = selectedCategory == null ||
                (exercise.category ?: exercise.mainMuscleGroup) == selectedCategory
            val matchesSearch = query.isBlank() ||
                exercise.name.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        _libraryState.value = _libraryState.value.copy(
            exercises = filtered,
            searchQuery = query
        )
    }
}
