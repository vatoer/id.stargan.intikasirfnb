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
    CONFIRMED,
    PAID,
    COMPLETED,
    VOIDED
}

enum class PaymentMethod { CASH, CARD, E_WALLET, TRANSFER, OTHER }

// --- Selected modifier (snapshot of chosen modifiers at time of order) ---

data class SelectedModifier(
    val groupName: String,
    val optionName: String,
    val priceDelta: Money = Money.zero()
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
    val notes: String? = null
) {
    fun modifierTotal(): Money =
        selectedModifiers.fold(Money.zero()) { acc, m -> acc + m.priceDelta }

    fun effectiveUnitPrice(): Money = unitPrice + modifierTotal()

    fun lineTotal(): Money = (effectiveUnitPrice() * quantity) - discountAmount
}

// --- Payment (within Sale aggregate) ---

data class Payment(
    val id: PaymentId = PaymentId.generate(),
    val method: PaymentMethod,
    val amount: Money,
    val reference: String? = null
)

// --- Sale (Order) aggregate root ---

data class Sale(
    val id: SaleId,
    val outletId: OutletId,
    val channelId: SalesChannelId,
    val receiptNumber: String? = null,
    val tableId: TableId? = null,
    val externalOrderId: String? = null,
    val cashierId: UserId? = null,
    val customerId: CustomerId? = null,
    val lines: List<OrderLine> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val taxLines: List<TaxLine> = emptyList(),
    val serviceCharge: ServiceChargeLine? = null,
    val tip: TipLine? = null,
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

    // --- Tax / SC / Tip mutations (DRAFT only) ---

    fun applyTotals(
        taxLines: List<TaxLine>,
        serviceCharge: ServiceChargeLine?
    ): Sale {
        require(status == SaleStatus.DRAFT) { "Cannot apply totals to non-draft sale" }
        return copy(
            taxLines = taxLines,
            serviceCharge = serviceCharge,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun addTip(tipLine: TipLine): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.CONFIRMED) {
            "Cannot add tip in current status"
        }
        return copy(tip = tipLine, updatedAtMillis = System.currentTimeMillis())
    }

    fun removeTip(): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.CONFIRMED) {
            "Cannot remove tip in current status"
        }
        return copy(tip = null, updatedAtMillis = System.currentTimeMillis())
    }

    // --- Line mutations (DRAFT only) ---

    fun addLine(line: OrderLine): Sale {
        require(status == SaleStatus.DRAFT) { "Cannot add line to non-draft sale" }
        return copy(lines = lines + line, updatedAtMillis = System.currentTimeMillis())
    }

    fun updateLine(lineId: OrderLineId, updater: (OrderLine) -> OrderLine): Sale {
        require(status == SaleStatus.DRAFT) { "Cannot update line on non-draft sale" }
        require(lines.any { it.id == lineId }) { "Line not found: ${lineId.value}" }
        return copy(
            lines = lines.map { if (it.id == lineId) updater(it) else it },
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun removeLine(lineId: OrderLineId): Sale {
        require(status == SaleStatus.DRAFT) { "Cannot remove line from non-draft sale" }
        require(lines.any { it.id == lineId }) { "Line not found: ${lineId.value}" }
        return copy(
            lines = lines.filter { it.id != lineId },
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    // --- Payment ---

    fun addPayment(payment: Payment): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.CONFIRMED) {
            "Cannot add payment in current status"
        }
        return copy(
            payments = payments + payment,
            status = if (isFullyPaidAfter(payment)) SaleStatus.PAID else status,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    private fun isFullyPaidAfter(payment: Payment): Boolean {
        val newTotalPaid = totalPaid().amount.add(payment.amount.amount)
        return newTotalPaid.compareTo(totalAmount().amount) >= 0
    }

    // --- Status transitions ---

    fun confirm(): Sale {
        require(status == SaleStatus.DRAFT) { "Can only confirm draft sale" }
        require(lines.isNotEmpty()) { "Cannot confirm sale with no lines" }
        return copy(status = SaleStatus.CONFIRMED, updatedAtMillis = System.currentTimeMillis())
    }

    fun complete(): Sale {
        require(status == SaleStatus.PAID) { "Can only complete paid sale" }
        return copy(status = SaleStatus.COMPLETED, updatedAtMillis = System.currentTimeMillis())
    }

    fun void(): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.CONFIRMED) {
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

data class Table(
    val id: TableId,
    val outletId: OutletId,
    val name: String,
    val capacity: Int = 4,
    val currentSaleId: SaleId? = null,
    val isActive: Boolean = true
)
