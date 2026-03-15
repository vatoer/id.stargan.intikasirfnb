package id.stargan.intikasirfnb.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.MenuItemRepository
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.inventory.StockLevel
import id.stargan.intikasirfnb.domain.inventory.StockMovementType
import id.stargan.intikasirfnb.domain.usecase.inventory.AdjustStockUseCase
import id.stargan.intikasirfnb.domain.usecase.inventory.GetStockLevelsUseCase
import id.stargan.intikasirfnb.domain.usecase.inventory.ReceiveStockUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class StockUiState(
    val stockLevels: List<StockLevel> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val filteredStock: List<StockLevel>
        get() = if (searchQuery.isBlank()) stockLevels
        else stockLevels.filter { it.productName.contains(searchQuery, ignoreCase = true) }

    val lowStockCount: Int get() = stockLevels.count { it.isLowStock }

    /** Menu items that don't have a stock level yet */
    val unregisteredItems: List<MenuItem>
        get() {
            val registeredIds = stockLevels.map { it.productId }.toSet()
            return menuItems.filter { it.id !in registeredIds && it.isActive }
        }
}

@HiltViewModel
class StockViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val getStockLevelsUseCase: GetStockLevelsUseCase,
    private val adjustStockUseCase: AdjustStockUseCase,
    private val receiveStockUseCase: ReceiveStockUseCase,
    private val menuItemRepository: MenuItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockUiState())
    val uiState: StateFlow<StockUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val outlet = sessionManager.getCurrentOutlet() ?: return@launch
                val stocks = getStockLevelsUseCase(outlet.id)
                val items = menuItemRepository.listByTenant(outlet.tenantId)
                _uiState.update { it.copy(stockLevels = stocks, menuItems = items, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun adjustStock(stock: StockLevel, newQuantity: BigDecimal, type: StockMovementType, notes: String?) {
        viewModelScope.launch {
            try {
                adjustStockUseCase(stock.productId, stock.outletId, newQuantity, type, notes)
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun receiveStock(stock: StockLevel, quantity: BigDecimal, notes: String?) {
        viewModelScope.launch {
            try {
                receiveStockUseCase(stock.productId, stock.outletId, quantity, notes)
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun registerItem(item: MenuItem, initialQty: BigDecimal) {
        viewModelScope.launch {
            try {
                val outlet = sessionManager.getCurrentOutlet() ?: return@launch
                val stock = StockLevel(
                    productId = item.id,
                    outletId = outlet.id,
                    productName = item.name,
                    quantity = initialQty
                )
                adjustStockUseCase(item.id, outlet.id, initialQty, StockMovementType.ADJUSTMENT, "Stok awal")
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
