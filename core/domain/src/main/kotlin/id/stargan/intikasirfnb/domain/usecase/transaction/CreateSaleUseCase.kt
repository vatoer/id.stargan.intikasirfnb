package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.UserId
import id.stargan.intikasirfnb.domain.transaction.OrderChannel
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.TableId

/**
 * Membuat sale baru. Untuk DINE_IN wajib tableId; untuk OJOL_A/OJOL_B wajib externalOrderId.
 */
class CreateSaleUseCase(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(
        outletId: OutletId,
        channel: OrderChannel,
        tableId: TableId? = null,
        externalOrderId: String? = null,
        cashierId: UserId? = null
    ): Result<Sale> = runCatching {
        require(channel != OrderChannel.DINE_IN || tableId != null) {
            "Table is required for Dine In"
        }
        require(
            (channel != OrderChannel.OJOL_A && channel != OrderChannel.OJOL_B) ||
                (externalOrderId != null && externalOrderId.isNotBlank())
        ) {
            "External order ID is required for Ojol"
        }
        val sale = Sale(
            id = SaleId.generate(),
            outletId = outletId,
            channel = channel,
            tableId = tableId,
            externalOrderId = externalOrderId,
            cashierId = cashierId
        )
        saleRepository.save(sale)
        sale
    }
}
