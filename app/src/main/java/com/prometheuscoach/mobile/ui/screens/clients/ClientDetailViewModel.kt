package com.prometheuscoach.mobile.ui.screens.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.AssignedWorkout
import com.prometheuscoach.mobile.data.model.Client
import com.prometheuscoach.mobile.data.model.RoutineSummary
import com.prometheuscoach.mobile.data.repository.ChatRepository
import com.prometheuscoach.mobile.data.repository.ClientRepository
import com.prometheuscoach.mobile.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientDetailState(
    val isLoading: Boolean = true,
    val client: Client? = null,
    val error: String? = null,
    // Workout assignment state
    val availableWorkouts: List<RoutineSummary> = emptyList(),
    val assignedWorkouts: List<AssignedWorkout> = emptyList(),
    val isLoadingWorkouts: Boolean = false,
    val isAssigning: Boolean = false,
    val workoutsError: String? = null
)

@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    private val clientRepository: ClientRepository,
    private val chatRepository: ChatRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _detailState = MutableStateFlow(ClientDetailState())
    val detailState: StateFlow<ClientDetailState> = _detailState.asStateFlow()

    private var currentClientId: String? = null

    fun loadClient(clientId: String) {
        currentClientId = clientId
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }

            clientRepository.getClientById(clientId)
                .onSuccess { client ->
                    _detailState.update {
                        it.copy(
                            isLoading = false,
                            client = client
                        )
                    }
                    // Also load assigned workouts
                    loadAssignedWorkouts(clientId)
                }
                .onFailure { exception ->
                    _detailState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message
                        )
                    }
                }
        }
    }

    /**
     * Load workouts assigned to this client.
     */
    private fun loadAssignedWorkouts(clientId: String) {
        viewModelScope.launch {
            workoutRepository.getClientAssignments(clientId)
                .onSuccess { assignments ->
                    _detailState.update {
                        it.copy(assignedWorkouts = assignments)
                    }
                }
        }
    }

    /**
     * Load available workouts for assignment.
     */
    fun loadAvailableWorkouts() {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoadingWorkouts = true, workoutsError = null) }

            workoutRepository.getRoutines()
                .onSuccess { workouts ->
                    _detailState.update {
                        it.copy(
                            isLoadingWorkouts = false,
                            availableWorkouts = workouts
                        )
                    }
                }
                .onFailure { exception ->
                    _detailState.update {
                        it.copy(
                            isLoadingWorkouts = false,
                            workoutsError = exception.message
                        )
                    }
                }
        }
    }

    /**
     * Assign a workout to the current client.
     */
    suspend fun assignWorkout(
        workoutId: String,
        notes: String? = null
    ): Result<Unit> {
        val clientId = currentClientId ?: return Result.failure(Exception("No client selected"))

        _detailState.update { it.copy(isAssigning = true) }

        return workoutRepository.assignWorkoutToClient(
            routineId = workoutId,
            clientId = clientId,
            notes = notes
        ).map {
            // Reload assigned workouts
            loadAssignedWorkouts(clientId)
            _detailState.update { it.copy(isAssigning = false) }
        }.onFailure {
            _detailState.update { state -> state.copy(isAssigning = false) }
        }
    }

    /**
     * Remove a workout assignment.
     */
    suspend fun removeAssignment(assignmentId: String): Result<Unit> {
        val clientId = currentClientId ?: return Result.failure(Exception("No client selected"))

        return workoutRepository.removeAssignment(assignmentId).also {
            if (it.isSuccess) {
                loadAssignedWorkouts(clientId)
            }
        }
    }

    /**
     * Start or find existing conversation with client.
     * Uses find_or_create_conversation RPC.
     */
    suspend fun startConversation(clientId: String): Result<String> {
        return chatRepository.findOrCreateConversation(clientId)
    }
}
