package id.stargan.intikasirfnb.ui.pos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.data.printer.LogoBitmapProcessor
import id.stargan.intikasirfnb.data.printer.PrinterServiceFactory
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.printer.buildSaleReceipt
import id.stargan.intikasirfnb.domain.settings.OutletSettings
import id.stargan.intikasirfnb.domain.settings.OutletSettingsRepository
import id.stargan.intikasirfnb.domain.settings.PrinterConnectionType
import id.stargan.intikasirfnb.domain.settings.TerminalSettings
import id.stargan.intikasirfnb.domain.settings.TerminalSettingsRepository
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SalesChannelRepository
import id.stargan.intikasirfnb.domain.usecase.transaction.GetSaleByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PrintStatus { IDLE, PRINTING, SUCCESS, ERROR }

data class ReceiptUiState(
    val sale: Sale? = null,
    val outletSettings: OutletSettings? = null,
    val terminalSettings: TerminalSettings? = null,
    val cashierName: String? = null,
    val channelName: String? = null,
    val hasPrinter: Boolean = false,
    val printStatus: PrintStatus = PrintStatus.IDLE,
    val printCount: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: SessionManager,
    private val getSaleByIdUseCase: GetSaleByIdUseCase,
    private val outletSettingsRepository: OutletSettingsRepository,
    private val terminalSettingsRepository: TerminalSettingsRepository,
    private val salesChannelRepository: SalesChannelRepository,
    private val printerServiceFactory: PrinterServiceFactory,
    private val logoBitmapProcessor: LogoBitmapProcessor
) : ViewModel() {

    private val saleId: String = checkNotNull(savedStateHandle["saleId"])

    private val _uiState = MutableStateFlow(ReceiptUiState())
    val uiState: StateFlow<ReceiptUiState> = _uiState.asStateFlow()

    init {
        loadReceipt()
    }

    private fun loadReceipt() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val sale = getSaleByIdUseCase(SaleId(saleId))
                    ?: error("Transaksi tidak ditemukan")
                val outlet = sessionManager.getCurrentOutlet()
                    ?: error("Outlet tidak ditemukan")

                val outletSettings = outletSettingsRepository.getByOutletId(outlet.id)
                    ?: OutletSettings(outletId = outlet.id, tenantId = outlet.tenantId)

                val terminalSettings = terminalSettingsRepository.getByTerminalId(
                    TerminalId(outlet.id.value)
                ) ?: TerminalSettings(
                    terminalId = TerminalId(outlet.id.value),
                    outletId = outlet.id
                )

                val cashierName = sessionManager.getCurrentUser()?.displayName
                val channelName = salesChannelRepository.getById(sale.channelId)?.name

                val hasPrinter = terminalSettings.printer.connectionType != PrinterConnectionType.NONE
                    && !terminalSettings.printer.address.isNullOrBlank()

                _uiState.update {
                    it.copy(
                        sale = sale,
                        outletSettings = outletSettings,
                        terminalSettings = terminalSettings,
                        cashierName = cashierName,
                        channelName = channelName,
                        hasPrinter = hasPrinter,
                        isLoading = false
                    )
                }

                // Auto-print if configured
                if (hasPrinter && terminalSettings.printer.autoPrintReceipt) {
                    printReceipt()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun printReceipt() {
        viewModelScope.launch {
            val state = _uiState.value
            val sale = state.sale ?: return@launch
            val outletSettings = state.outletSettings ?: return@launch
            val terminalSettings = state.terminalSettings ?: return@launch

            _uiState.update { it.copy(printStatus = PrintStatus.PRINTING, errorMessage = null) }

            val service = printerServiceFactory.create(terminalSettings.printer)
            if (service == null) {
                _uiState.update {
                    it.copy(
                        printStatus = PrintStatus.ERROR,
                        errorMessage = "Printer tidak dikonfigurasi atau alamat belum diisi"
                    )
                }
                return@launch
            }

            val logoRaster = if (outletSettings.receipt.header.showLogo) {
                outletSettings.outletProfile.logoImagePath?.let { path ->
                    logoBitmapProcessor.loadCachedRaster(path)
                }
            } else null

            val receiptBytes = buildSaleReceipt(
                sale = sale,
                outletProfile = outletSettings.outletProfile,
                printerConfig = terminalSettings.printer,
                receiptConfig = outletSettings.receipt,
                cashierName = state.cashierName,
                channelName = state.channelName,
                logoRaster = logoRaster
            )

            val copies = terminalSettings.printer.receiptCopies.coerceAtLeast(1)
            var success = true

            repeat(copies) {
                service.print(receiptBytes)
                    .onFailure { e ->
                        success = false
                        _uiState.update {
                            it.copy(
                                printStatus = PrintStatus.ERROR,
                                errorMessage = "Gagal cetak: ${e.message}"
                            )
                        }
                        return@launch
                    }
            }

            if (success) {
                _uiState.update {
                    it.copy(
                        printStatus = PrintStatus.SUCCESS,
                        printCount = it.printCount + copies
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetPrintStatus() {
        _uiState.update { it.copy(printStatus = PrintStatus.IDLE) }
    }
}
