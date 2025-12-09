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

data class ClientDetailState(
    val isLoading: Boolean = true,
    val client: Client? = null,
    val error: String? = null
)

@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    private val clientRepository: ClientRepository
) : ViewModel() {

    private val _detailState = MutableStateFlow(ClientDetailState())
    val detailState: StateFlow<ClientDetailState> = _detailState.asStateFlow()

    fun loadClient(clientId: String) {
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
}
