package com.prometheuscoach.mobile.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.FeedbackType
import com.prometheuscoach.mobile.data.repository.BetaFeedbackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isSubmittingFeedback: Boolean = false,
    val feedbackSubmitSuccess: Boolean = false,
    val feedbackSubmitError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val betaFeedbackRepository: BetaFeedbackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun submitFeedback(
        feedbackType: FeedbackType,
        message: String,
        screenName: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSubmittingFeedback = true,
                feedbackSubmitError = null
            )

            betaFeedbackRepository.submitFeedback(
                feedbackType = feedbackType,
                message = message,
                screenName = screenName
            ).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingFeedback = false,
                        feedbackSubmitSuccess = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmittingFeedback = false,
                        feedbackSubmitError = error.message ?: "Failed to submit feedback"
                    )
                }
            )
        }
    }

    fun clearFeedbackSuccess() {
        _uiState.value = _uiState.value.copy(feedbackSubmitSuccess = false)
    }

    fun clearFeedbackError() {
        _uiState.value = _uiState.value.copy(feedbackSubmitError = null)
    }
}
