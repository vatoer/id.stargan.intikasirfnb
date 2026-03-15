package id.stargan.intikasirfnb.data.mapper

import id.stargan.intikasirfnb.data.local.entity.StockLevelEntity
import id.stargan.intikasirfnb.data.local.entity.StockMovementEntity
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.catalog.UnitOfMeasure
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.inventory.StockLevel
import id.stargan.intikasirfnb.domain.inventory.StockLevelId
import id.stargan.intikasirfnb.domain.inventory.StockMovement
import id.stargan.intikasirfnb.domain.inventory.StockMovementId
import id.stargan.intikasirfnb.domain.inventory.StockMovementType
import java.math.BigDecimal

fun StockLevelEntity.toDomain(): StockLevel = StockLevel(
    id = StockLevelId(id),
    productId = ProductId(productId),
    outletId = OutletId(outletId),
    productName = productName,
    quantity = BigDecimal(quantity),
    unitOfMeasure = try { UnitOfMeasure.valueOf(unitOfMeasure) } catch (_: Exception) { UnitOfMeasure.PCS },
    lowStockThreshold = BigDecimal(lowStockThreshold)
)

fun StockLevel.toEntity(): StockLevelEntity = StockLevelEntity(
    id = id.value,
    productId = productId.value,
    outletId = outletId.value,
    productName = productName,
    quantity = quantity.toPlainString(),
    unitOfMeasure = unitOfMeasure.name,
    lowStockThreshold = lowStockThreshold.toPlainString()
)

fun StockMovementEntity.toDomain(): StockMovement = StockMovement(
    id = StockMovementId(id),
    productId = ProductId(productId),
    outletId = OutletId(outletId),
    type = try { StockMovementType.valueOf(type) } catch (_: Exception) { StockMovementType.ADJUSTMENT },
    quantity = BigDecimal(quantity),
    notes = notes,
    referenceType = referenceType,
    referenceId = referenceId,
    createdAtMillis = createdAtMillis
)

fun StockMovement.toEntity(): StockMovementEntity = StockMovementEntity(
    id = id.value,
    productId = productId.value,
    outletId = outletId.value,
    type = type.name,
    quantity = quantity.toPlainString(),
    notes = notes,
    referenceType = referenceType,
    referenceId = referenceId,
    createdAtMillis = createdAtMillis
)
