package id.stargan.intikasirfnb.domain.transaction

import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.shared.UlidGenerator
import java.math.BigDecimal
import java.math.RoundingMode

// --- SalesChannel ID ---

@JvmInline
value class SalesChannelId(val value: String) {
    companion object {
        fun generate() = SalesChannelId(UlidGenerator.generate())
    }
}

// --- Channel Type ---

enum class ChannelType {
    DINE_IN,
    TAKE_AWAY,
    DELIVERY_PLATFORM,
    OWN_DELIVERY
}

// --- Order Flow Type ---
// Determines payment & kitchen workflow for each channel

enum class OrderFlowType {
    PAY_FIRST,      // Counter/Fast-food: Order → Pay → Queue Number → Kitchen → Pickup
    PAY_LAST,       // Table Service: Order → Kitchen → Serve → Bill → Pay
    PAY_FLEXIBLE    // Hybrid: Cashier decides per order
}

fun ChannelType.defaultFlow(): OrderFlowType = when (this) {
    ChannelType.DINE_IN -> OrderFlowType.PAY_LAST
    ChannelType.TAKE_AWAY -> OrderFlowType.PAY_FIRST
    ChannelType.DELIVERY_PLATFORM -> OrderFlowType.PAY_FIRST
    ChannelType.OWN_DELIVERY -> OrderFlowType.PAY_FIRST
}

// --- Price Adjustment ---

enum class PriceAdjustmentType {
    MARKUP_PERCENT,
    MARKUP_FIXED,
    DISCOUNT_PERCENT,
    DISCOUNT_FIXED
}

// --- Platform Config (for DELIVERY_PLATFORM channels) ---

data class PlatformConfig(
    val platformName: String,
    val commissionPercent: BigDecimal = BigDecimal.ZERO,
    val requiresExternalOrderId: Boolean = true,
    val autoConfirmOrder: Boolean = false
)

// --- SalesChannel aggregate ---

data class SalesChannel(
    val id: SalesChannelId,
    val tenantId: TenantId,
    val channelType: ChannelType,
    val name: String,
    val code: String,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val defaultOrderFlow: OrderFlowType = channelType.defaultFlow(),
    val priceAdjustmentType: PriceAdjustmentType? = null,
    val priceAdjustmentValue: BigDecimal? = null,
    val platformConfig: PlatformConfig? = null
) {
    init {
        require(name.isNotBlank()) { "Channel name must not be blank" }
        require(code.isNotBlank()) { "Channel code must not be blank" }
        if (channelType == ChannelType.DELIVERY_PLATFORM) {
            require(platformConfig != null) { "PlatformConfig required for DELIVERY_PLATFORM" }
        }
        if (priceAdjustmentType != null) {
            require(priceAdjustmentValue != null && priceAdjustmentValue > BigDecimal.ZERO) {
                "Adjustment value required when adjustment type is set"
            }
        }
    }

    // Table management not yet implemented (Phase 2).
    // When table management is available, this should check channelType == DINE_IN.
    val requiresTable: Boolean get() = false

    val requiresExternalOrderId: Boolean
        get() = platformConfig?.requiresExternalOrderId == true

    fun resolvePrice(basePrice: Money): Money {
        val type = priceAdjustmentType ?: return basePrice
        val value = priceAdjustmentValue ?: return basePrice
        val adjusted = when (type) {
            PriceAdjustmentType.MARKUP_PERCENT ->
                basePrice.amount.multiply(BigDecimal.ONE + value.divide(BigDecimal(100), 4, RoundingMode.HALF_UP))
            PriceAdjustmentType.MARKUP_FIXED ->
                basePrice.amount.add(value)
            PriceAdjustmentType.DISCOUNT_PERCENT ->
                basePrice.amount.multiply(BigDecimal.ONE - value.divide(BigDecimal(100), 4, RoundingMode.HALF_UP))
            PriceAdjustmentType.DISCOUNT_FIXED ->
                basePrice.amount.subtract(value)
        }
        return Money(adjusted.setScale(0, RoundingMode.HALF_UP), basePrice.currencyCode)
    }

    companion object {
        fun dineIn(tenantId: TenantId) = SalesChannel(
            id = SalesChannelId.generate(),
            tenantId = tenantId,
            channelType = ChannelType.DINE_IN,
            name = "Dine In",
            code = "DI",
            sortOrder = 1
        )

        fun takeAway(tenantId: TenantId) = SalesChannel(
            id = SalesChannelId.generate(),
            tenantId = tenantId,
            channelType = ChannelType.TAKE_AWAY,
            name = "Take Away",
            code = "TA",
            sortOrder = 2
        )
    }
}
