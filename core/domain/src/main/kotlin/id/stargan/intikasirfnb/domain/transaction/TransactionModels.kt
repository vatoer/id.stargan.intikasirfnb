package id.stargan.intikasirfnb.domain.transaction

import id.stargan.intikasirfnb.domain.catalog.ProductRef
import id.stargan.intikasirfnb.domain.customer.CustomerId
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.identity.UserId
import id.stargan.intikasirfnb.domain.shared.Money
import java.util.UUID

// --- Value objects ---

@JvmInline
value class SaleId(val value: String) {
    companion object {
        fun generate() = SaleId(UUID.randomUUID().toString())
    }
}

enum class OrderChannel {
    DINE_IN,
    TAKE_AWAY,
    OJOL_A,
    OJOL_B
}

@JvmInline
value class TableId(val value: String) {
    companion object {
        fun generate() = TableId(UUID.randomUUID().toString())
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

// --- Order line (within Sale aggregate) ---

data class OrderLine(
    val productRef: ProductRef,
    val quantity: Int,
    val unitPrice: Money,
    val discountAmount: Money = Money.zero(),
    val modifierSnapshot: String? = null
) {
    fun lineTotal(): Money = (unitPrice * quantity) - discountAmount
}

// --- Payment (within Sale aggregate) ---

data class Payment(
    val method: PaymentMethod,
    val amount: Money,
    val reference: String? = null
)

// --- Sale (Order) aggregate root ---

data class Sale(
    val id: SaleId,
    val outletId: OutletId,
    val channel: OrderChannel,
    val tableId: TableId? = null,
    val externalOrderId: String? = null,
    val cashierId: UserId? = null,
    val customerId: CustomerId? = null,
    val lines: List<OrderLine> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val status: SaleStatus = SaleStatus.DRAFT,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun totalAmount(): Money = lines.fold(Money.zero(unitCurrency())) { acc, line -> acc + line.lineTotal() }

    fun totalPaid(): Money = payments.fold(Money.zero(unitCurrency())) { acc, p -> acc + p.amount }

    fun isFullyPaid(): Boolean = totalPaid().amount >= totalAmount().amount && totalAmount().amount > java.math.BigDecimal.ZERO

    private fun unitCurrency(): String = lines.firstOrNull()?.unitPrice?.currencyCode ?: "IDR"

    fun addLine(line: OrderLine): Sale {
        require(status == SaleStatus.DRAFT) { "Cannot add line to non-draft sale" }
        return copy(
            lines = lines + line,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun addPayment(payment: Payment): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.CONFIRMED) { "Cannot add payment in current status" }
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

    fun confirm(): Sale {
        require(status == SaleStatus.DRAFT) { "Can only confirm draft sale" }
        return copy(status = SaleStatus.CONFIRMED, updatedAtMillis = System.currentTimeMillis())
    }

    fun complete(): Sale {
        require(status == SaleStatus.PAID) { "Can only complete paid sale" }
        return copy(status = SaleStatus.COMPLETED, updatedAtMillis = System.currentTimeMillis())
    }

    fun void(): Sale {
        require(status == SaleStatus.DRAFT || status == SaleStatus.CONFIRMED) { "Cannot void in current status" }
        return copy(status = SaleStatus.VOIDED, updatedAtMillis = System.currentTimeMillis())
    }
}

// --- Cashier session aggregate ---

enum class CashierSessionStatus { OPEN, CLOSED }

data class CashierSession(
    val id: TerminalId,
    val outletId: OutletId,
    val userId: UserId,
    val openAtMillis: Long,
    val closeAtMillis: Long? = null,
    val openingFloat: Money,
    val status: CashierSessionStatus = CashierSessionStatus.OPEN
)

// --- Table (F&B dine-in) ---

data class Table(
    val id: TableId,
    val outletId: OutletId,
    val name: String,
    val capacity: Int = 4,
    val currentSaleId: SaleId? = null,
    val isActive: Boolean = true
)
