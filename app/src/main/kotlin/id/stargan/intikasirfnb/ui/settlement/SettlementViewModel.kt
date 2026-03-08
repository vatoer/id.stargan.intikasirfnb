package id.stargan.intikasirfnb.ui.settlement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlement
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementId
import id.stargan.intikasirfnb.domain.transaction.SalesChannel
import id.stargan.intikasirfnb.domain.transaction.SalesChannelId
import id.stargan.intikasirfnb.domain.transaction.SalesChannelRepository
import id.stargan.intikasirfnb.domain.transaction.SettlementStatus
import id.stargan.intikasirfnb.domain.usecase.transaction.BatchSettleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetPendingSettlementsUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetSettlementSummaryUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.MarkSettlementDisputedUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.MarkSettlementSettledUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.CancelSettlementUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.SettlementSummary
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class SettlementTab { PENDING, SETTLED, ALL }

data class SettlementUiState(
    val settlements: List<PlatformSettlement> = emptyList(),
    val channels: Map<SalesChannelId, SalesChannel> = emptyMap(),
    val summary: SettlementSummary? = null,
    val selectedTab: SettlementTab = SettlementTab.PENDING,
    val selectedChannelId: SalesChannelId? = null,
    val selectedSettlement: PlatformSettlement? = null,
    val showSettleDialog: Boolean = false,
    val showBatchSettleDialog: Boolean = false,
    val selectedForBatch: Set<PlatformSettlementId> = emptySet(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val salesChannelRepository: SalesChannelRepository,
    private val settlementRepository: PlatformSettlementRepository,
    private val getPendingSettlementsUseCase: GetPendingSettlementsUseCase,
    private val getSettlementSummaryUseCase: GetSettlementSummaryUseCase,
    private val markSettlementSettledUseCase: MarkSettlementSettledUseCase,
    private val markSettlementDisputedUseCase: MarkSettlementDisputedUseCase,
    private val cancelSettlementUseCase: CancelSettlementUseCase,
    private val batchSettleUseCase: BatchSettleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettlementUiState())
    val uiState: StateFlow<SettlementUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val outlet = sessionManager.getCurrentOutlet()
                    ?: error("Outlet tidak ditemukan")

                val channels = salesChannelRepository.listByTenant(outlet.tenantId)
                    .filter { it.channelType == id.stargan.intikasirfnb.domain.transaction.ChannelType.DELIVERY_PLATFORM }
                    .associateBy { it.id }

                // Summary: this month
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val fromMillis = cal.timeInMillis
                val toMillis = System.currentTimeMillis()

                val summary = getSettlementSummaryUseCase(outlet.id, fromMillis, toMillis)

                val state = _uiState.value
                val settlements = loadSettlementsByTab(state.selectedTab, state.selectedChannelId)

                _uiState.update {
                    it.copy(
                        settlements = settlements,
                        channels = channels,
                        summary = summary,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun selectTab(tab: SettlementTab) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedTab = tab, selectedForBatch = emptySet()) }
            reloadList()
        }
    }

    fun filterByChannel(channelId: SalesChannelId?) {
        viewModelScope.launch {
            val newId = if (_uiState.value.selectedChannelId == channelId) null else channelId
            _uiState.update { it.copy(selectedChannelId = newId, selectedForBatch = emptySet()) }
            reloadList()
        }
    }

    fun selectSettlement(settlement: PlatformSettlement) {
        _uiState.update { it.copy(selectedSettlement = settlement) }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(selectedSettlement = null) }
    }

    fun showSettleDialog(settlement: PlatformSettlement) {
        _uiState.update { it.copy(selectedSettlement = settlement, showSettleDialog = true) }
    }

    fun dismissSettleDialog() {
        _uiState.update { it.copy(showSettleDialog = false) }
    }

    fun markSettled(settlementId: PlatformSettlementId, amount: Money, reference: String?) {
        viewModelScope.launch {
            markSettlementSettledUseCase(settlementId, amount, reference)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showSettleDialog = false,
                            selectedSettlement = null,
                            successMessage = "Settlement berhasil dicatat"
                        )
                    }
                    loadData()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    fun markDisputed(settlementId: PlatformSettlementId, notes: String?) {
        viewModelScope.launch {
            markSettlementDisputedUseCase(settlementId, notes)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            selectedSettlement = null,
                            successMessage = "Settlement ditandai sebagai dispute"
                        )
                    }
                    loadData()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    fun cancelSettlement(settlementId: PlatformSettlementId) {
        viewModelScope.launch {
            cancelSettlementUseCase(settlementId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            selectedSettlement = null,
                            successMessage = "Settlement dibatalkan"
                        )
                    }
                    loadData()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    // --- Batch settle ---

    fun toggleBatchSelection(settlementId: PlatformSettlementId) {
        _uiState.update { state ->
            val newSet = state.selectedForBatch.toMutableSet()
            if (newSet.contains(settlementId)) newSet.remove(settlementId)
            else newSet.add(settlementId)
            state.copy(selectedForBatch = newSet)
        }
    }

    fun selectAllPending() {
        _uiState.update { state ->
            val pendingIds = state.settlements
                .filter { it.status == SettlementStatus.PENDING }
                .map { it.id }
                .toSet()
            state.copy(selectedForBatch = pendingIds)
        }
    }

    fun clearBatchSelection() {
        _uiState.update { it.copy(selectedForBatch = emptySet()) }
    }

    fun showBatchSettleDialog() {
        _uiState.update { it.copy(showBatchSettleDialog = true) }
    }

    fun dismissBatchSettleDialog() {
        _uiState.update { it.copy(showBatchSettleDialog = false) }
    }

    fun batchSettle(totalAmount: Money, reference: String?) {
        viewModelScope.launch {
            val ids = _uiState.value.selectedForBatch.toList()
            batchSettleUseCase(ids, totalAmount, reference)
                .onSuccess { result ->
                    val msg = if (result.hasDiscrepancy) {
                        "Batch settled dengan selisih: ${result.totalExpected} vs ${result.totalSettled}"
                    } else {
                        "${result.settled.size} settlement berhasil dicatat"
                    }
                    _uiState.update {
                        it.copy(
                            showBatchSettleDialog = false,
                            selectedForBatch = emptySet(),
                            successMessage = msg
                        )
                    }
                    loadData()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    private suspend fun reloadList() {
        try {
            val state = _uiState.value
            val settlements = loadSettlementsByTab(state.selectedTab, state.selectedChannelId)
            _uiState.update { it.copy(settlements = settlements) }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    private suspend fun loadSettlementsByTab(
        tab: SettlementTab,
        channelId: SalesChannelId?
    ): List<PlatformSettlement> {
        val outlet = sessionManager.getCurrentOutlet() ?: return emptyList()
        return when (tab) {
            SettlementTab.PENDING -> {
                if (channelId != null) {
                    settlementRepository.listPendingByChannel(channelId)
                } else {
                    settlementRepository.listPending(outlet.id)
                }
            }
            SettlementTab.SETTLED -> {
                settlementRepository.listByStatus(outlet.id, SettlementStatus.SETTLED)
                    .let { settled ->
                        if (channelId != null) settled.filter { it.channelId == channelId }
                        else settled
                    }
            }
            SettlementTab.ALL -> {
                settlementRepository.listByOutlet(outlet.id, limit = 200)
                    .let { all ->
                        if (channelId != null) all.filter { it.channelId == channelId }
                        else all
                    }
            }
        }
    }
}
