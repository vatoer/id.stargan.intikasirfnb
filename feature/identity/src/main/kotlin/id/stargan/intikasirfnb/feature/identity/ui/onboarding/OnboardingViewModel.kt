package id.stargan.intikasirfnb.feature.identity.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.identity.User
import id.stargan.intikasirfnb.domain.usecase.identity.CompleteOnboardingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OnboardingUiState {
    data object Idle : OnboardingUiState
    data object Loading : OnboardingUiState
    data class Success(val user: User) : OnboardingUiState
    data class Error(val message: String) : OnboardingUiState
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _businessName = MutableStateFlow("")
    val businessName: StateFlow<String> = _businessName.asStateFlow()

    private val _outletName = MutableStateFlow("")
    val outletName: StateFlow<String> = _outletName.asStateFlow()

    private val _outletAddress = MutableStateFlow("")
    val outletAddress: StateFlow<String> = _outletAddress.asStateFlow()

    private val _ownerName = MutableStateFlow("")
    val ownerName: StateFlow<String> = _ownerName.asStateFlow()

    private val _ownerEmail = MutableStateFlow("")
    val ownerEmail: StateFlow<String> = _ownerEmail.asStateFlow()

    private val _pin = MutableStateFlow("")
    val pin: StateFlow<String> = _pin.asStateFlow()

    private val _confirmPin = MutableStateFlow("")
    val confirmPin: StateFlow<String> = _confirmPin.asStateFlow()

    private val _isEnteringConfirmPin = MutableStateFlow(false)
    val isEnteringConfirmPin: StateFlow<Boolean> = _isEnteringConfirmPin.asStateFlow()

    fun onBusinessNameChanged(value: String) { _businessName.value = value }
    fun onOutletNameChanged(value: String) { _outletName.value = value }
    fun onOutletAddressChanged(value: String) { _outletAddress.value = value }
    fun onOwnerNameChanged(value: String) { _ownerName.value = value }
    fun onOwnerEmailChanged(value: String) { _ownerEmail.value = value }

    fun onPinChanged(newPin: String) {
        if (newPin.length <= 6 && newPin.all { it.isDigit() }) {
            if (_isEnteringConfirmPin.value) {
                _confirmPin.value = newPin
            } else {
                _pin.value = newPin
            }
        }
    }

    fun onPinDelete() {
        if (_isEnteringConfirmPin.value) {
            if (_confirmPin.value.isNotEmpty()) {
                _confirmPin.value = _confirmPin.value.dropLast(1)
            }
        } else {
            if (_pin.value.isNotEmpty()) {
                _pin.value = _pin.value.dropLast(1)
            }
        }
    }

    fun switchToConfirmPin() {
        _isEnteringConfirmPin.value = true
    }

    fun switchToPin() {
        _isEnteringConfirmPin.value = false
        _confirmPin.value = ""
    }

    fun isStep1Valid(): Boolean = _businessName.value.isNotBlank()

    fun isStep2Valid(): Boolean = _outletName.value.isNotBlank()

    fun isStep3Valid(): Boolean =
        _ownerName.value.isNotBlank() &&
        _ownerEmail.value.isNotBlank() &&
        _pin.value.length >= 4 &&
        _pin.value == _confirmPin.value

    fun onNextStep() {
        if (_currentStep.value < 3) {
            _currentStep.value++
        }
    }

    fun onPreviousStep() {
        if (_currentStep.value > 1) {
            _currentStep.value--
        }
    }

    fun onComplete() {
        if (!isStep3Valid()) {
            _uiState.value = OnboardingUiState.Error(
                when {
                    _ownerName.value.isBlank() -> "Nama tidak boleh kosong"
                    _ownerEmail.value.isBlank() -> "Email tidak boleh kosong"
                    _pin.value.length < 4 -> "PIN minimal 4 digit"
                    _pin.value != _confirmPin.value -> "PIN tidak cocok"
                    else -> "Data tidak valid"
                }
            )
            return
        }

        _uiState.value = OnboardingUiState.Loading
        viewModelScope.launch {
            completeOnboardingUseCase(
                businessName = _businessName.value.trim(),
                outletName = _outletName.value.trim(),
                outletAddress = _outletAddress.value.trim(),
                ownerName = _ownerName.value.trim(),
                ownerEmail = _ownerEmail.value.trim(),
                pin = _pin.value
            ).onSuccess { user ->
                _uiState.value = OnboardingUiState.Success(user)
            }.onFailure { error ->
                _uiState.value = OnboardingUiState.Error(
                    error.message ?: "Gagal menyelesaikan setup"
                )
            }
        }
    }

    fun clearError() {
        if (_uiState.value is OnboardingUiState.Error) {
            _uiState.value = OnboardingUiState.Idle
        }
    }
}
