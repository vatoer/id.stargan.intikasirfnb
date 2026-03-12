package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.UserId
import id.stargan.intikasirfnb.domain.transaction.OrderFlowType
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.SalesChannelId
import id.stargan.intikasirfnb.domain.transaction.SalesChannelRepository
import id.stargan.intikasirfnb.domain.transaction.TableId
import id.stargan.intikasirfnb.domain.transaction.TableRepository

class CreateSaleUseCase(
    private val saleRepository: SaleRepository,
    private val salesChannelRepository: SalesChannelRepository,
    private val tableRepository: TableRepository? = null
) {
    suspend operator fun invoke(
        outletId: OutletId,
        channelId: SalesChannelId,
        tableId: TableId? = null,
        externalOrderId: String? = null,
        cashierId: UserId? = null,
        orderFlowOverride: OrderFlowType? = null
    ): Result<Sale> = runCatching {
        val channel = salesChannelRepository.getById(channelId)
            ?: error("Sales channel not found")
        require(channel.isActive) { "Sales channel is inactive" }
        require(!channel.requiresTable || tableId != null) {
            "Table is required for ${channel.name}"
        }
        require(!channel.requiresExternalOrderId || !externalOrderId.isNullOrBlank()) {
            "External order ID is required for ${channel.name}"
        }

        // Validate and occupy table if provided
        if (tableId != null && tableRepository != null) {
            val table = tableRepository.getById(tableId)
                ?: error("Meja tidak ditemukan")
            require(table.isAvailable) { "Meja ${table.name} sedang digunakan" }
        }

        val sale = Sale(
            id = SaleId.generate(),
            outletId = outletId,
            channelId = channelId,
            orderFlow = orderFlowOverride ?: channel.defaultOrderFlow,
            tableId = tableId,
            externalOrderId = externalOrderId,
            cashierId = cashierId
        )
        saleRepository.save(sale)

        // Occupy table after sale is saved
        if (tableId != null && tableRepository != null) {
            tableRepository.occupyTable(tableId, sale.id)
        }

        sale
    }
}
