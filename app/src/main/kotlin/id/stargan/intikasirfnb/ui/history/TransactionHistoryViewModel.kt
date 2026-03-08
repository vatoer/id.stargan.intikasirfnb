package id.stargan.intikasirfnb.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleStatus
import id.stargan.intikasirfnb.domain.usecase.transaction.GetSalesByOutletUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionHistoryUiState(
    val allSales: List<Sale> = emptyList(),
    val filteredSales: List<Sale> = emptyList(),
    val selectedStatus: SaleStatus? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val getSalesByOutletUseCase: GetSalesByOutletUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionHistoryUiState())
    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    init {
        loadSales()
    }

    fun loadSales() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val outlet = sessionManager.getCurrentOutlet()
                    ?: error("Outlet tidak ditemukan")
                val sales = getSalesByOutletUseCase(outlet.id, limit = 200)
                _uiState.update { state ->
                    state.copy(
                        allSales = sales,
                        filteredSales = applyFilters(sales, state.selectedStatus, state.searchQuery),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun filterByStatus(status: SaleStatus?) {
        _uiState.update { state ->
            val newStatus = if (state.selectedStatus == status) null else status
            state.copy(
                selectedStatus = newStatus,
                filteredSales = applyFilters(state.allSales, newStatus, state.searchQuery)
            )
        }
    }

    fun search(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredSales = applyFilters(state.allSales, state.selectedStatus, query)
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun applyFilters(
        sales: List<Sale>,
        status: SaleStatus?,
        query: String
    ): List<Sale> {
        var result = sales
        if (status != null) {
            result = result.filter { it.status == status }
        }
        if (query.isNotBlank()) {
            val q = query.lowercase()
            result = result.filter { sale ->
                (sale.receiptNumber?.lowercase()?.contains(q) == true) ||
                    (sale.externalOrderId?.lowercase()?.contains(q) == true) ||
                    sale.lines.any { it.productRef.name.lowercase().contains(q) }
            }
        }
        return result
    }
}
