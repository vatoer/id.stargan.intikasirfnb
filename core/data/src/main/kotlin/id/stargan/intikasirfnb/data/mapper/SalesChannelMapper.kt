package id.stargan.intikasirfnb.data.mapper

import id.stargan.intikasirfnb.data.local.entity.SalesChannelEntity
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.transaction.ChannelType
import id.stargan.intikasirfnb.domain.transaction.OrderFlowType
import id.stargan.intikasirfnb.domain.transaction.PlatformConfig
import id.stargan.intikasirfnb.domain.transaction.PriceAdjustmentType
import id.stargan.intikasirfnb.domain.transaction.SalesChannel
import id.stargan.intikasirfnb.domain.transaction.SalesChannelId
import id.stargan.intikasirfnb.domain.transaction.defaultFlow
import java.math.BigDecimal

fun SalesChannelEntity.toDomain(): SalesChannel = SalesChannel(
    id = SalesChannelId(id),
    tenantId = TenantId(tenantId),
    channelType = ChannelType.valueOf(channelType),
    name = name,
    code = code,
    isActive = isActive,
    sortOrder = sortOrder,
    defaultOrderFlow = try { OrderFlowType.valueOf(defaultOrderFlow) } catch (_: Exception) {
        ChannelType.valueOf(channelType).defaultFlow()
    },
    priceAdjustmentType = priceAdjustmentType?.let { PriceAdjustmentType.valueOf(it) },
    priceAdjustmentValue = priceAdjustmentValue?.let { BigDecimal(it) },
    platformConfig = if (platformName != null) PlatformConfig(
        platformName = platformName,
        commissionPercent = commissionPercent?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
        requiresExternalOrderId = requiresExternalOrderId,
        autoConfirmOrder = autoConfirmOrder
    ) else null
)

fun SalesChannel.toEntity(): SalesChannelEntity = SalesChannelEntity(
    id = id.value,
    tenantId = tenantId.value,
    channelType = channelType.name,
    name = name,
    code = code,
    isActive = isActive,
    sortOrder = sortOrder,
    defaultOrderFlow = defaultOrderFlow.name,
    priceAdjustmentType = priceAdjustmentType?.name,
    priceAdjustmentValue = priceAdjustmentValue?.toPlainString(),
    platformName = platformConfig?.platformName,
    commissionPercent = platformConfig?.commissionPercent?.toPlainString(),
    requiresExternalOrderId = platformConfig?.requiresExternalOrderId ?: false,
    autoConfirmOrder = platformConfig?.autoConfirmOrder ?: false
)
