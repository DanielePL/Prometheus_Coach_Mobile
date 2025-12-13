package com.prometheuscoach.mobile.ui.screens.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.Client
import com.prometheuscoach.mobile.data.repository.ClientRepository
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
    val coachInviteCode: String? = null
)

@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val clientRepository: ClientRepository
) : ViewModel() {

    private val _clientsState = MutableStateFlow(ClientsState())
    val clientsState: StateFlow<ClientsState> = _clientsState.asStateFlow()

    init {
        loadClients()
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

    fun inviteClientByEmail(email: String) {
        viewModelScope.launch {
            _clientsState.update { it.copy(isInviting = true, inviteError = null, inviteSuccess = false) }

            clientRepository.inviteClientByEmail(email)
                .onSuccess {
                    _clientsState.update {
                        it.copy(
                            isInviting = false,
                            inviteSuccess = true
                        )
                    }
                    // Reload clients to show any changes
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
