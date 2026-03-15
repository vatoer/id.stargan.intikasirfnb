package id.stargan.intikasirfnb.ui.kitchen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.usecase.workflow.UpdateKitchenTicketStatusUseCase
import id.stargan.intikasirfnb.domain.workflow.KitchenStationType
import id.stargan.intikasirfnb.domain.workflow.KitchenTicket
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketId
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketRepository
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KitchenDisplayUiState(
    val allTickets: List<KitchenTicket> = emptyList(),
    val selectedStation: KitchenStationType? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val filteredTickets: List<KitchenTicket>
        get() = if (selectedStation == null) allTickets
        else allTickets.filter { it.station == selectedStation }

    val pendingTickets: List<KitchenTicket>
        get() = filteredTickets.filter { it.status == KitchenTicketStatus.PENDING }

    val preparingTickets: List<KitchenTicket>
        get() = filteredTickets.filter { it.status == KitchenTicketStatus.PREPARING }

    val readyTickets: List<KitchenTicket>
        get() = filteredTickets.filter { it.status == KitchenTicketStatus.READY }

    val activeCount: Int get() = filteredTickets.size
}

@HiltViewModel
class KitchenDisplayViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val kitchenTicketRepository: KitchenTicketRepository,
    private val updateStatusUseCase: UpdateKitchenTicketStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(KitchenDisplayUiState())
    val uiState: StateFlow<KitchenDisplayUiState> = _uiState.asStateFlow()

    init {
        observeTickets()
    }

    private fun observeTickets() {
        viewModelScope.launch {
            try {
                val outlet = sessionManager.getCurrentOutlet() ?: return@launch
                _uiState.update { it.copy(isLoading = false) }
                kitchenTicketRepository.streamActiveByOutlet(outlet.id).collect { tickets ->
                    _uiState.update { it.copy(allTickets = tickets.sortedBy { t -> t.createdAtMillis }) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun selectStation(station: KitchenStationType?) {
        _uiState.update { it.copy(selectedStation = station) }
    }

    fun startPreparing(ticketId: KitchenTicketId) {
        viewModelScope.launch {
            val result = updateStatusUseCase(ticketId, KitchenTicketStatus.PREPARING)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun markReady(ticketId: KitchenTicketId) {
        viewModelScope.launch {
            val result = updateStatusUseCase(ticketId, KitchenTicketStatus.READY)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun markServed(ticketId: KitchenTicketId) {
        viewModelScope.launch {
            val result = updateStatusUseCase(ticketId, KitchenTicketStatus.SERVED)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
