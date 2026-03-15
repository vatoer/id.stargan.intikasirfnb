package id.stargan.intikasirfnb.ui.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.accounting.DailySummary
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.usecase.accounting.GetDailySummaryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SalesSummaryUiState(
    val dailySummaries: List<DailySummary> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class SalesSummaryViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val getDailySummaryUseCase: GetDailySummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesSummaryUiState())
    val uiState: StateFlow<SalesSummaryUiState> = _uiState.asStateFlow()

    init {
        loadSummary()
    }

    fun loadSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val outlet = sessionManager.getCurrentOutlet() ?: return@launch
                val summaries = getDailySummaryUseCase(outlet.id)
                _uiState.update { it.copy(dailySummaries = summaries, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
