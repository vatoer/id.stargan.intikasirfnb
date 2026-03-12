package id.stargan.intikasirfnb.domain.transaction

import id.stargan.intikasirfnb.domain.catalog.ProductRef
import id.stargan.intikasirfnb.domain.customer.CustomerId
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.identity.UserId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.shared.UlidGenerator
import java.math.BigDecimal
import java.math.RoundingMode

// --- Value objects ---

@JvmInline
value class SaleId(val value: String) {
    companion object {
        fun generate() = SaleId(UlidGenerator.generate())
    }
}

@JvmInline
value class OrderLineId(val value: String) {
    companion object {
        fun generate() = OrderLineId(UlidGenerator.generate())
    }
}

@JvmInline
value class PaymentId(val value: String) {
    companion object {
        fun generate() = PaymentId(UlidGenerator.generate())
    }
}

@JvmInline
value class CashierSessionId(val value: String) {
    companion object {
        fun generate() = CashierSessionId(UlidGenerator.generate())
    }
}

@JvmInline
value class TableId(val value: String) {
    companion object {
        fun generate() = TableId(UlidGenerator.generate())
    }
}

enum class SaleStatus {
    DRAFT,
    OPEN,       // Sent to kitchen, still accepting additional items
    CONFIRMED,
    PAID,
    COMPLETED,
    VOIDED
}

enum class PaymentMethod {
    CASH, CARD, E_WALLET, TRANSFER,
    // Platform settlement methods — money collected by platform, settled later
    PLATFORM_GOFOOD,
    PLATFORM_GRABFOOD,
    PLATFORM_SHOPEEFOOD,
    PLATFORM_OTHER,
    OTHER;

    /** Whether this payment method is a platform settlement (AR/receivable) */
    val isPlatformSettlement: Boolean
        get() = this in listOf(PLATFORM_GOFOOD, PLATFORM_GRABFOOD, PLATFORM_SHOPEEFOOD, PLATFORM_OTHER)
}

// --- Selected modifier (snapshot of chosen modifiers at time of order) ---

data class SelectedModifier(
    val groupName: String,
    val optionName: String,
    val priceDelta: Money = Money.zero()
)

// --- Selected add-on (snapshot of chosen add-ons at time of order) ---

data class SelectedAddOn(
    val addOnName: String,
    val quantity: Int,
    val unitPrice: Money,
    val totalPrice: Money = unitPrice * quantity
)

// --- Tax / Service Charge / Tip snapshots on Sale ---

data class TaxLine(
    val taxName: String,
    val taxRate: BigDecimal,
    val isIncludedInPrice: Boolean,
    val taxableAmount: Money,
    val taxAmount: Money
) {
    companion object {
        fun compute(
            taxName: String,
            taxRate: BigDecimal,
            isIncludedInPrice: Boolean,
            taxableAmount: Money
        ): TaxLine {
            val amount = if (isIncludedInPrice) {
                // Extract tax from inclusive price: amount * rate / (100 + rate)
                taxableAmount.amount.multiply(taxRate)
                    .divide(BigDecimal(100) + taxRate, 0, RoundingMode.HALF_UP)
            } else {
                // Add tax to price: amount * rate / 100
                taxableAmount.amount.multiply(taxRate)
                    .divide(BigDecimal(100), 0, RoundingMode.HALF_UP)
            }
            return TaxLine(
                taxName = taxName,
                taxRate = taxRate,
                isIncludedInPrice = isIncludedInPrice,
                taxableAmount = taxableAmount,
                taxAmount = Money(amount, taxableAmount.currencyCode)
            )
        }
    }
}

data class ServiceChargeLine(
    val rate: BigDecimal,
    val isIncludedInPrice: Boolean,
    val baseAmount: Money,
    val chargeAmount: Money
) {
    companion object {
        fun compute(
            rate: BigDecimal,
            isIncludedInPrice: Boolean,
            baseAmount: Money
        ): ServiceChargeLine {
            val amount = if (isIncludedInPrice) {
                baseAmount.amount.multiply(rate)
                    .divide(BigDecimal(100) + rate, 0, RoundingMode.HALF_UP)
            } else {
                baseAmount.amount.multiply(rate)
                    .divide(BigDecimal(100), 0, RoundingMode.HALF_UP)
            }
            return ServiceChargeLine(
                rate = rate,
                isIncludedInPrice = isIncludedInPrice,
                baseAmount = baseAmount,
                chargeAmount = Money(amount, baseAmount.currencyCode)
            )
        }
    }
}

data class TipLine(
    val amount: Money
)

// --- Order line (within Sale aggregate) ---

data class OrderLine(
    val id: OrderLineId = OrderLineId.generate(),
    val productRef: ProductRef,
    val quantity: Int,
    val unitPrice: Money,
    val discountAmount: Money = Money.zero(),
    val selectedModifiers: List<SelectedModifier> = emptyList(),
    val selectedAddOns: List<SelectedAddOn> = emptyList(),
    val notes: String? = null,
    val isSentToKitchen: Boolean = false
) {
    fun modifierTotal(): Money =
        selectedModifiers.fold(Money.zero()) { acc, m -> acc + m.priceDelta }

    fun addOnTotal(): Money =
        selectedAddOns.fold(Money.zero()) { acc, a -> acc + a.totalPrice }

    fun effectiveUnitPrice(): Money = unitPrice + modifierTotal()

    /** lineTotal = (effectiveUnitPrice * qty) + addOnTotal - discount.
     *  addOnTotal is NOT multiplied by qty (add-ons are per-line, not per-unit). */
    fun lineTotal(): Money = (effectiveUnitPrice() * quantity) + addOnTotal() - discountAmount
}

// --- Payment breakdown (for receipt / reporting) ---

data class PaymentBreakdownEntry(
    val method: PaymentMethod,
    val count: Int,
    val total: Money
)

data class PaymentBreakdown(
    val entries: List<PaymentBreakdownEntry>,
    val totalPaid: Money,
    val changeDue: Money,
    val totalAmount: Money
) {
    val isMixed: Boolean get() = entries.size > 1
    fun entryFor(method: PaymentMethod): PaymentBreakdownEntry? = entries.find { it.method == method }
}

// --- Payment (within Sale aggregate) ---

data class Payment(
    val id: PaymentId = PaymentId.generate(),
    val method: PaymentMethod,
    val amount: Money,
    val reference: String? = null,
    val payerIndex: Int? = null  // Links to SplitBillEntry.payerIndex when split bill active
)

// --- Sale (Order) aggregate root ---

data class Sale(
    val id: SaleId,
    val outletId: OutletId,
    val channelId: SalesChannelId,
    val orderFlow: OrderFlowType = OrderFlowType.PAY_FIRST,
    val receiptNumber: String? = null,
    val queueNumber: String? = null,
    val tableId: TableId? = null,
    val externalOrderId: String? = null,
    val cashierId: UserId? = null,
    val customerId: CustomerId? = null,
    val customerName: String? = null,
    val lines: List<OrderLine> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val taxLines: List<TaxLine> = emptyList(),
    val serviceCharge: ServiceChargeLine? = null,
    val tip: TipLine? = null,
    val platformPayment: PlatformPayment? = null,
    val splitBill: SplitBill? = null,
    val status: SaleStatus = SaleStatus.DRAFT,
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun subtotal(): Money = lines.fold(Money.zero()) { acc, line -> acc + line.lineTotal() }

    fun taxTotal(): Money = taxLines
        .filter { !it.isIncludedInPrice }
        .fold(Money.zero()) { acc, t -> acc + t.taxAmount }

    fun inclusiveTaxTotal(): Money = taxLines
        .filter { it.isIncludedInPrice }
        .fold(Money.zero()) { acc, t -> acc + t.taxAmount }

    fun serviceChargeAmount(): Money =
        serviceCharge?.takeIf { !it.isIncludedInPrice }?.chargeAmount ?: Money.zero()

    fun inclusiveServiceChargeAmount(): Money =
        serviceCharge?.takeIf { it.isIncludedInPrice }?.chargeAmount ?: Money.zero()

    fun tipAmount(): Money = tip?.amount ?: Money.zero()

    fun totalAmount(): Money = subtotal() + taxTotal() + serviceChargeAmount() + tipAmount()

    fun totalPaid(): Money = payments.fold(Money.zero()) { acc, p -> acc + p.amount }

    fun isFullyPaid(): Boolean =
        totalPaid().amount >= totalAmount().amount && totalAmount().amount > BigDecimal.ZERO

    fun changeDue(): Money {
        val diff = totalPaid() - totalAmount()
        return if (diff.amount > BigDecimal.ZERO) diff else Money.zero()
    }

    /** Remaining amount to pay */
    fun remainingAmount(): Money {
        val diff = totalAmount() - totalPaid()
        return if (diff.amount > BigDecimal.ZERO) diff else Money.zero()
    }

    // --- Multi-payment helpers ---

    /** Group payments by method */
    fun paymentsByMethod(): Map<PaymentMethod, List<Payment>> = payments.groupBy { it.method }

    /** Total paid by specific method */
    fun totalByMethod(method: PaymentMethod): Money =
        payments.filter { it.method == method }.fold(Money.zero()) { acc, p -> acc + p.amount }

    /** Total of cash payments */
    fun cashTotal(): Money = totalByMethod(PaymentMethod.CASH)

    /** Total of non-cash payments */
    fun nonCashTotal(): Money =
        payments.filter { it.method != PaymentMethod.CASH }.fold(Money.zero()) { acc, p -> acc + p.amount }

    /** Whether sale has mixed payment methods */
    val isMixedPayment: Boolean get() = payments.map { it.method }.distinct().size > 1

    /** Payment count */
    val paymentCount: Int get() = payments.size

    /** Payment breakdown for receipt/reporting */
    fun paymentBreakdown(): PaymentBreakdown {
        val byMethod = paymentsByMethod()
        val entries = byMethod.map { (method, pList) ->
            PaymentBreakdownEntry(
                method = method,
                count = pList.size,
                total = pList.fold(Money.zero()) { acc, p -> acc + p.amount }
            )
        }
        return PaymentBreakdown(
            entries = entries,
            totalPaid = totalPaid(),
            changeDue = changeDue(),
            totalAmount = totalAmount()
        )
    }

    // --- Tax / SC / Tip mutations (DRAFT or OPEN only) ---

    fun applyTotals(
        taxLines: List<TaxLine>,
        serviceCharge: ServiceChargeLine?
    ): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN) {
            "Cannot apply totals in current status"
        }
        return copy(
            taxLines = taxLines,
            serviceCharge = serviceCharge,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun addTip(tipLine: TipLine): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN || status == SaleStatus.CONFIRMED) {
            "Cannot add tip in current status"
        }
        return copy(tip = tipLine, updatedAtMillis = System.currentTimeMillis())
    }

    fun removeTip(): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN || status == SaleStatus.CONFIRMED) {
            "Cannot remove tip in current status"
        }
        return copy(tip = null, updatedAtMillis = System.currentTimeMillis())
    }

    // --- Line mutations (DRAFT or OPEN) ---

    fun addLine(line: OrderLine): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN) {
            "Cannot add line in current status"
        }
        return copy(lines = lines + line, updatedAtMillis = System.currentTimeMillis())
    }

    fun updateLine(lineId: OrderLineId, updater: (OrderLine) -> OrderLine): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN) {
            "Cannot update line in current status"
        }
        require(lines.any { it.id == lineId }) { "Line not found: ${lineId.value}" }
        return copy(
            lines = lines.map { if (it.id == lineId) updater(it) else it },
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun removeLine(lineId: OrderLineId): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN) {
            "Cannot remove line in current status"
        }
        val line = lines.find { it.id == lineId }
            ?: throw IllegalArgumentException("Line not found: ${lineId.value}")
        require(!line.isSentToKitchen) {
            "Cannot remove line already sent to kitchen"
        }
        return copy(
            lines = lines.filter { it.id != lineId },
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    // --- Channel switch (DRAFT only, no items sent to kitchen) ---

    fun switchChannel(newChannelId: SalesChannelId, newOrderFlow: OrderFlowType): Sale {
        require(status == SaleStatus.DRAFT) {
            "Can only switch channel on draft sale"
        }
        require(lines.none { it.isSentToKitchen }) {
            "Cannot switch channel after items sent to kitchen"
        }
        return copy(
            channelId = newChannelId,
            orderFlow = newOrderFlow,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    // --- Order info mutations (DRAFT or OPEN) ---

    fun assignTable(tableId: TableId?): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN) {
            "Cannot change table in current status"
        }
        return copy(tableId = tableId, updatedAtMillis = System.currentTimeMillis())
    }

    fun setCustomerName(name: String?): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN) {
            "Cannot change customer name in current status"
        }
        val trimmed = name?.trim()?.takeIf { it.isNotBlank() }
        return copy(customerName = trimmed, updatedAtMillis = System.currentTimeMillis())
    }

    fun assignQueueNumber(queueNumber: String): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN || status == SaleStatus.CONFIRMED) {
            "Cannot assign queue number in current status"
        }
        return copy(queueNumber = queueNumber, updatedAtMillis = System.currentTimeMillis())
    }

    /** Display label for kitchen/receipt: table name, queue number, or customer name */
    fun orderLabel(): String? {
        return tableId?.let { "Meja ${it.value}" }
            ?: queueNumber
            ?: customerName
    }

    // --- Kitchen operations ---

    fun sendToKitchen(): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN) {
            "Cannot send to kitchen in current status"
        }
        require(lines.isNotEmpty()) { "Cannot send empty order to kitchen" }
        val unsentLines = lines.filter { !it.isSentToKitchen }
        require(unsentLines.isNotEmpty()) { "No new items to send to kitchen" }
        return copy(
            lines = lines.map { it.copy(isSentToKitchen = true) },
            status = SaleStatus.OPEN,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun unsentLines(): List<OrderLine> = lines.filter { !it.isSentToKitchen }

    // --- Payment ---

    fun addPayment(payment: Payment): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN || status == SaleStatus.CONFIRMED) {
            "Cannot add payment in current status"
        }
        // Non-cash payments cannot exceed remaining amount (only cash can overpay for change)
        if (payment.method != PaymentMethod.CASH) {
            val remaining = remainingAmount()
            require(payment.amount.amount <= remaining.amount) {
                "Pembayaran non-tunai tidak boleh melebihi sisa tagihan"
            }
        }
        // Update split bill paidAmount tracking if split is active
        val updatedSplit = if (splitBill != null && payment.payerIndex != null) {
            splitBill.updateEntry(payment.payerIndex) { entry ->
                entry.addPaid(payment.amount)
            }
        } else splitBill

        return copy(
            payments = payments + payment,
            splitBill = updatedSplit,
            status = if (isFullyPaidAfter(payment)) SaleStatus.PAID else status,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun removePayment(paymentId: PaymentId): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN || status == SaleStatus.CONFIRMED || status == SaleStatus.PAID) {
            "Cannot remove payment in current status"
        }
        val removed = payments.find { it.id == paymentId }
            ?: throw IllegalArgumentException("Payment not found: ${paymentId.value}")
        val newPayments = payments.filter { it.id != paymentId }

        // Reverse split bill paidAmount tracking
        val updatedSplit = if (splitBill != null && removed.payerIndex != null) {
            splitBill.updateEntry(removed.payerIndex) { entry ->
                val newPaid = (entry.paidAmount.amount - removed.amount.amount).coerceAtLeast(BigDecimal.ZERO)
                entry.copy(paidAmount = Money(newPaid, entry.paidAmount.currencyCode))
            }
        } else splitBill

        return copy(
            payments = newPayments,
            splitBill = updatedSplit,
            status = if (status == SaleStatus.PAID) SaleStatus.CONFIRMED else status,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    private fun isFullyPaidAfter(payment: Payment): Boolean {
        val newTotalPaid = totalPaid().amount.add(payment.amount.amount)
        return newTotalPaid.compareTo(totalAmount().amount) >= 0
    }

    // --- Split Bill ---

    fun initSplitEqual(payerCount: Int, labels: List<String>? = null): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN || status == SaleStatus.CONFIRMED) {
            "Cannot split bill in current status"
        }
        require(lines.isNotEmpty()) { "Tidak bisa split bill tanpa item" }
        require(payments.isEmpty()) { "Tidak bisa split bill jika sudah ada pembayaran" }
        return copy(
            splitBill = SplitBill.equal(totalAmount(), payerCount, labels),
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun initSplitByItem(
        assignments: Map<Int, List<OrderLineId>>,
        labels: List<String>? = null
    ): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN || status == SaleStatus.CONFIRMED) {
            "Cannot split bill in current status"
        }
        require(lines.isNotEmpty()) { "Tidak bisa split bill tanpa item" }
        require(payments.isEmpty()) { "Tidak bisa split bill jika sudah ada pembayaran" }
        return copy(
            splitBill = SplitBill.byItem(lines, assignments, totalAmount(), labels),
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun initSplitByAmount(amounts: List<Money>, labels: List<String>? = null): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN || status == SaleStatus.CONFIRMED) {
            "Cannot split bill in current status"
        }
        require(lines.isNotEmpty()) { "Tidak bisa split bill tanpa item" }
        require(payments.isEmpty()) { "Tidak bisa split bill jika sudah ada pembayaran" }
        return copy(
            splitBill = SplitBill.byAmount(amounts, totalAmount(), labels),
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun cancelSplit(): Sale {
        require(splitBill != null) { "Tidak ada split bill yang aktif" }
        require(payments.none { it.payerIndex != null }) {
            "Tidak bisa batal split, sudah ada pembayaran per tamu"
        }
        return copy(splitBill = null, updatedAtMillis = System.currentTimeMillis())
    }

    val isSplitBill: Boolean get() = splitBill != null

    fun paymentsForPayer(payerIndex: Int): List<Payment> =
        payments.filter { it.payerIndex == payerIndex }

    // --- Status transitions ---

    fun confirm(): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN) {
            "Can only confirm draft or open sale"
        }
        require(lines.isNotEmpty()) { "Cannot confirm sale with no lines" }
        return copy(status = SaleStatus.CONFIRMED, updatedAtMillis = System.currentTimeMillis())
    }

    fun complete(): Sale {
        require(status == SaleStatus.PAID) { "Can only complete paid sale" }
        return copy(status = SaleStatus.COMPLETED, updatedAtMillis = System.currentTimeMillis())
    }

    fun void(): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.OPEN || status == SaleStatus.CONFIRMED) {
            "Cannot void in current status"
        }
        return copy(status = SaleStatus.VOIDED, updatedAtMillis = System.currentTimeMillis())
    }
}

// --- Cashier session aggregate ---

enum class CashierSessionStatus { OPEN, CLOSED }

data class CashierSession(
    val id: CashierSessionId,
    val terminalId: TerminalId,
    val outletId: OutletId,
    val userId: UserId,
    val openAtMillis: Long,
    val closeAtMillis: Long? = null,
    val openingFloat: Money,
    val closingCash: Money? = null,
    val expectedCash: Money? = null,
    val notes: String? = null,
    val status: CashierSessionStatus = CashierSessionStatus.OPEN
) {
    fun cashDifference(): Money? {
        val closing = closingCash ?: return null
        val expected = expectedCash ?: return null
        return closing - expected
    }
}

// --- Table (F&B dine-in) ---

enum class TableStatus {
    AVAILABLE,
    OCCUPIED,
    RESERVED
}

data class Table(
    val id: TableId,
    val outletId: OutletId,
    val name: String,
    val capacity: Int = 4,
    val section: String? = null,
    val currentSaleId: SaleId? = null,
    val isActive: Boolean = true
) {
    val status: TableStatus get() = when {
        currentSaleId != null -> TableStatus.OCCUPIED
        else -> TableStatus.AVAILABLE
    }

    val isAvailable: Boolean get() = status == TableStatus.AVAILABLE

    fun occupy(saleId: SaleId): Table {
        require(isAvailable) { "Meja ${name} sedang digunakan" }
        return copy(currentSaleId = saleId)
    }

    fun release(): Table = copy(currentSaleId = null)

    fun transferTo(newSaleId: SaleId): Table = copy(currentSaleId = newSaleId)
}
