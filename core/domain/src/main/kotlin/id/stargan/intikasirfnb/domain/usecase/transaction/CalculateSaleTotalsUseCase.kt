package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.settings.OutletSettingsRepository
import id.stargan.intikasirfnb.domain.settings.TaxConfigRepository
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.ServiceChargeLine
import id.stargan.intikasirfnb.domain.transaction.TaxLine

class CalculateSaleTotalsUseCase(
    private val taxConfigRepository: TaxConfigRepository,
    private val outletSettingsRepository: OutletSettingsRepository
) {
    suspend operator fun invoke(sale: Sale, tenantId: TenantId): Result<Sale> = runCatching {
        val subtotal = sale.subtotal()

        // Load active tax configs and compute tax lines
        val activeTaxes = taxConfigRepository.getActiveByTenant(tenantId)
        val taxLines = activeTaxes.map { tax ->
            TaxLine.compute(
                taxName = tax.name,
                taxRate = tax.rate,
                isIncludedInPrice = tax.isIncludedInPrice,
                taxableAmount = subtotal
            )
        }

        // Load outlet settings for service charge
        val outletSettings = outletSettingsRepository.getByOutletId(sale.outletId)
        val sc = outletSettings?.serviceCharge
        val serviceCharge = if (sc != null && sc.isEnabled) {
            ServiceChargeLine.compute(
                rate = sc.rate,
                isIncludedInPrice = sc.isIncludedInPrice,
                baseAmount = subtotal
            )
        } else null

        sale.applyTotals(taxLines = taxLines, serviceCharge = serviceCharge)
    }
}
