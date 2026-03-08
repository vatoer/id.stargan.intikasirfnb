package id.stargan.intikasirfnb.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.settings.OutletSettingsRepository
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleStatus
import id.stargan.intikasirfnb.domain.transaction.SalesChannel
import id.stargan.intikasirfnb.domain.transaction.SalesChannelId
import id.stargan.intikasirfnb.domain.transaction.SalesChannelRepository
import id.stargan.intikasirfnb.domain.usecase.transaction.GetSalesByOutletUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import javax.inject.Inject

enum class DateRange {
    TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, ALL
}

data class SalesSummary(
    val count: Int = 0,
    val totalRevenue: Money = Money.zero(),
    val avgTicket: Money = Money.zero()
)

data class TransactionDetailState(
    val sale: Sale? = null,
    val channelName: String? = null,
    val cashierName: String? = null,
    val outletName: String = "",
    val outletAddress: String? = null,
    val outletPhone: String? = null
)

data class TransactionHistoryUiState(
    val allSales: List<Sale> = emptyList(),
    val filteredSales: List<Sale> = emptyList(),
    val channels: Map<SalesChannelId, SalesChannel> = emptyMap(),
    val selectedDateRange: DateRange = DateRange.TODAY,
    val selectedStatus: SaleStatus? = null,
    val selectedChannelId: SalesChannelId? = null,
    val searchQuery: String = "",
    val summary: SalesSummary = SalesSummary(),
    val detail: TransactionDetailState? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val getSalesByOutletUseCase: GetSalesByOutletUseCase,
    private val salesChannelRepository: SalesChannelRepository,
    private val outletSettingsRepository: OutletSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionHistoryUiState())
    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true, errorMessage = null)
                else it.copy(isLoading = true, errorMessage = null)
            }
            try {
                val outlet = sessionManager.getCurrentOutlet()
                    ?: error("Outlet tidak ditemukan")

                val sales = getSalesByOutletUseCase(outlet.id, limit = 500)
                val channels = salesChannelRepository.listByTenant(outlet.tenantId)
                    .associateBy { it.id }

                _uiState.update { state ->
                    val filtered = applyFilters(
                        sales, state.selectedDateRange, state.selectedStatus,
                        state.selectedChannelId, state.searchQuery
                    )
                    state.copy(
                        allSales = sales,
                        filteredSales = filtered,
                        channels = channels,
                        summary = computeSummary(filtered),
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, errorMessage = e.message)
                }
            }
        }
    }

    fun selectSale(saleId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val sale = state.allSales.find { it.id == SaleId(saleId) } ?: return@launch

            val outlet = sessionManager.getCurrentOutlet()
            val outletSettings = outlet?.let {
                outletSettingsRepository.getByOutletId(it.id)
            }
            val profile = outletSettings?.outletProfile
            val channelName = state.channels[sale.channelId]?.name
            val cashierName = sessionManager.getCurrentUser()?.displayName

            _uiState.update {
                it.copy(
                    detail = TransactionDetailState(
                        sale = sale,
                        channelName = channelName,
                        cashierName = cashierName,
                        outletName = profile?.name ?: outlet?.name ?: "",
                        outletAddress = profile?.address,
                        outletPhone = profile?.phone
                    )
                )
            }
        }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(detail = null) }
    }

    fun selectDateRange(range: DateRange) {
        _uiState.update { state ->
            val filtered = applyFilters(
                state.allSales, range, state.selectedStatus,
                state.selectedChannelId, state.searchQuery
            )
            state.copy(
                selectedDateRange = range,
                filteredSales = filtered,
                summary = computeSummary(filtered)
            )
        }
    }

    fun filterByStatus(status: SaleStatus?) {
        _uiState.update { state ->
            val newStatus = if (state.selectedStatus == status) null else status
            val filtered = applyFilters(
                state.allSales, state.selectedDateRange, newStatus,
                state.selectedChannelId, state.searchQuery
            )
            state.copy(
                selectedStatus = newStatus,
                filteredSales = filtered,
                summary = computeSummary(filtered)
            )
        }
    }

    fun filterByChannel(channelId: SalesChannelId?) {
        _uiState.update { state ->
            val newChannelId = if (state.selectedChannelId == channelId) null else channelId
            val filtered = applyFilters(
                state.allSales, state.selectedDateRange, state.selectedStatus,
                newChannelId, state.searchQuery
            )
            state.copy(
                selectedChannelId = newChannelId,
                filteredSales = filtered,
                summary = computeSummary(filtered)
            )
        }
    }

    fun search(query: String) {
        _uiState.update { state ->
            val filtered = applyFilters(
                state.allSales, state.selectedDateRange, state.selectedStatus,
                state.selectedChannelId, query
            )
            state.copy(
                searchQuery = query,
                filteredSales = filtered,
                summary = computeSummary(filtered)
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun applyFilters(
        sales: List<Sale>,
        dateRange: DateRange,
        status: SaleStatus?,
        channelId: SalesChannelId?,
        query: String
    ): List<Sale> {
        var result = sales

        // Date range
        val (startMillis, endMillis) = dateRangeBounds(dateRange)
        if (startMillis != null) {
            result = result.filter { sale ->
                sale.createdAtMillis >= startMillis &&
                    (endMillis == null || sale.createdAtMillis < endMillis)
            }
        }

        // Status
        if (status != null) {
            result = result.filter { it.status == status }
        }

        // Channel
        if (channelId != null) {
            result = result.filter { it.channelId == channelId }
        }

        // Search
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

    private fun dateRangeBounds(range: DateRange): Pair<Long?, Long?> {
        if (range == DateRange.ALL) return null to null

        val cal = Calendar.getInstance()

        // End = start of tomorrow
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val endOfToday = cal.timeInMillis

        cal.add(Calendar.DAY_OF_MONTH, -1) // back to today start
        val startOfToday = cal.timeInMillis

        return when (range) {
            DateRange.TODAY -> startOfToday to endOfToday
            DateRange.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_MONTH, -1)
                cal.timeInMillis to startOfToday
            }
            DateRange.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.timeInMillis to endOfToday
            }
            DateRange.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis to endOfToday
            }
            DateRange.ALL -> null to null
        }
    }

    private fun computeSummary(sales: List<Sale>): SalesSummary {
        val revenueSales = sales.filter {
            it.status == SaleStatus.COMPLETED || it.status == SaleStatus.PAID
        }
        val totalRevenue = revenueSales.fold(Money.zero()) { acc, sale ->
            acc + sale.totalAmount()
        }
        val avgTicket = if (revenueSales.isNotEmpty()) {
            Money(
                totalRevenue.amount.divide(
                    BigDecimal(revenueSales.size), 0, RoundingMode.HALF_UP
                )
            )
        } else Money.zero()

        return SalesSummary(
            count = sales.size,
            totalRevenue = totalRevenue,
            avgTicket = avgTicket
        )
    }
}
