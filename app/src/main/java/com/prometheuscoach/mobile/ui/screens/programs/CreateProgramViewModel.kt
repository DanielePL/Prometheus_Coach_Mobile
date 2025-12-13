package com.prometheuscoach.mobile.ui.screens.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateProgramState(
    val isCreating: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CreateProgramViewModel @Inject constructor(
    private val programRepository: ProgramRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreateProgramState())
    val state: StateFlow<CreateProgramState> = _state.asStateFlow()

    suspend fun createProgram(
        name: String,
        description: String?,
        durationWeeks: Int,
        workoutsPerWeek: Int,
        difficulty: String
    ): Result<String> {
        _state.value = _state.value.copy(isCreating = true, error = null)

        return programRepository.createProgram(
            name = name,
            description = description,
            durationWeeks = durationWeeks,
            workoutsPerWeek = workoutsPerWeek,
            difficulty = difficulty
        ).map { program ->
            _state.value = _state.value.copy(isCreating = false)
            program.id
        }.onFailure { error ->
            _state.value = _state.value.copy(
                isCreating = false,
                error = error.message ?: "Failed to create program"
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
