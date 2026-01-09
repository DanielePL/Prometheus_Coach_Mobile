package com.prometheuscoach.mobile.ui.screens.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.AssignedWorkout
import com.prometheuscoach.mobile.data.model.Client
import com.prometheuscoach.mobile.data.model.UpdateClientRequest
import com.prometheuscoach.mobile.data.model.WorkoutSummary
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
    val availableWorkouts: List<WorkoutSummary> = emptyList(),
    val assignedWorkouts: List<AssignedWorkout> = emptyList(),
    val isLoadingWorkouts: Boolean = false,
    val isAssigning: Boolean = false,
    val workoutsError: String? = null,
    // Client edit state
    val isSavingClient: Boolean = false,
    val clientSaveError: String? = null,
    // Assignment edit state
    val isUpdatingAssignment: Boolean = false,
    val assignmentUpdateError: String? = null
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

            workoutRepository.getWorkouts()
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
            workoutId = workoutId,
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

    /**
     * Update client profile data.
     * @param name New client name
     * @param timezone New timezone (optional)
     * @return Result indicating success or failure
     */
    fun updateClient(name: String, timezone: String?): Result<Unit> {
        val clientId = currentClientId ?: return Result.failure(Exception("No client selected"))

        viewModelScope.launch {
            _detailState.update { it.copy(isSavingClient = true, clientSaveError = null) }

            val request = UpdateClientRequest(
                fullName = name,
                preferredTimezone = timezone
            )

            clientRepository.updateClient(clientId, request)
                .onSuccess {
                    // Reload client data to reflect changes
                    loadClient(clientId)
                    _detailState.update { it.copy(isSavingClient = false) }
                }
                .onFailure { exception ->
                    _detailState.update {
                        it.copy(
                            isSavingClient = false,
                            clientSaveError = exception.message ?: "Failed to update client"
                        )
                    }
                }
        }

        return Result.success(Unit)
    }

    /**
     * Clear client save error.
     */
    fun clearClientSaveError() {
        _detailState.update { it.copy(clientSaveError = null) }
    }

    /**
     * Update an assignment (notes, scheduled date, status) and exercise sets.
     *
     * @param assignmentId The assignment ID
     * @param notes Optional notes for the client
     * @param scheduledDate Optional scheduled date (YYYY-MM-DD)
     * @param status Assignment status (active, completed, cancelled)
     * @param exerciseSets Map of workoutExerciseId to list of EditableSetInfo
     */
    suspend fun updateAssignmentWithSets(
        assignmentId: String,
        notes: String?,
        scheduledDate: String?,
        status: String?,
        exerciseSets: Map<String, List<EditableSetInfo>>
    ): Result<Unit> {
        val clientId = currentClientId ?: return Result.failure(Exception("No client selected"))

        _detailState.update { it.copy(isUpdatingAssignment = true, assignmentUpdateError = null) }

        try {
            // First update the assignment metadata
            workoutRepository.updateAssignment(
                assignmentId = assignmentId,
                notes = notes,
                scheduledDate = scheduledDate,
                status = status
            ).getOrThrow()

            // Then update each exercise's sets
            for ((workoutExerciseId, sets) in exerciseSets) {
                val setUpdates = sets.map { set ->
                    WorkoutRepository.ExerciseSetUpdate(
                        id = set.id,
                        setNumber = set.setNumber,
                        targetReps = set.targetReps,
                        targetWeight = set.targetWeight,
                        restSeconds = set.restSeconds
                    )
                }
                workoutRepository.updateExerciseSets(workoutExerciseId, setUpdates).getOrThrow()
            }

            // Reload to reflect changes
            loadAssignedWorkouts(clientId)
            _detailState.update { it.copy(isUpdatingAssignment = false) }
            return Result.success(Unit)
        } catch (e: Exception) {
            _detailState.update {
                it.copy(
                    isUpdatingAssignment = false,
                    assignmentUpdateError = e.message ?: "Failed to update assignment"
                )
            }
            return Result.failure(e)
        }
    }

    /**
     * Update an assignment (notes, scheduled date, status).
     * @deprecated Use updateAssignmentWithSets for full editing support
     */
    suspend fun updateAssignment(
        assignmentId: String,
        notes: String?,
        scheduledDate: String?,
        status: String?
    ): Result<Unit> {
        val clientId = currentClientId ?: return Result.failure(Exception("No client selected"))

        _detailState.update { it.copy(isUpdatingAssignment = true, assignmentUpdateError = null) }

        return workoutRepository.updateAssignment(
            assignmentId = assignmentId,
            notes = notes,
            scheduledDate = scheduledDate,
            status = status
        ).also { result ->
            result.onSuccess {
                loadAssignedWorkouts(clientId)
                _detailState.update { it.copy(isUpdatingAssignment = false) }
            }.onFailure { exception ->
                _detailState.update {
                    it.copy(
                        isUpdatingAssignment = false,
                        assignmentUpdateError = exception.message ?: "Failed to update assignment"
                    )
                }
            }
        }
    }

    /**
     * Clear assignment update error.
     */
    fun clearAssignmentUpdateError() {
        _detailState.update { it.copy(assignmentUpdateError = null) }
    }
}
