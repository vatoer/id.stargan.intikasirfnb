package id.stargan.intikasirfnb.ui.pos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.data.printer.LogoBitmapProcessor
import id.stargan.intikasirfnb.data.printer.PrinterServiceFactory
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.printer.buildSaleReceipt
import id.stargan.intikasirfnb.domain.settings.OutletSettings
import id.stargan.intikasirfnb.domain.settings.OutletSettingsRepository
import id.stargan.intikasirfnb.domain.settings.PrinterConnectionType
import id.stargan.intikasirfnb.domain.settings.TerminalSettings
import id.stargan.intikasirfnb.domain.settings.TerminalSettingsRepository
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.Payment
import id.stargan.intikasirfnb.domain.transaction.PaymentMethod
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleStatus
import id.stargan.intikasirfnb.domain.transaction.SalesChannelRepository
import id.stargan.intikasirfnb.domain.usecase.transaction.AddPaymentUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.CompleteSaleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.ConfirmSaleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetSaleByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/** A payment entry staged locally before final submission */
data class StagedPayment(
    val method: PaymentMethod,
    val amount: Money,
    val reference: String? = null
)

data class PaymentUiState(
    val sale: Sale? = null,
    val selectedMethod: PaymentMethod = PaymentMethod.CASH,
    val amountInput: String = "",
    val paymentReference: String = "",
    val isProcessing: Boolean = false,
    val isLoading: Boolean = true,
    val isCompleted: Boolean = false,
    val stagedPayments: List<StagedPayment> = emptyList(),
    val errorMessage: String? = null,
    // Receipt / print state
    val outletSettings: OutletSettings? = null,
    val terminalSettings: TerminalSettings? = null,
    val cashierName: String? = null,
    val channelName: String? = null,
    val hasPrinter: Boolean = false,
    val printStatus: PrintStatus = PrintStatus.IDLE,
    val printCount: Int = 0
) {
    val totalAmount: Money get() = sale?.totalAmount() ?: Money.zero()

    /** Total of staged payments (not yet saved to sale) */
    val stagedTotal: Money
        get() = stagedPayments.fold(Money.zero()) { acc, p -> acc + p.amount }

    /** Remaining = total - staged total */
    val remainingAmount: Money
        get() {
            val diff = totalAmount - stagedTotal
            return if (diff.amount > BigDecimal.ZERO) diff else Money.zero()
        }

    val amountInputValue: BigDecimal
        get() = amountInput.replace(".", "").replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO

    /** Change due: only for cash, only the cash entry that exceeds remaining */
    val changeDue: Money
        get() {
            val overPay = stagedTotal - totalAmount
            val hasCash = stagedPayments.any { it.method == PaymentMethod.CASH }
            return if (overPay.amount > BigDecimal.ZERO && hasCash) overPay else Money.zero()
        }

    /** Can add a staged payment entry */
    val canAddStaged: Boolean
        get() {
            if (sale == null) return false
            if (remainingAmount.amount <= BigDecimal.ZERO) return false
            if (amountInputValue <= BigDecimal.ZERO) return false
            // Non-cash: can't exceed remaining
            if (selectedMethod != PaymentMethod.CASH && amountInputValue > remainingAmount.amount) return false
            return true
        }

    /** Can process payment: staged payments cover total */
    val canPay: Boolean
        get() {
            if (sale == null) return false
            return stagedTotal.amount >= totalAmount.amount
        }

    val isFullyStaged: Boolean
        get() = sale != null && stagedTotal.amount >= totalAmount.amount
}

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: SessionManager,
    private val getSaleByIdUseCase: GetSaleByIdUseCase,
    private val confirmSaleUseCase: ConfirmSaleUseCase,
    private val addPaymentUseCase: AddPaymentUseCase,
    private val completeSaleUseCase: CompleteSaleUseCase,
    private val outletSettingsRepository: OutletSettingsRepository,
    private val terminalSettingsRepository: TerminalSettingsRepository,
    private val salesChannelRepository: SalesChannelRepository,
    private val printerServiceFactory: PrinterServiceFactory,
    private val logoBitmapProcessor: LogoBitmapProcessor
) : ViewModel() {

    private val saleId: String = checkNotNull(savedStateHandle["saleId"])

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        loadAndConfirmSale()
    }

    private fun loadAndConfirmSale() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val sale = getSaleByIdUseCase(SaleId(saleId))
                    ?: error("Transaksi tidak ditemukan")

                val confirmed = if (sale.status == SaleStatus.DRAFT || sale.status == SaleStatus.OPEN) {
                    val outlet = sessionManager.getCurrentOutlet()
                        ?: error("Outlet tidak ditemukan")
                    confirmSaleUseCase(SaleId(saleId), outlet.tenantId).getOrThrow()
                } else {
                    sale
                }

                _uiState.update {
                    it.copy(
                        sale = confirmed,
                        amountInput = "",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _uiState.update { state ->
            val remaining = state.remainingAmount.amount.toLong()
            state.copy(
                selectedMethod = method,
                paymentReference = "",
                // Non-cash autofill remaining; cash clear for manual entry
                amountInput = if (method != PaymentMethod.CASH) {
                    remaining.toString()
                } else {
                    ""
                }
            )
        }
    }

    fun updateAmountInput(input: String) {
        val cleaned = input.filter { it.isDigit() }
        _uiState.update { state ->
            val value = cleaned.toBigDecimalOrNull() ?: BigDecimal.ZERO
            // For non-cash, cap at remaining amount
            val capped = if (state.selectedMethod != PaymentMethod.CASH) {
                val max = state.remainingAmount.amount.toLong()
                val inputLong = value.toLong()
                if (inputLong > max) max.toString() else cleaned
            } else {
                cleaned
            }
            state.copy(amountInput = capped)
        }
    }

    fun updatePaymentReference(ref: String) {
        _uiState.update { it.copy(paymentReference = ref) }
    }

    fun selectQuickCash(amount: Long) {
        _uiState.update { it.copy(amountInput = amount.toString()) }
    }

    // --- Stage a payment entry locally ---

    fun addStagedPayment() {
        _uiState.update { state ->
            if (!state.canAddStaged) return@update state

            val entry = StagedPayment(
                method = state.selectedMethod,
                amount = Money(state.amountInputValue),
                reference = state.paymentReference.ifBlank { null }
            )

            val newStaged = state.stagedPayments + entry
            val newRemaining = state.totalAmount - newStaged.fold(Money.zero()) { acc, p -> acc + p.amount }
            val remainingPositive = if (newRemaining.amount > BigDecimal.ZERO) newRemaining.amount.toLong() else 0L

            state.copy(
                stagedPayments = newStaged,
                amountInput = if (remainingPositive > 0) "" else "",
                paymentReference = "",
                selectedMethod = PaymentMethod.CASH
            )
        }
    }

    fun removeStagedPayment(index: Int) {
        _uiState.update { state ->
            if (index !in state.stagedPayments.indices) return@update state
            val newStaged = state.stagedPayments.toMutableList().apply { removeAt(index) }
            state.copy(
                stagedPayments = newStaged,
                amountInput = ""
            )
        }
    }

    // --- Process payment ---

    fun processPayment() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.canPay || state.isProcessing) return@launch

            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            try {
                val paymentsToAdd = state.stagedPayments.map { staged ->
                    Triple(staged.method, staged.amount, staged.reference)
                }

                var currentSale: Sale? = null
                for ((method, amount, ref) in paymentsToAdd) {
                    currentSale = addPaymentUseCase(
                        saleId = SaleId(saleId),
                        method = method,
                        amount = amount,
                        reference = ref
                    ).getOrThrow()
                }

                val afterPayments = currentSale!!

                // If PAID, complete the sale
                val result = if (afterPayments.status == SaleStatus.PAID) {
                    completeSaleUseCase(SaleId(saleId)).getOrThrow()
                } else {
                    afterPayments
                }

                val isCompleted = result.status == SaleStatus.COMPLETED

                _uiState.update {
                    it.copy(
                        sale = result,
                        isProcessing = false,
                        isCompleted = isCompleted,
                        stagedPayments = emptyList()
                    )
                }

                if (isCompleted) {
                    loadReceiptData(result)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = e.message) }
            }
        }
    }

    private suspend fun loadReceiptData(sale: Sale) {
        try {
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
                    outletSettings = outletSettings,
                    terminalSettings = terminalSettings,
                    cashierName = cashierName,
                    channelName = channelName,
                    hasPrinter = hasPrinter
                )
            }

            if (hasPrinter && terminalSettings.printer.autoPrintReceipt) {
                printReceipt()
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Gagal memuat data struk: ${e.message}") }
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

    fun resetPrintStatus() {
        _uiState.update { it.copy(printStatus = PrintStatus.IDLE) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
