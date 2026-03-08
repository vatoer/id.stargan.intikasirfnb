package id.stargan.intikasirfnb.domain.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.shared.UlidGenerator
import java.math.BigDecimal
import java.math.RoundingMode

// --- IDs ---

@JvmInline
value class PlatformSettlementId(val value: String) {
    companion object {
        fun generate() = PlatformSettlementId(UlidGenerator.generate())
    }
}

// --- PlatformPayment VO — embedded in Sale for platform orders ---

/**
 * Captures the financial breakdown of a platform delivery order.
 * Attached to a Sale when channelType == DELIVERY_PLATFORM.
 *
 * grossAmount     = total customer pays on the platform
 * commission      = platform fee (calculated from commissionPercent)
 * netAmount       = grossAmount - commission (what merchant receives)
 */
data class PlatformPayment(
    val grossAmount: Money,
    val commissionPercent: BigDecimal,
    val commissionType: CommissionType,
    val commissionAmount: Money,
    val netAmount: Money,
    val platformName: String,
    val platformOrderId: String? = null
) {
    companion object {
        /**
         * Calculate platform payment breakdown from sale subtotal and platform config.
         * @param subtotal Sale subtotal (menu price total)
         * @param sellingTotal Actual selling total (after markup/discount)
         * @param config Platform configuration with commission settings
         * @param platformOrderId External order ID from the platform
         */
        fun calculate(
            subtotal: Money,
            sellingTotal: Money,
            config: PlatformConfig,
            platformOrderId: String? = null
        ): PlatformPayment {
            val baseAmount = when (config.commissionType) {
                CommissionType.FROM_MENU_PRICE -> subtotal
                CommissionType.FROM_SELLING_PRICE -> sellingTotal
            }
            val commission = baseAmount.amount
                .multiply(config.commissionPercent)
                .divide(BigDecimal(100), 0, RoundingMode.HALF_UP)
            val commissionMoney = Money(commission, sellingTotal.currencyCode)
            val net = Money(
                sellingTotal.amount.subtract(commission).coerceAtLeast(BigDecimal.ZERO),
                sellingTotal.currencyCode
            )
            return PlatformPayment(
                grossAmount = sellingTotal,
                commissionPercent = config.commissionPercent,
                commissionType = config.commissionType,
                commissionAmount = commissionMoney,
                netAmount = net,
                platformName = config.platformName,
                platformOrderId = platformOrderId
            )
        }

        private fun BigDecimal.coerceAtLeast(min: BigDecimal): BigDecimal =
            if (this < min) min else this
    }
}

// --- Settlement Status ---

enum class SettlementStatus {
    PENDING,      // Platform has not yet settled
    SETTLED,      // Platform has transferred the funds
    PARTIAL,      // Partially settled (rare, but possible with deductions)
    DISPUTED,     // Merchant disputes the settlement amount
    CANCELLED     // Order cancelled, no settlement expected
}

// --- PlatformSettlement entity — tracks settlement from platform to merchant ---

/**
 * Tracks a settlement batch or individual settlement from a delivery platform.
 * One settlement can cover multiple sales (batch settlement) or one sale.
 */
data class PlatformSettlement(
    val id: PlatformSettlementId,
    val outletId: OutletId,
    val channelId: SalesChannelId,
    val platformName: String,
    val saleIds: List<SaleId>,
    val expectedAmount: Money,
    val settledAmount: Money? = null,
    val commissionTotal: Money,
    val status: SettlementStatus = SettlementStatus.PENDING,
    val platformReference: String? = null,
    val settlementDate: Long? = null,
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    /** Difference between expected and settled — positive means underpaid */
    fun difference(): Money? {
        val settled = settledAmount ?: return null
        return Money(
            expectedAmount.amount.subtract(settled.amount),
            expectedAmount.currencyCode
        )
    }

    fun markSettled(amount: Money, reference: String? = null, date: Long = System.currentTimeMillis()): PlatformSettlement {
        return copy(
            settledAmount = amount,
            platformReference = reference ?: platformReference,
            settlementDate = date,
            status = if (amount.amount.compareTo(expectedAmount.amount) == 0)
                SettlementStatus.SETTLED else SettlementStatus.PARTIAL,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun markDisputed(notes: String? = null): PlatformSettlement {
        return copy(
            status = SettlementStatus.DISPUTED,
            notes = notes ?: this.notes,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun markCancelled(): PlatformSettlement {
        return copy(
            status = SettlementStatus.CANCELLED,
            updatedAtMillis = System.currentTimeMillis()
        )
    }
}
