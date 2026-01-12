package com.prometheuscoach.mobile.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.repository.AuthRepository
import com.prometheuscoach.mobile.data.repository.ClientRepository
import com.prometheuscoach.mobile.data.repository.WorkoutRepository
import com.prometheuscoach.mobile.data.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoachAccountState(
    val isLoading: Boolean = true,
    val isUploadingAvatar: Boolean = false,
    val coachId: String = "",
    val coachName: String = "",
    val coachEmail: String = "",
    val avatarUrl: String? = null,
    val inviteCode: String = "",
    val clientCount: Int = 0,
    val programCount: Int = 0,
    val workoutCount: Int = 0,
    val error: String? = null,
    val uploadSuccess: Boolean = false
)

@HiltViewModel
class CoachAccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val clientRepository: ClientRepository,
    private val workoutRepository: WorkoutRepository,
    private val programRepository: ProgramRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CoachAccountState())
    val state: StateFlow<CoachAccountState> = _state.asStateFlow()

    init {
        loadCoachData()
    }

    fun loadCoachData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load profile
            authRepository.getCurrentUserProfile()
                .onSuccess { profile ->
                    _state.update { it.copy(
                        coachId = profile.id,
                        coachName = profile.fullName ?: "Coach",
                        coachEmail = profile.email ?: "",
                        avatarUrl = profile.avatarUrl
                    )}
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }

            // Load invite code
            clientRepository.getCoachInviteCode()
                .onSuccess { code ->
                    _state.update { it.copy(inviteCode = code) }
                }

            // Load client count
            clientRepository.getClientCount()
                .onSuccess { count ->
                    _state.update { it.copy(clientCount = count) }
                }

            // Load workout count
            workoutRepository.getWorkoutCount()
                .onSuccess { count ->
                    _state.update { it.copy(workoutCount = count) }
                }

            // Load program count
            programRepository.getProgramCount()
                .onSuccess { count ->
                    _state.update { it.copy(programCount = count) }
                }

            _state.update { it.copy(isLoading = false) }
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onLogoutComplete()
        }
    }

    fun uploadAvatar(imageBytes: ByteArray, contentType: String) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingAvatar = true, error = null, uploadSuccess = false) }

            authRepository.uploadProfileAvatar(imageBytes, contentType)
                .onSuccess { newAvatarUrl ->
                    _state.update { it.copy(
                        avatarUrl = newAvatarUrl,
                        isUploadingAvatar = false,
                        uploadSuccess = true
                    )}
                }
                .onFailure { e ->
                    _state.update { it.copy(
                        isUploadingAvatar = false,
                        error = e.message ?: "Failed to upload avatar"
                    )}
                }
        }
    }

    fun clearUploadSuccess() {
        _state.update { it.copy(uploadSuccess = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun updateName(newName: String) {
        if (newName.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            authRepository.updateProfileName(newName)
                .onSuccess {
                    _state.update { it.copy(
                        coachName = newName,
                        isLoading = false
                    )}
                }
                .onFailure { e ->
                    _state.update { it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to update name"
                    )}
                }
        }
    }
}
