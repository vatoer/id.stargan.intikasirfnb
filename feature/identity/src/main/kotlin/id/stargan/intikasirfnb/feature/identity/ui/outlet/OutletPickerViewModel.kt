package id.stargan.intikasirfnb.feature.identity.ui.outlet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.identity.Outlet
import id.stargan.intikasirfnb.domain.identity.OutletRepository
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.usecase.identity.SelectOutletUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OutletPickerUiState {
    data object Loading : OutletPickerUiState
    data class Ready(val outlets: List<Outlet>) : OutletPickerUiState
    data class Error(val message: String) : OutletPickerUiState
}

@HiltViewModel
class OutletPickerViewModel @Inject constructor(
    private val outletRepository: OutletRepository,
    private val selectOutletUseCase: SelectOutletUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<OutletPickerUiState>(OutletPickerUiState.Loading)
    val uiState: StateFlow<OutletPickerUiState> = _uiState.asStateFlow()

    private val _outletSelected = MutableStateFlow(false)
    val outletSelected: StateFlow<Boolean> = _outletSelected.asStateFlow()

    init {
        loadOutlets()
    }

    private fun loadOutlets() {
        viewModelScope.launch {
            val user = sessionManager.getCurrentUser()
            if (user == null) {
                _uiState.value = OutletPickerUiState.Error("Sesi tidak valid")
                return@launch
            }

            val allOutlets = outletRepository.listByTenant(user.tenantId)
            val accessibleOutlets = allOutlets.filter { outlet ->
                outlet.isActive && user.hasAccessToOutlet(outlet.id)
            }

            if (accessibleOutlets.isEmpty()) {
                _uiState.value = OutletPickerUiState.Error("Tidak ada outlet yang tersedia")
            } else {
                _uiState.value = OutletPickerUiState.Ready(accessibleOutlets)
            }
        }
    }

    fun onOutletSelected(outlet: Outlet) {
        viewModelScope.launch {
            selectOutletUseCase(outlet.id)
                .onSuccess { _outletSelected.value = true }
                .onFailure { error ->
                    _uiState.value = OutletPickerUiState.Error(error.message ?: "Gagal memilih outlet")
                }
        }
    }
}
