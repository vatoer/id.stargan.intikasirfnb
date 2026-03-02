package id.stargan.intikasirfnb.domain.settings

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TenantId
import java.math.BigDecimal

/** Tax rate (e.g. 0.11 for 11% PPN). */
data class TaxRate(
    val code: String,
    val name: String,
    val rate: BigDecimal,
    val isInclusive: Boolean = false
)

/** Scope for numbering sequence (e.g. receipt, invoice). */
data class NumberingSequenceConfig(
    val prefix: String,
    val paddingLength: Int = 6,
    val nextNumber: Long = 1L
)

/** Tenant-level settings aggregate. */
data class TenantSettings(
    val tenantId: TenantId,
    val defaultCurrencyCode: String = "IDR",
    val taxRates: List<TaxRate> = emptyList(),
    val receiptNumbering: NumberingSequenceConfig? = null,
    val invoiceNumbering: NumberingSequenceConfig? = null
)

/** Outlet-level settings aggregate. */
data class OutletSettings(
    val outletId: OutletId,
    val tenantId: TenantId,
    val timeZoneId: String = "Asia/Jakarta",
    val receiptHeaderText: String? = null,
    val receiptFooterText: String? = null
)
