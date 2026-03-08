package id.stargan.intikasirfnb.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.transaction.SalesChannel
import id.stargan.intikasirfnb.domain.transaction.SalesChannelRepository
import id.stargan.intikasirfnb.domain.usecase.transaction.SaveSalesChannelUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SalesChannelUiState(
    val channels: List<SalesChannel> = emptyList(),
    val tenantId: TenantId? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class SalesChannelViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val salesChannelRepository: SalesChannelRepository,
    private val saveSalesChannelUseCase: SaveSalesChannelUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesChannelUiState())
    val uiState: StateFlow<SalesChannelUiState> = _uiState.asStateFlow()

    init {
        loadChannels()
    }

    fun loadChannels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val outlet = sessionManager.getCurrentOutlet()
                    ?: error("Outlet tidak ditemukan")
                val channels = salesChannelRepository.listByTenant(outlet.tenantId)
                _uiState.update { it.copy(channels = channels, tenantId = outlet.tenantId, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun saveChannel(channel: SalesChannel) {
        viewModelScope.launch {
            saveSalesChannelUseCase(channel)
                .onSuccess { loadChannels() }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    fun deleteChannel(channel: SalesChannel) {
        viewModelScope.launch {
            try {
                salesChannelRepository.delete(channel.id)
                loadChannels()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun toggleActive(channel: SalesChannel) {
        viewModelScope.launch {
            saveSalesChannelUseCase(channel.copy(isActive = !channel.isActive))
                .onSuccess { loadChannels() }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
