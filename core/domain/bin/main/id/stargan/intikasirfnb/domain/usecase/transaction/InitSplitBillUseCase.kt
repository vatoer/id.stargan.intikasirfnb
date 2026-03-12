package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.OrderLineId
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.SplitType

class InitSplitBillUseCase(private val saleRepository: SaleRepository) {

    /** Split equal: total dibagi rata ke N payers */
    suspend fun equal(
        saleId: SaleId,
        payerCount: Int,
        labels: List<String>? = null
    ): Result<Sale> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val updated = sale.initSplitEqual(payerCount, labels)
        saleRepository.save(updated)
        updated
    }

    /** Split by item: tiap payer di-assign item tertentu */
    suspend fun byItem(
        saleId: SaleId,
        assignments: Map<Int, List<OrderLineId>>,
        labels: List<String>? = null
    ): Result<Sale> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val updated = sale.initSplitByItem(assignments, labels)
        saleRepository.save(updated)
        updated
    }

    /** Split by amount: nominal custom per payer */
    suspend fun byAmount(
        saleId: SaleId,
        amounts: List<Money>,
        labels: List<String>? = null
    ): Result<Sale> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val updated = sale.initSplitByAmount(amounts, labels)
        saleRepository.save(updated)
        updated
    }
}
