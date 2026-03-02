package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.identity.UserId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.CashierSession
import id.stargan.intikasirfnb.domain.transaction.CashierSessionRepository
import id.stargan.intikasirfnb.domain.transaction.CashierSessionStatus

class OpenCashierSessionUseCase(private val cashierSessionRepository: CashierSessionRepository) {
    suspend operator fun invoke(
        terminalId: TerminalId,
        outletId: OutletId,
        userId: UserId,
        openingFloat: Money
    ): Result<CashierSession> = runCatching {
        require(cashierSessionRepository.getCurrentSession(outletId, terminalId) == null) {
            "Session already open for this terminal"
        }
        val session = CashierSession(
            id = terminalId,
            outletId = outletId,
            userId = userId,
            openAtMillis = System.currentTimeMillis(),
            openingFloat = openingFloat,
            status = CashierSessionStatus.OPEN
        )
        cashierSessionRepository.save(session)
        session
    }
}
