package id.stargan.intikasirfnb.data.mapper

import id.stargan.intikasirfnb.data.local.entity.PriceListEntity
import id.stargan.intikasirfnb.data.local.entity.PriceListEntryEntity
import id.stargan.intikasirfnb.domain.catalog.PriceList
import id.stargan.intikasirfnb.domain.catalog.PriceListEntry
import id.stargan.intikasirfnb.domain.catalog.PriceListId
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.shared.Money
import java.math.BigDecimal

fun PriceListEntity.toDomain(entries: List<PriceListEntryEntity>): PriceList = PriceList(
    id = PriceListId(id),
    tenantId = TenantId(tenantId),
    name = name,
    description = description,
    entries = entries.map { it.toDomain() },
    isActive = isActive,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun PriceListEntryEntity.toDomain(): PriceListEntry = PriceListEntry(
    productId = ProductId(productId),
    price = Money(BigDecimal(priceAmount), priceCurrency)
)

fun PriceList.toEntity(): PriceListEntity = PriceListEntity(
    id = id.value,
    tenantId = tenantId.value,
    name = name,
    description = description,
    isActive = isActive,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun PriceListEntry.toEntity(priceListId: String): PriceListEntryEntity = PriceListEntryEntity(
    priceListId = priceListId,
    productId = productId.value,
    priceAmount = price.amount.toPlainString(),
    priceCurrency = price.currencyCode
)
