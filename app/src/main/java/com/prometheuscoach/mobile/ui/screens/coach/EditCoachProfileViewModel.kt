package com.prometheuscoach.mobile.ui.screens.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.UpdateCoachProfileRequest
import com.prometheuscoach.mobile.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditCoachProfileState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val bio: String = "",
    val specialization: String = "",
    val yearsExperience: String = "",
    val instagramHandle: String = "",
    val tiktokHandle: String = "",
    val youtubeHandle: String = "",
    val twitterHandle: String = "",
    val websiteUrl: String = "",
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class EditCoachProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditCoachProfileState())
    val state: StateFlow<EditCoachProfileState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            authRepository.getCurrentCoachSetCard()
                .onSuccess { profile ->
                    _state.update { it.copy(
                        isLoading = false,
                        bio = profile.bio ?: "",
                        specialization = profile.specialization ?: "",
                        yearsExperience = profile.yearsExperience?.toString() ?: "",
                        instagramHandle = profile.instagramHandle ?: "",
                        tiktokHandle = profile.tiktokHandle ?: "",
                        youtubeHandle = profile.youtubeHandle ?: "",
                        twitterHandle = profile.twitterHandle ?: "",
                        websiteUrl = profile.websiteUrl ?: ""
                    )}
                }
                .onFailure { e ->
                    _state.update { it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load profile"
                    )}
                }
        }
    }

    fun updateBio(value: String) {
        _state.update { it.copy(bio = value) }
    }

    fun updateSpecialization(value: String) {
        _state.update { it.copy(specialization = value) }
    }

    fun updateYearsExperience(value: String) {
        // Only allow numbers
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _state.update { it.copy(yearsExperience = value) }
        }
    }

    fun updateInstagram(value: String) {
        _state.update { it.copy(instagramHandle = value) }
    }

    fun updateTikTok(value: String) {
        _state.update { it.copy(tiktokHandle = value) }
    }

    fun updateYouTube(value: String) {
        _state.update { it.copy(youtubeHandle = value) }
    }

    fun updateTwitter(value: String) {
        _state.update { it.copy(twitterHandle = value) }
    }

    fun updateWebsite(value: String) {
        _state.update { it.copy(websiteUrl = value) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            val currentState = _state.value
            _state.update { it.copy(isSaving = true, error = null) }

            val request = UpdateCoachProfileRequest(
                bio = currentState.bio.ifBlank { null },
                specialization = currentState.specialization.ifBlank { null },
                instagramHandle = currentState.instagramHandle.ifBlank { null },
                tiktokHandle = currentState.tiktokHandle.ifBlank { null },
                youtubeHandle = currentState.youtubeHandle.ifBlank { null },
                twitterHandle = currentState.twitterHandle.ifBlank { null },
                websiteUrl = currentState.websiteUrl.ifBlank { null },
                yearsExperience = currentState.yearsExperience.toIntOrNull()
            )

            authRepository.updateCoachProfile(request)
                .onSuccess {
                    _state.update { it.copy(
                        isSaving = false,
                        saveSuccess = true
                    )}
                }
                .onFailure { e ->
                    _state.update { it.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to save profile"
                    )}
                }
        }
    }

    fun clearSaveSuccess() {
        _state.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}