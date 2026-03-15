package id.stargan.intikasirfnb.domain.accounting

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.shared.UlidGenerator
import java.math.BigDecimal

@JvmInline
value class AccountId(val value: String) {
    companion object {
        fun generate() = AccountId(UlidGenerator.generate())
    }
}

@JvmInline
value class JournalId(val value: String) {
    companion object {
        fun generate() = JournalId(UlidGenerator.generate())
    }
}

data class Account(
    val id: AccountId,
    val code: String,
    val name: String,
    val type: AccountType
)

enum class AccountType { ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE }

data class JournalEntry(
    val accountId: AccountId,
    val accountName: String = "",
    val debit: Money = Money.zero(),
    val credit: Money = Money.zero(),
    val description: String? = null
) {
    init {
        require(
            (debit.amount > BigDecimal.ZERO && credit.isZero()) ||
                (debit.isZero() && credit.amount > BigDecimal.ZERO)
        ) {
            "Each entry must have either debit or credit (not both, not both zero)"
        }
    }
}

data class Journal(
    val id: JournalId = JournalId.generate(),
    val outletId: OutletId,
    val entries: List<JournalEntry>,
    val description: String? = null,
    val referenceType: String? = null,  // "SALE", "PURCHASE", etc.
    val referenceId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val totalDebit: Money get() = entries.fold(Money.zero()) { acc, e -> acc + e.debit }
    val totalCredit: Money get() = entries.fold(Money.zero()) { acc, e -> acc + e.credit }
    val isBalanced: Boolean get() = totalDebit.amount.compareTo(totalCredit.amount) == 0
}

/**
 * Daily sales summary for simple P&L view.
 */
data class DailySummary(
    val date: String,  // "2026-03-15"
    val totalSales: Money,
    val totalTax: Money,
    val totalServiceCharge: Money,
    val totalDiscount: Money,
    val netSales: Money,  // totalSales - totalDiscount
    val transactionCount: Int
)
