package com.prometheuscoach.mobile.ui.screens.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.Client
import com.prometheuscoach.mobile.data.repository.ClientLimitInfo
import com.prometheuscoach.mobile.data.repository.ClientRepository
import com.prometheuscoach.mobile.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientsState(
    val isLoading: Boolean = true,
    val clients: List<Client> = emptyList(),
    val error: String? = null,
    val isInviting: Boolean = false,
    val inviteError: String? = null,
    val inviteSuccess: Boolean = false,
    val coachInviteCode: String? = null,
    // Client limit info
    val clientLimitInfo: ClientLimitInfo? = null,
    val isAtClientLimit: Boolean = false
)

@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val clientRepository: ClientRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _clientsState = MutableStateFlow(ClientsState())
    val clientsState: StateFlow<ClientsState> = _clientsState.asStateFlow()

    init {
        loadClients()
        checkClientLimit()
    }

    fun loadClients() {
        viewModelScope.launch {
            _clientsState.update { it.copy(isLoading = true, error = null) }

            clientRepository.getClients()
                .onSuccess { clients ->
                    _clientsState.update {
                        it.copy(
                            isLoading = false,
                            clients = clients
                        )
                    }
                    // Also update client limit after loading clients
                    checkClientLimit()
                }
                .onFailure { exception ->
                    _clientsState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message
                        )
                    }
                }
        }
    }

    /**
     * Check if the coach has reached their client limit based on subscription plan.
     */
    fun checkClientLimit() {
        viewModelScope.launch {
            subscriptionRepository.checkClientLimit()
                .onSuccess { limitInfo ->
                    _clientsState.update {
                        it.copy(
                            clientLimitInfo = limitInfo,
                            isAtClientLimit = !limitInfo.canAddClient
                        )
                    }
                }
        }
    }

    fun inviteClientByEmail(email: String) {
        viewModelScope.launch {
            // First check if at client limit
            val limitInfo = _clientsState.value.clientLimitInfo
            if (limitInfo != null && !limitInfo.canAddClient) {
                _clientsState.update {
                    it.copy(
                        inviteError = "You've reached your client limit (${limitInfo.limit} clients). " +
                                "Upgrade your plan to add more clients."
                    )
                }
                return@launch
            }

            _clientsState.update { it.copy(isInviting = true, inviteError = null, inviteSuccess = false) }

            clientRepository.inviteClientByEmail(email)
                .onSuccess {
                    _clientsState.update {
                        it.copy(
                            isInviting = false,
                            inviteSuccess = true
                        )
                    }
                    // Reload clients and check limit again
                    loadClients()
                }
                .onFailure { exception ->
                    _clientsState.update {
                        it.copy(
                            isInviting = false,
                            inviteError = exception.message
                        )
                    }
                }
        }
    }

    fun loadCoachInviteCode() {
        viewModelScope.launch {
            clientRepository.getCoachInviteCode()
                .onSuccess { code ->
                    _clientsState.update { it.copy(coachInviteCode = code) }
                }
        }
    }

    fun clearInviteState() {
        _clientsState.update { it.copy(inviteError = null, inviteSuccess = false) }
    }
}
