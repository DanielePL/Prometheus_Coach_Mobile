package com.prometheuscoach.mobile.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null,
    val isSignUpMode: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.isAuthenticated.collect { isAuthenticated ->
                _authState.update { it.copy(isAuthenticated = isAuthenticated) }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            authRepository.signIn(email, password)
                .onSuccess {
                    _authState.update { it.copy(isLoading = false, isAuthenticated = true) }
                }
                .onFailure { exception ->
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Login failed"
                        )
                    }
                }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            authRepository.signUp(email, password)
                .onSuccess {
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Sign up failed"
                        )
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.update { AuthState() }
        }
    }

    fun toggleSignUpMode() {
        _authState.update { it.copy(isSignUpMode = !it.isSignUpMode, error = null) }
    }

    fun clearError() {
        _authState.update { it.copy(error = null) }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            authRepository.resetPassword(email)
                .onSuccess {
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            error = "Password reset email sent"
                        )
                    }
                }
                .onFailure { exception ->
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Password reset failed"
                        )
                    }
                }
        }
    }
}
