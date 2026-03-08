package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.settings.OutletSettingsRepository
import id.stargan.intikasirfnb.domain.settings.TaxConfigRepository
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.ServiceChargeLine
import id.stargan.intikasirfnb.domain.transaction.TaxLine

class ConfirmSaleUseCase(
    private val saleRepository: SaleRepository,
    private val taxConfigRepository: TaxConfigRepository,
    private val outletSettingsRepository: OutletSettingsRepository
) {
    suspend operator fun invoke(saleId: SaleId, tenantId: TenantId): Result<Sale> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")

        // Calculate tax and service charge snapshot before confirming
        val subtotal = sale.subtotal()

        val activeTaxes = taxConfigRepository.getActiveByTenant(tenantId)
        val taxLines = activeTaxes.map { tax ->
            TaxLine.compute(
                taxName = tax.name,
                taxRate = tax.rate,
                isIncludedInPrice = tax.isIncludedInPrice,
                taxableAmount = subtotal
            )
        }

        val outletSettings = outletSettingsRepository.getByOutletId(sale.outletId)
        val sc = outletSettings?.serviceCharge
        val serviceCharge = if (sc != null && sc.isEnabled) {
            ServiceChargeLine.compute(
                rate = sc.rate,
                isIncludedInPrice = sc.isIncludedInPrice,
                baseAmount = subtotal
            )
        } else null

        // applyTotals and confirm both accept DRAFT and OPEN status
        val updated = sale.applyTotals(taxLines = taxLines, serviceCharge = serviceCharge).confirm()
        saleRepository.save(updated)
        updated
    }
}
