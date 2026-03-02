package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository

class GetSaleByIdUseCase(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(saleId: SaleId): Sale? = saleRepository.getById(saleId)
}
