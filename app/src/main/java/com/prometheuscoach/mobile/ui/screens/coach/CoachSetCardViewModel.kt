package com.prometheuscoach.mobile.ui.screens.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.CoachSetCard
import com.prometheuscoach.mobile.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoachSetCardState(
    val isLoading: Boolean = true,
    val profile: CoachSetCard? = null,
    val error: String? = null
)

@HiltViewModel
class CoachSetCardViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CoachSetCardState())
    val state: StateFlow<CoachSetCardState> = _state.asStateFlow()

    fun loadCoachProfile(coachId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            authRepository.getCoachSetCard(coachId)
                .onSuccess { profile ->
                    _state.update { it.copy(
                        profile = profile,
                        isLoading = false
                    )}
                }
                .onFailure { e ->
                    _state.update { it.copy(
                        error = e.message ?: "Failed to load profile",
                        isLoading = false
                    )}
                }
        }
    }
}