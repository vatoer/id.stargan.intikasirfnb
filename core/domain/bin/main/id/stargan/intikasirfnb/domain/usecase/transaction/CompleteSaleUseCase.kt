package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.PlatformSettlement
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementId
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementRepository
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.TableRepository

class CompleteSaleUseCase(
    private val saleRepository: SaleRepository,
    private val settlementRepository: PlatformSettlementRepository? = null,
    private val tableRepository: TableRepository? = null
) {
    suspend operator fun invoke(saleId: SaleId): Result<Sale> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val updated = sale.complete()
        saleRepository.save(updated)

        // Release table if dine-in
        if (updated.tableId != null && tableRepository != null) {
            tableRepository.releaseBySaleId(updated.id)
        }

        // Auto-create platform settlement if this is a platform order
        if (updated.platformPayment != null && settlementRepository != null) {
            val pp = updated.platformPayment
            val settlement = PlatformSettlement(
                id = PlatformSettlementId.generate(),
                outletId = updated.outletId,
                channelId = updated.channelId,
                platformName = pp.platformName,
                saleIds = listOf(updated.id),
                expectedAmount = pp.netAmount,
                commissionTotal = pp.commissionAmount
            )
            settlementRepository.save(settlement)
        }

        updated
    }
}
