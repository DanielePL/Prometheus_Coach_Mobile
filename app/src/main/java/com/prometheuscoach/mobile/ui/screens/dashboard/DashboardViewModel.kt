package com.prometheuscoach.mobile.ui.screens.dashboard

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

data class DashboardState(
    val isLoading: Boolean = true,
    val clientCount: Int = 0,
    val recentClients: List<Client> = emptyList(),
    val todayAppointments: Int = 0,
    val pendingMessages: Int = 0,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val clientRepository: ClientRepository
) : ViewModel() {

    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _dashboardState.update { it.copy(isLoading = true, error = null) }

            // Load clients
            clientRepository.getClients()
                .onSuccess { clients ->
                    _dashboardState.update {
                        it.copy(
                            isLoading = false,
                            clientCount = clients.size,
                            recentClients = clients.take(5)
                        )
                    }
                }
                .onFailure { exception ->
                    _dashboardState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message
                        )
                    }
                }
        }
    }

    fun refresh() {
        loadDashboardData()
    }
}
