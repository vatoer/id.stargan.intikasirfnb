package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.PaymentId
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository

class RemovePaymentUseCase(private val saleRepository: SaleRepository) {
    suspend operator fun invoke(saleId: SaleId, paymentId: PaymentId): Result<Sale> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val updated = sale.removePayment(paymentId)
        saleRepository.save(updated)
        updated
    }
}
