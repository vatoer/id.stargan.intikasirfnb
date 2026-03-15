package id.stargan.intikasirfnb.domain.usecase.accounting

import id.stargan.intikasirfnb.domain.accounting.AccountId
import id.stargan.intikasirfnb.domain.accounting.Journal
import id.stargan.intikasirfnb.domain.accounting.JournalEntry
import id.stargan.intikasirfnb.domain.accounting.JournalRepository
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.Sale

/**
 * Record journal entries when a sale is completed.
 * Double-entry: Debit Cash/Receivable, Credit Revenue.
 * Tax and service charge recorded as separate liability entries.
 */
class RecordSaleJournalUseCase(
    private val journalRepository: JournalRepository
) {
    // Well-known account IDs (pre-seeded)
    companion object {
        val CASH_ACCOUNT = AccountId("ACCT-CASH")
        val RECEIVABLE_ACCOUNT = AccountId("ACCT-RECEIVABLE")
        val REVENUE_ACCOUNT = AccountId("ACCT-REVENUE")
        val TAX_PAYABLE_ACCOUNT = AccountId("ACCT-TAX-PAYABLE")
        val SC_REVENUE_ACCOUNT = AccountId("ACCT-SC-REVENUE")
    }

    suspend operator fun invoke(sale: Sale): Result<Journal> = runCatching {
        val entries = mutableListOf<JournalEntry>()
        val total = sale.totalAmount()

        // Debit: Cash or Receivable
        val hasPlatformPayment = sale.payments.any { it.method.isPlatformSettlement }
        if (hasPlatformPayment) {
            val cashAmount = sale.payments.filter { !it.method.isPlatformSettlement }
                .fold(Money.zero()) { acc, p -> acc + p.amount }
            val platformAmount = sale.payments.filter { it.method.isPlatformSettlement }
                .fold(Money.zero()) { acc, p -> acc + p.amount }
            if (cashAmount.isPositive()) {
                entries.add(JournalEntry(CASH_ACCOUNT, "Kas", debit = cashAmount))
            }
            if (platformAmount.isPositive()) {
                entries.add(JournalEntry(RECEIVABLE_ACCOUNT, "Piutang Platform", debit = platformAmount))
            }
        } else {
            entries.add(JournalEntry(CASH_ACCOUNT, "Kas", debit = total))
        }

        // Credit: Revenue (net of tax + SC)
        val netRevenue = sale.subtotal()
        entries.add(JournalEntry(REVENUE_ACCOUNT, "Pendapatan", credit = netRevenue))

        // Credit: Tax payable
        val taxTotal = sale.taxTotal()
        if (taxTotal.isPositive()) {
            entries.add(JournalEntry(TAX_PAYABLE_ACCOUNT, "Utang Pajak", credit = taxTotal))
        }

        // Credit: Service charge
        val scAmount = sale.serviceChargeAmount()
        if (scAmount.isPositive()) {
            entries.add(JournalEntry(SC_REVENUE_ACCOUNT, "Service Charge", credit = scAmount))
        }

        val journal = Journal(
            outletId = sale.outletId,
            entries = entries,
            description = "Penjualan ${sale.receiptNumber ?: sale.id.value}",
            referenceType = "SALE",
            referenceId = sale.id.value
        )
        journalRepository.save(journal)
        journal
    }
}
