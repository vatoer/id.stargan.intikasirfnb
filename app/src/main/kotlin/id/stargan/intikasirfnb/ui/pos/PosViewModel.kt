package id.stargan.intikasirfnb.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.catalog.Category
import id.stargan.intikasirfnb.domain.catalog.CategoryId
import id.stargan.intikasirfnb.domain.catalog.CategoryRepository
import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.MenuItemRepository
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.transaction.OrderLineId
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SalesChannel
import id.stargan.intikasirfnb.domain.usecase.transaction.AddLineItemUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.CreateSaleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetSalesChannelsUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.RemoveLineItemUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.UpdateLineItemUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PosUiState(
    val salesChannels: List<SalesChannel> = emptyList(),
    val selectedChannel: SalesChannel? = null,
    val categories: List<Category> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val filteredItems: List<MenuItem> = emptyList(),
    val selectedCategoryId: CategoryId? = null,
    val currentSale: Sale? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class PosViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val categoryRepository: CategoryRepository,
    private val menuItemRepository: MenuItemRepository,
    private val getSalesChannelsUseCase: GetSalesChannelsUseCase,
    private val createSaleUseCase: CreateSaleUseCase,
    private val addLineItemUseCase: AddLineItemUseCase,
    private val updateLineItemUseCase: UpdateLineItemUseCase,
    private val removeLineItemUseCase: RemoveLineItemUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val outlet = sessionManager.getCurrentOutlet() ?: return@launch
                val tenantId = outlet.tenantId

                val channels = getSalesChannelsUseCase(tenantId)
                val categories = categoryRepository.listByTenant(tenantId)
                    .filter { it.isActive }
                    .sortedBy { it.sortOrder }
                val menuItems = menuItemRepository.listByTenant(tenantId)
                    .filter { it.isActive }
                    .sortedBy { it.sortOrder }

                val defaultChannel = channels.firstOrNull()

                _uiState.update {
                    it.copy(
                        salesChannels = channels,
                        selectedChannel = defaultChannel,
                        categories = categories,
                        menuItems = menuItems,
                        filteredItems = menuItems,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun selectChannel(channel: SalesChannel) {
        _uiState.update { it.copy(selectedChannel = channel) }
    }

    fun selectCategory(categoryId: CategoryId?) {
        _uiState.update { state ->
            val filtered = if (categoryId == null) {
                state.menuItems
            } else {
                state.menuItems.filter { it.categoryId == categoryId }
            }
            state.copy(
                selectedCategoryId = categoryId,
                filteredItems = applySearch(filtered, state.searchQuery)
            )
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { state ->
            val byCategory = if (state.selectedCategoryId == null) {
                state.menuItems
            } else {
                state.menuItems.filter { it.categoryId == state.selectedCategoryId }
            }
            state.copy(
                searchQuery = query,
                filteredItems = applySearch(byCategory, query)
            )
        }
    }

    fun addItemToCart(menuItem: MenuItem) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val sale = state.currentSale

                if (sale == null) {
                    // Create a new draft sale first
                    val outlet = sessionManager.getCurrentOutlet() ?: return@launch
                    val channel = state.selectedChannel ?: return@launch
                    val user = sessionManager.getCurrentUser()

                    val createResult = createSaleUseCase(
                        outletId = outlet.id,
                        channelId = channel.id,
                        cashierId = user?.id
                    )
                    val newSale = createResult.getOrThrow()

                    // Now add the item
                    val addResult = addLineItemUseCase(
                        saleId = newSale.id,
                        menuItem = menuItem,
                        quantity = 1
                    )
                    _uiState.update { it.copy(currentSale = addResult.getOrThrow()) }
                } else {
                    // Check if item already in cart → increment qty
                    val existingLine = sale.lines.find {
                        it.productRef.productId == menuItem.id && it.selectedModifiers.isEmpty()
                    }
                    if (existingLine != null) {
                        val result = updateLineItemUseCase(
                            saleId = sale.id,
                            lineId = existingLine.id,
                            quantity = existingLine.quantity + 1
                        )
                        _uiState.update { it.copy(currentSale = result.getOrThrow()) }
                    } else {
                        val result = addLineItemUseCase(
                            saleId = sale.id,
                            menuItem = menuItem,
                            quantity = 1
                        )
                        _uiState.update { it.copy(currentSale = result.getOrThrow()) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun incrementLine(lineId: OrderLineId) {
        viewModelScope.launch {
            try {
                val sale = _uiState.value.currentSale ?: return@launch
                val line = sale.lines.find { it.id == lineId } ?: return@launch
                val result = updateLineItemUseCase(
                    saleId = sale.id,
                    lineId = lineId,
                    quantity = line.quantity + 1
                )
                _uiState.update { it.copy(currentSale = result.getOrThrow()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun decrementLine(lineId: OrderLineId) {
        viewModelScope.launch {
            try {
                val sale = _uiState.value.currentSale ?: return@launch
                val line = sale.lines.find { it.id == lineId } ?: return@launch
                if (line.quantity <= 1) {
                    removeLine(lineId)
                } else {
                    val result = updateLineItemUseCase(
                        saleId = sale.id,
                        lineId = lineId,
                        quantity = line.quantity - 1
                    )
                    _uiState.update { it.copy(currentSale = result.getOrThrow()) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun removeLine(lineId: OrderLineId) {
        viewModelScope.launch {
            try {
                val sale = _uiState.value.currentSale ?: return@launch
                val result = removeLineItemUseCase(saleId = sale.id, lineId = lineId)
                _uiState.update { it.copy(currentSale = result.getOrThrow()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearCart() {
        _uiState.update { it.copy(currentSale = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun applySearch(items: List<MenuItem>, query: String): List<MenuItem> {
        if (query.isBlank()) return items
        val lower = query.lowercase()
        return items.filter { it.name.lowercase().contains(lower) }
    }
}
