package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import kotlinx.coroutines.flow.Flow

class GetSalesByOutletUseCase(
    private val saleRepository: SaleRepository
) {
    fun stream(outletId: OutletId): Flow<List<Sale>> =
        saleRepository.streamByOutlet(outletId)

    suspend operator fun invoke(outletId: OutletId, limit: Int = 100): List<Sale> =
        saleRepository.listByOutlet(outletId, limit)
}
