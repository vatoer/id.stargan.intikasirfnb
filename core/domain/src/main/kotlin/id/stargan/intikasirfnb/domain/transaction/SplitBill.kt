package id.stargan.intikasirfnb.domain.transaction

import id.stargan.intikasirfnb.domain.shared.Money
import java.math.BigDecimal
import java.math.RoundingMode

// --- Split Bill Types ---

enum class SplitType {
    EQUAL,      // Total / N payers (rounding to last payer)
    BY_ITEM,    // Each payer assigned specific line items
    BY_AMOUNT   // Custom amount per payer
}

// --- Split Bill Entry (one per payer) ---

data class SplitBillEntry(
    val payerIndex: Int,
    val label: String,                          // "Tamu 1", "Pak Budi", etc.
    val lineIds: List<OrderLineId> = emptyList(),  // For BY_ITEM: assigned lines
    val shareAmount: Money,                     // What this payer owes
    val paidAmount: Money = Money.zero()        // How much already paid
) {
    val remaining: Money get() {
        val diff = shareAmount - paidAmount
        return if (diff.amount > BigDecimal.ZERO) diff else Money.zero()
    }

    val isFullyPaid: Boolean get() =
        paidAmount.amount >= shareAmount.amount && shareAmount.amount > BigDecimal.ZERO

    fun addPaid(amount: Money): SplitBillEntry = copy(
        paidAmount = Money(paidAmount.amount + amount.amount, paidAmount.currencyCode)
    )
}

// --- Split Bill aggregate (embedded in Sale) ---

data class SplitBill(
    val type: SplitType,
    val entries: List<SplitBillEntry>
) {
    val payerCount: Int get() = entries.size

    val totalShare: Money get() = entries.fold(Money.zero()) { acc, e -> acc + e.shareAmount }

    val totalPaid: Money get() = entries.fold(Money.zero()) { acc, e -> acc + e.paidAmount }

    val isFullyPaid: Boolean get() = entries.all { it.isFullyPaid }

    fun entryFor(payerIndex: Int): SplitBillEntry? = entries.find { it.payerIndex == payerIndex }

    fun updateEntry(payerIndex: Int, updater: (SplitBillEntry) -> SplitBillEntry): SplitBill {
        return copy(entries = entries.map { if (it.payerIndex == payerIndex) updater(it) else it })
    }

    companion object {
        /**
         * Split total equally among N payers.
         * Last payer absorbs rounding remainder.
         */
        fun equal(totalAmount: Money, payerCount: Int, labels: List<String>? = null): SplitBill {
            require(payerCount >= 2) { "Split membutuhkan minimal 2 pembayar" }
            val perPayer = totalAmount.amount
                .divide(BigDecimal(payerCount), 0, RoundingMode.DOWN)
            val perPayerMoney = Money(perPayer, totalAmount.currencyCode)
            val remainder = Money(
                totalAmount.amount - perPayer.multiply(BigDecimal(payerCount)),
                totalAmount.currencyCode
            )

            val entries = (0 until payerCount).map { i ->
                val label = labels?.getOrNull(i) ?: "Tamu ${i + 1}"
                val share = if (i == payerCount - 1) perPayerMoney + remainder else perPayerMoney
                SplitBillEntry(
                    payerIndex = i,
                    label = label,
                    shareAmount = share
                )
            }
            return SplitBill(type = SplitType.EQUAL, entries = entries)
        }

        /**
         * Split by item assignment.
         * Each payer gets the sum of their assigned line totals.
         * Shared costs (tax, SC, tip) are distributed proportionally.
         */
        fun byItem(
            lines: List<OrderLine>,
            assignments: Map<Int, List<OrderLineId>>,
            totalAmount: Money,
            labels: List<String>? = null
        ): SplitBill {
            require(assignments.size >= 2) { "Split membutuhkan minimal 2 pembayar" }

            // Validate all lines are assigned
            val allAssigned = assignments.values.flatten().toSet()
            val allLineIds = lines.map { it.id }.toSet()
            val unassigned = allLineIds - allAssigned
            require(unassigned.isEmpty()) {
                "Semua item harus di-assign ke pembayar (${unassigned.size} item belum di-assign)"
            }

            val subtotal = lines.fold(Money.zero()) { acc, l -> acc + l.lineTotal() }
            val surchargeRatio = if (subtotal.amount > BigDecimal.ZERO) {
                totalAmount.amount.divide(subtotal.amount, 6, RoundingMode.HALF_UP)
            } else BigDecimal.ONE

            val entries = assignments.entries.sortedBy { it.key }.mapIndexed { _, (payerIndex, lineIds) ->
                val payerSubtotal = lines
                    .filter { it.id in lineIds }
                    .fold(Money.zero()) { acc, l -> acc + l.lineTotal() }
                // Proportional share of total (includes tax/SC/tip)
                val share = Money(
                    payerSubtotal.amount.multiply(surchargeRatio).setScale(0, RoundingMode.HALF_UP),
                    totalAmount.currencyCode
                )
                val label = labels?.getOrNull(payerIndex) ?: "Tamu ${payerIndex + 1}"
                SplitBillEntry(
                    payerIndex = payerIndex,
                    label = label,
                    lineIds = lineIds,
                    shareAmount = share
                )
            }

            // Adjust last entry to absorb rounding
            val sumShares = entries.fold(Money.zero()) { acc, e -> acc + e.shareAmount }
            val diff = totalAmount.amount - sumShares.amount
            val adjusted = if (diff.compareTo(BigDecimal.ZERO) != 0 && entries.isNotEmpty()) {
                val last = entries.last()
                entries.dropLast(1) + last.copy(
                    shareAmount = Money(last.shareAmount.amount + diff, totalAmount.currencyCode)
                )
            } else entries

            return SplitBill(type = SplitType.BY_ITEM, entries = adjusted)
        }

        /**
         * Split by custom amounts per payer.
         * Total of all amounts must equal sale total.
         */
        fun byAmount(
            amounts: List<Money>,
            totalAmount: Money,
            labels: List<String>? = null
        ): SplitBill {
            require(amounts.size >= 2) { "Split membutuhkan minimal 2 pembayar" }
            val sum = amounts.fold(Money.zero()) { acc, m -> acc + m }
            require(sum.amount.compareTo(totalAmount.amount) == 0) {
                "Total split (${sum.amount}) harus sama dengan total bill (${totalAmount.amount})"
            }

            val entries = amounts.mapIndexed { i, amount ->
                val label = labels?.getOrNull(i) ?: "Tamu ${i + 1}"
                SplitBillEntry(
                    payerIndex = i,
                    label = label,
                    shareAmount = amount
                )
            }
            return SplitBill(type = SplitType.BY_AMOUNT, entries = entries)
        }
    }
}
