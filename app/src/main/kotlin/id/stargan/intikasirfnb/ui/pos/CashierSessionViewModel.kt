package id.stargan.intikasirfnb.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.CashierSession
import id.stargan.intikasirfnb.domain.usecase.transaction.CloseCashierSessionUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetCurrentCashierSessionUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.OpenCashierSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class CashierSessionUiState(
    val currentSession: CashierSession? = null,
    val isLoading: Boolean = true,
    val showOpenDialog: Boolean = false,
    val showCloseDialog: Boolean = false,
    val openingFloatInput: String = "0",
    val closingCashInput: String = "0",
    val closeNotes: String = "",
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val sessionOpened: Boolean = false,
    val sessionClosed: Boolean = false
)

@HiltViewModel
class CashierSessionViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val openCashierSessionUseCase: OpenCashierSessionUseCase,
    private val closeCashierSessionUseCase: CloseCashierSessionUseCase,
    private val getCurrentCashierSessionUseCase: GetCurrentCashierSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashierSessionUiState())
    val uiState: StateFlow<CashierSessionUiState> = _uiState.asStateFlow()

    init {
        loadCurrentSession()
    }

    fun loadCurrentSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val outlet = sessionManager.getCurrentOutlet()
                    ?: error("Outlet tidak ditemukan")
                val terminalId = TerminalId(outlet.id.value)
                val session = getCurrentCashierSessionUseCase(outlet.id, terminalId)
                _uiState.update { it.copy(currentSession = session, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun showOpenDialog() {
        _uiState.update { it.copy(showOpenDialog = true, openingFloatInput = "0") }
    }

    fun dismissOpenDialog() {
        _uiState.update { it.copy(showOpenDialog = false) }
    }

    fun updateOpeningFloat(value: String) {
        val cleaned = value.filter { it.isDigit() }
        _uiState.update { it.copy(openingFloatInput = cleaned.ifEmpty { "0" }) }
    }

    fun openSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            try {
                val outlet = sessionManager.getCurrentOutlet()
                    ?: error("Outlet tidak ditemukan")
                val user = sessionManager.getCurrentUser()
                    ?: error("User tidak ditemukan")
                val terminalId = TerminalId(outlet.id.value)
                val openingFloat = Money(
                    BigDecimal(_uiState.value.openingFloatInput),
                    "IDR"
                )

                val result = openCashierSessionUseCase(
                    terminalId = terminalId,
                    outletId = outlet.id,
                    userId = user.id,
                    openingFloat = openingFloat
                )

                val session = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        currentSession = session,
                        showOpenDialog = false,
                        isProcessing = false,
                        sessionOpened = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = e.message) }
            }
        }
    }

    fun showCloseDialog() {
        _uiState.update { it.copy(showCloseDialog = true, closingCashInput = "0", closeNotes = "") }
    }

    fun dismissCloseDialog() {
        _uiState.update { it.copy(showCloseDialog = false) }
    }

    fun updateClosingCash(value: String) {
        val cleaned = value.filter { it.isDigit() }
        _uiState.update { it.copy(closingCashInput = cleaned.ifEmpty { "0" }) }
    }

    fun updateCloseNotes(value: String) {
        _uiState.update { it.copy(closeNotes = value) }
    }

    fun closeSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            try {
                val outlet = sessionManager.getCurrentOutlet()
                    ?: error("Outlet tidak ditemukan")
                val terminalId = TerminalId(outlet.id.value)
                val closingCash = Money(
                    BigDecimal(_uiState.value.closingCashInput),
                    "IDR"
                )

                val session = _uiState.value.currentSession
                    ?: error("Tidak ada sesi yang aktif")
                val expectedCash = session.openingFloat

                val result = closeCashierSessionUseCase(
                    outletId = outlet.id,
                    terminalId = terminalId,
                    closingCash = closingCash,
                    expectedCash = expectedCash,
                    notes = _uiState.value.closeNotes.ifBlank { null }
                )

                val closed = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        currentSession = null,
                        showCloseDialog = false,
                        isProcessing = false,
                        sessionClosed = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSessionOpened() {
        _uiState.update { it.copy(sessionOpened = false) }
    }

    fun resetSessionClosed() {
        _uiState.update { it.copy(sessionClosed = false) }
    }
}
