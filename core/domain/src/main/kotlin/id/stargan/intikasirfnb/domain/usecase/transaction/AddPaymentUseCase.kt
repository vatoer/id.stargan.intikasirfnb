package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.Payment
import id.stargan.intikasirfnb.domain.transaction.PaymentMethod
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository

class AddPaymentUseCase(private val saleRepository: SaleRepository) {
    suspend operator fun invoke(
        saleId: SaleId,
        method: PaymentMethod,
        amount: Money,
        reference: String? = null
    ): Result<Sale> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val payment = Payment(method = method, amount = amount, reference = reference)
        val updated = sale.addPayment(payment)
        saleRepository.save(updated)
        updated
    }
}
