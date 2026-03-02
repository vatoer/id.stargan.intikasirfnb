package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.transaction.CashierSession
import id.stargan.intikasirfnb.domain.transaction.CashierSessionRepository
import id.stargan.intikasirfnb.domain.transaction.CashierSessionStatus

class CloseCashierSessionUseCase(private val cashierSessionRepository: CashierSessionRepository) {
    suspend operator fun invoke(outletId: OutletId, terminalId: TerminalId): Result<CashierSession> = runCatching {
        val session = cashierSessionRepository.getCurrentSession(outletId, terminalId)
            ?: error("No open session for this terminal")
        val closed = session.copy(
            closeAtMillis = System.currentTimeMillis(),
            status = CashierSessionStatus.CLOSED
        )
        cashierSessionRepository.save(closed)
        closed
    }
}
