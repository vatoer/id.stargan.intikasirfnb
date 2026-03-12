package id.stargan.intikasirfnb.ui.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.licensing.ActivationRepository
import id.stargan.intikasirfnb.licensing.SignedLicenseDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ActivationUiState {
    data object Idle : ActivationUiState
    data object Loading : ActivationUiState
    data class Success(val license: SignedLicenseDto) : ActivationUiState
    data class Error(val message: String) : ActivationUiState
}

@HiltViewModel
class ActivationViewModel @Inject constructor(
    private val activationRepo: ActivationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ActivationUiState>(ActivationUiState.Idle)
    val uiState: StateFlow<ActivationUiState> = _uiState.asStateFlow()

    fun activate(sn: String) {
        if (_uiState.value is ActivationUiState.Loading) return
        _uiState.value = ActivationUiState.Loading

        viewModelScope.launch {
            activationRepo.activate(sn)
                .onSuccess { license ->
                    _uiState.value = ActivationUiState.Success(license)
                }
                .onFailure { e ->
                    // Jika "already bound", coba reactivate
                    val msg = e.message ?: ""
                    if (msg.contains("already bound", ignoreCase = true) ||
                        msg.contains("sudah terikat", ignoreCase = true)
                    ) {
                        reactivate(sn)
                    } else {
                        _uiState.value = ActivationUiState.Error(
                            e.message ?: "Aktivasi gagal"
                        )
                    }
                }
        }
    }

    private fun reactivate(sn: String) {
        viewModelScope.launch {
            activationRepo.reactivate(sn)
                .onSuccess { license ->
                    _uiState.value = ActivationUiState.Success(license)
                }
                .onFailure { e ->
                    _uiState.value = ActivationUiState.Error(
                        e.message ?: "Re-aktivasi gagal"
                    )
                }
        }
    }

    fun resetState() {
        _uiState.value = ActivationUiState.Idle
    }
}
