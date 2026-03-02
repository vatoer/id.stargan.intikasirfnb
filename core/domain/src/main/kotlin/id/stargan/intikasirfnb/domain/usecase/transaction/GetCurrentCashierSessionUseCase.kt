package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.transaction.CashierSession
import id.stargan.intikasirfnb.domain.transaction.CashierSessionRepository

class GetCurrentCashierSessionUseCase(private val cashierSessionRepository: CashierSessionRepository) {
    suspend operator fun invoke(outletId: OutletId, terminalId: TerminalId): CashierSession? =
        cashierSessionRepository.getCurrentSession(outletId, terminalId)
}
