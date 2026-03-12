package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import kotlinx.coroutines.flow.Flow

class GetOpenSalesUseCase(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(outletId: OutletId): List<Sale> =
        saleRepository.listOpenByOutlet(outletId)

    fun stream(outletId: OutletId): Flow<List<Sale>> =
        saleRepository.streamOpenByOutlet(outletId)
}
