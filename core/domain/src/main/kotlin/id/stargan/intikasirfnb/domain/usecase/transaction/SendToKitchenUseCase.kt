package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.OrderLine
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository

data class KitchenTicketResult(
    val sale: Sale,
    val newLines: List<OrderLine>
)

class SendToKitchenUseCase(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(saleId: SaleId): Result<KitchenTicketResult> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val unsentLines = sale.unsentLines()
        require(unsentLines.isNotEmpty()) { "Tidak ada item baru untuk dikirim ke dapur" }
        val updated = sale.sendToKitchen()
        saleRepository.save(updated)
        KitchenTicketResult(sale = updated, newLines = unsentLines)
    }
}
