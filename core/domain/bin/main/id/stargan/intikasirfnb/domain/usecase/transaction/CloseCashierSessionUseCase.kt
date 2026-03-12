package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.CashierSession
import id.stargan.intikasirfnb.domain.transaction.CashierSessionRepository
import id.stargan.intikasirfnb.domain.transaction.CashierSessionStatus

class CloseCashierSessionUseCase(private val cashierSessionRepository: CashierSessionRepository) {
    suspend operator fun invoke(
        outletId: OutletId,
        terminalId: TerminalId,
        closingCash: Money,
        expectedCash: Money,
        notes: String? = null
    ): Result<CashierSession> = runCatching {
        val session = cashierSessionRepository.getCurrentSession(outletId, terminalId)
            ?: error("No open session for this terminal")
        val closed = session.copy(
            closeAtMillis = System.currentTimeMillis(),
            closingCash = closingCash,
            expectedCash = expectedCash,
            notes = notes,
            status = CashierSessionStatus.CLOSED
        )
        cashierSessionRepository.save(closed)
        closed
    }
}
