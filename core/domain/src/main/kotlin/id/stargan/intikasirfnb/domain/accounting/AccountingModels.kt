package id.stargan.intikasirfnb.domain.accounting

import id.stargan.intikasirfnb.domain.shared.Money
import java.util.UUID

@JvmInline
value class AccountId(val value: String) {
    companion object {
        fun generate() = AccountId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class JournalId(val value: String) {
    companion object {
        fun generate() = JournalId(UUID.randomUUID().toString())
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
    val debit: Money,
    val credit: Money,
    val description: String? = null,
    val referenceType: String? = null,
    val referenceId: String? = null
) {
    init {
        require(
            (debit.amount > java.math.BigDecimal.ZERO && credit.isZero()) ||
                (debit.isZero() && credit.amount > java.math.BigDecimal.ZERO)
        ) {
            "Each entry must have either debit or credit (not both, not both zero)"
        }
    }
}

data class Journal(
    val id: JournalId,
    val entries: List<JournalEntry>,
    val description: String? = null,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
