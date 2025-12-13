package com.prometheuscoach.mobile.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.ExerciseListItem
import com.prometheuscoach.mobile.data.model.RoutineSummary
import com.prometheuscoach.mobile.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryState(
    // Exercises
    val exercises: List<ExerciseListItem> = emptyList(),
    val allExercises: List<ExerciseListItem> = emptyList(),
    val myExercises: List<ExerciseListItem> = emptyList(),
    val exerciseCategories: List<String> = emptyList(),

    // Workouts
    val workouts: List<RoutineSummary> = emptyList(),
    val workoutTemplates: List<RoutineSummary> = emptyList(),

    // Programs (TODO: implement)
    val programs: List<Any> = emptyList(),
    val programTemplates: List<Any> = emptyList(),

    // Search
    val searchQuery: String = "",

    // Loading states
    val isLoading: Boolean = false,
    val error: String? = null,

    // Create workout
    val isCreating: Boolean = false,
    val createError: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        loadExercises()
    }

    // ==================== EXERCISES ====================

    fun loadExercises() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            workoutRepository.searchExercises("", limit = 500)
                .onSuccess { exercises ->
                    val categories = exercises
                        .mapNotNull { it.category ?: it.mainMuscleGroup }
                        .distinct()
                        .sorted()

                    _state.value = _state.value.copy(
                        exercises = exercises,
                        allExercises = exercises,
                        exerciseCategories = categories,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load exercises"
                    )
                }
        }
    }

    fun loadMyExercises() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // TODO: Filter by owner_id when we have user context
            workoutRepository.searchExercises("", limit = 100)
                .onSuccess { exercises ->
                    // For now, show empty - in the future filter by owner_id
                    _state.value = _state.value.copy(
                        myExercises = emptyList(), // Will be filtered by owner_id
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load exercises"
                    )
                }
        }
    }

    // ==================== WORKOUTS ====================

    fun loadWorkouts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            workoutRepository.getRoutines()
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

    suspend fun createWorkout(name: String, description: String?): Result<String> {
        _state.value = _state.value.copy(isCreating = true, createError = null)

        return workoutRepository.createRoutine(name, description)
            .map { routine ->
                _state.value = _state.value.copy(isCreating = false)
                loadWorkouts() // Refresh list
                routine.id
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isCreating = false,
                    createError = error.message ?: "Failed to create workout"
                )
            }
    }

    fun clearCreateError() {
        _state.value = _state.value.copy(createError = null)
    }

    // ==================== PROGRAMS ====================

    fun loadPrograms() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // TODO: Implement when program tables are available
            _state.value = _state.value.copy(
                programs = emptyList(),
                programTemplates = emptyList(),
                isLoading = false
            )
        }
    }

    // ==================== SEARCH & FILTERS ====================

    fun setSearchQuery(query: String) {
        val allExercises = _state.value.allExercises

        val filtered = if (query.isBlank()) {
            allExercises
        } else {
            allExercises.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }

        _state.value = _state.value.copy(
            exercises = filtered,
            searchQuery = query
        )
    }

    fun setCategoryFilter(category: String?) {
        val allExercises = _state.value.allExercises
        val searchQuery = _state.value.searchQuery

        val filtered = allExercises.filter { exercise ->
            val matchesCategory = category == null ||
                (exercise.category ?: exercise.mainMuscleGroup) == category
            val matchesSearch = searchQuery.isBlank() ||
                exercise.name.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        _state.value = _state.value.copy(exercises = filtered)
    }
}
