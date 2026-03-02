package id.stargan.intikasirfnb.feature.identity.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.User
import id.stargan.intikasirfnb.domain.usecase.identity.LoginWithPinUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val user: User) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginWithPinUseCase: LoginWithPinUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _pin = MutableStateFlow("")
    val pin: StateFlow<String> = _pin.asStateFlow()

    // Default tenant for single-tenant setup
    private val defaultTenantId = TenantId("default")

    fun onPinChanged(newPin: String) {
        if (newPin.length <= 6 && newPin.all { it.isDigit() }) {
            _pin.value = newPin
            if (_uiState.value is LoginUiState.Error) {
                _uiState.value = LoginUiState.Idle
            }
        }
    }

    fun onLoginClicked() {
        val currentPin = _pin.value
        if (currentPin.length < 4) {
            _uiState.value = LoginUiState.Error("PIN minimal 4 digit")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            loginWithPinUseCase(defaultTenantId, currentPin)
                .onSuccess { user ->
                    _uiState.value = LoginUiState.Success(user)
                }
                .onFailure { error ->
                    _uiState.value = LoginUiState.Error(error.message ?: "Login gagal")
                    _pin.value = ""
                }
        }
    }
}
