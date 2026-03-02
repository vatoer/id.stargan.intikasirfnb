package id.stargan.intikasirfnb.data.mapper

import id.stargan.intikasirfnb.data.local.entity.OutletSettingsEntity
import id.stargan.intikasirfnb.data.local.entity.TenantSettingsEntity
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.settings.NumberingSequenceConfig
import id.stargan.intikasirfnb.domain.settings.OutletSettings
import id.stargan.intikasirfnb.domain.settings.TenantSettings
import id.stargan.intikasirfnb.domain.settings.TaxRate
import java.math.BigDecimal

private fun parseTaxRates(json: String): List<TaxRate> {
    if (json.isBlank() || json == "[]") return emptyList()
    return emptyList()
}

fun TenantSettingsEntity.toDomain(): TenantSettings = TenantSettings(
    tenantId = TenantId(tenantId),
    defaultCurrencyCode = defaultCurrencyCode,
    taxRates = parseTaxRates(taxRatesJson),
    receiptNumbering = receiptNumberingPrefix?.let {
        NumberingSequenceConfig(prefix = it, nextNumber = receiptNumberingNext)
    },
    invoiceNumbering = invoiceNumberingPrefix?.let {
        NumberingSequenceConfig(prefix = it, nextNumber = invoiceNumberingNext)
    }
)

fun TenantSettings.toEntity(): TenantSettingsEntity = TenantSettingsEntity(
    tenantId = tenantId.value,
    defaultCurrencyCode = defaultCurrencyCode,
    taxRatesJson = "[]",
    receiptNumberingPrefix = receiptNumbering?.prefix,
    receiptNumberingNext = receiptNumbering?.nextNumber ?: 1L,
    invoiceNumberingPrefix = invoiceNumbering?.prefix,
    invoiceNumberingNext = invoiceNumbering?.nextNumber ?: 1L
)

fun OutletSettingsEntity.toDomain(): OutletSettings = OutletSettings(
    outletId = OutletId(outletId),
    tenantId = TenantId(tenantId),
    timeZoneId = timeZoneId,
    receiptHeaderText = receiptHeaderText,
    receiptFooterText = receiptFooterText
)

fun OutletSettings.toEntity(): OutletSettingsEntity = OutletSettingsEntity(
    outletId = outletId.value,
    tenantId = tenantId.value,
    timeZoneId = timeZoneId,
    receiptHeaderText = receiptHeaderText,
    receiptFooterText = receiptFooterText
)
