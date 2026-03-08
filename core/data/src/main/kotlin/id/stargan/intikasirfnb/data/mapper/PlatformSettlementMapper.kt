package id.stargan.intikasirfnb.data.mapper

import id.stargan.intikasirfnb.data.local.entity.PlatformSettlementEntity
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlement
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementId
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SalesChannelId
import id.stargan.intikasirfnb.domain.transaction.SettlementStatus
import org.json.JSONArray
import java.math.BigDecimal

fun PlatformSettlementEntity.toDomain(): PlatformSettlement = PlatformSettlement(
    id = PlatformSettlementId(id),
    outletId = OutletId(outletId),
    channelId = SalesChannelId(channelId),
    platformName = platformName,
    saleIds = deserializeSaleIds(saleIdsJson),
    expectedAmount = Money(BigDecimal(expectedAmountAmount), expectedAmountCurrency),
    settledAmount = settledAmountAmount?.let { Money(BigDecimal(it), settledAmountCurrency ?: "IDR") },
    commissionTotal = Money(BigDecimal(commissionTotalAmount), commissionTotalCurrency),
    status = try { SettlementStatus.valueOf(status) } catch (_: Exception) { SettlementStatus.PENDING },
    platformReference = platformReference,
    settlementDate = settlementDate,
    notes = notes,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun PlatformSettlement.toEntity(): PlatformSettlementEntity = PlatformSettlementEntity(
    id = id.value,
    outletId = outletId.value,
    channelId = channelId.value,
    platformName = platformName,
    saleIdsJson = serializeSaleIds(saleIds),
    expectedAmountAmount = expectedAmount.amount.toPlainString(),
    expectedAmountCurrency = expectedAmount.currencyCode,
    settledAmountAmount = settledAmount?.amount?.toPlainString(),
    settledAmountCurrency = settledAmount?.currencyCode,
    commissionTotalAmount = commissionTotal.amount.toPlainString(),
    commissionTotalCurrency = commissionTotal.currencyCode,
    status = status.name,
    platformReference = platformReference,
    settlementDate = settlementDate,
    notes = notes,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

private fun serializeSaleIds(saleIds: List<SaleId>): String {
    val arr = JSONArray()
    saleIds.forEach { arr.put(it.value) }
    return arr.toString()
}

private fun deserializeSaleIds(json: String): List<SaleId> {
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { SaleId(arr.getString(it)) }
    } catch (_: Exception) {
        emptyList()
    }
}
