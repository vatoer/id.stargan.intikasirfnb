package id.stargan.intikasirfnb.ui.pos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.catalog.Category
import id.stargan.intikasirfnb.domain.catalog.CategoryId
import id.stargan.intikasirfnb.domain.catalog.CategoryRepository
import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.MenuItemRepository
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.transaction.ChannelType
import id.stargan.intikasirfnb.domain.transaction.OrderFlowType
import id.stargan.intikasirfnb.domain.transaction.OrderLineId
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleStatus
import id.stargan.intikasirfnb.domain.transaction.SalesChannel
import id.stargan.intikasirfnb.domain.usecase.transaction.AddLineItemUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.CreateSaleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GenerateQueueNumberUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetOpenSalesUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetSaleByIdUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetSalesChannelsUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.KitchenTicketResult
import id.stargan.intikasirfnb.domain.usecase.transaction.RemoveLineItemUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.SendToKitchenUseCase
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
    val orderFlowOverride: OrderFlowType? = null, // null = use channel default
    val categories: List<Category> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val filteredItems: List<MenuItem> = emptyList(),
    val selectedCategoryId: CategoryId? = null,
    val currentSale: Sale? = null,
    val openOrders: List<Sale> = emptyList(),
    val showOpenOrders: Boolean = false,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSendingToKitchen: Boolean = false,
    val kitchenTicketResult: KitchenTicketResult? = null,
    val errorMessage: String? = null
) {
    /** Effective order flow: from current sale if exists, otherwise override or channel default */
    val effectiveOrderFlow: OrderFlowType
        get() = currentSale?.orderFlow
            ?: orderFlowOverride
            ?: selectedChannel?.defaultOrderFlow
            ?: OrderFlowType.PAY_FIRST
}

@HiltViewModel
class PosViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: SessionManager,
    private val categoryRepository: CategoryRepository,
    private val menuItemRepository: MenuItemRepository,
    private val getSalesChannelsUseCase: GetSalesChannelsUseCase,
    private val createSaleUseCase: CreateSaleUseCase,
    private val addLineItemUseCase: AddLineItemUseCase,
    private val updateLineItemUseCase: UpdateLineItemUseCase,
    private val removeLineItemUseCase: RemoveLineItemUseCase,
    private val getSaleByIdUseCase: GetSaleByIdUseCase,
    private val sendToKitchenUseCase: SendToKitchenUseCase,
    private val getOpenSalesUseCase: GetOpenSalesUseCase,
    private val generateQueueNumberUseCase: GenerateQueueNumberUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    private val resumeSaleId: String? = savedStateHandle["saleId"]

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

                // Resume draft/open sale if saleId was passed
                val draftSale = resumeSaleId?.let { id ->
                    getSaleByIdUseCase(SaleId(id))
                }
                val selectedChannel = if (draftSale != null) {
                    channels.find { it.id == draftSale.channelId } ?: defaultChannel
                } else defaultChannel

                // Load open orders for this outlet
                val openOrders = getOpenSalesUseCase(outlet.id)

                _uiState.update {
                    it.copy(
                        salesChannels = channels,
                        selectedChannel = selectedChannel,
                        categories = categories,
                        menuItems = menuItems,
                        filteredItems = menuItems,
                        currentSale = draftSale,
                        openOrders = openOrders,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun selectChannel(channel: SalesChannel) {
        _uiState.update { it.copy(selectedChannel = channel, orderFlowOverride = null) }
    }

    fun overrideOrderFlow(flow: OrderFlowType?) {
        _uiState.update { it.copy(orderFlowOverride = flow) }
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
                        cashierId = user?.id,
                        orderFlowOverride = state.orderFlowOverride
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
                    // Check if item already in cart (unsent only) → increment qty
                    val existingLine = sale.lines.find {
                        it.productRef.productId == menuItem.id &&
                            it.selectedModifiers.isEmpty() &&
                            !it.isSentToKitchen
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

    fun sendToKitchen() {
        viewModelScope.launch {
            try {
                val sale = _uiState.value.currentSale ?: return@launch
                _uiState.update { it.copy(isSendingToKitchen = true) }
                val result = sendToKitchenUseCase(sale.id)
                val ticketResult = result.getOrThrow()
                // Refresh open orders
                val outlet = sessionManager.getCurrentOutlet()
                val openOrders = outlet?.let { getOpenSalesUseCase(it.id) } ?: emptyList()
                _uiState.update {
                    it.copy(
                        currentSale = ticketResult.sale,
                        openOrders = openOrders,
                        isSendingToKitchen = false,
                        kitchenTicketResult = ticketResult
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSendingToKitchen = false, errorMessage = e.message) }
            }
        }
    }

    fun clearKitchenTicketResult() {
        _uiState.update { it.copy(kitchenTicketResult = null) }
    }

    fun toggleOpenOrders() {
        viewModelScope.launch {
            val show = !_uiState.value.showOpenOrders
            if (show) {
                // Refresh open orders
                val outlet = sessionManager.getCurrentOutlet()
                val openOrders = outlet?.let { getOpenSalesUseCase(it.id) } ?: emptyList()
                _uiState.update { it.copy(showOpenOrders = true, openOrders = openOrders) }
            } else {
                _uiState.update { it.copy(showOpenOrders = false) }
            }
        }
    }

    fun resumeOpenOrder(saleId: SaleId) {
        viewModelScope.launch {
            try {
                val sale = getSaleByIdUseCase(saleId) ?: error("Pesanan tidak ditemukan")
                val channel = _uiState.value.salesChannels.find { it.id == sale.channelId }
                _uiState.update {
                    it.copy(
                        currentSale = sale,
                        selectedChannel = channel ?: it.selectedChannel,
                        showOpenOrders = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun newOrder() {
        _uiState.update { it.copy(currentSale = null, showOpenOrders = false) }
    }

    fun clearCart() {
        _uiState.update { it.copy(currentSale = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Assign queue number to current sale (for PAY_FIRST flow after payment). */
    fun assignQueueNumber(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val sale = _uiState.value.currentSale ?: return@launch
                val existingQueue = sale.queueNumber
                if (existingQueue != null) {
                    onComplete(existingQueue)
                    return@launch
                }
                val outlet = sessionManager.getCurrentOutlet() ?: return@launch
                val queueNumber = generateQueueNumberUseCase(outlet.id)
                val updated = sale.copy(queueNumber = queueNumber, updatedAtMillis = System.currentTimeMillis())
                _uiState.update { it.copy(currentSale = updated) }
                onComplete(queueNumber)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    private fun applySearch(items: List<MenuItem>, query: String): List<MenuItem> {
        if (query.isBlank()) return items
        val lower = query.lowercase()
        return items.filter { it.name.lowercase().contains(lower) }
    }
}
