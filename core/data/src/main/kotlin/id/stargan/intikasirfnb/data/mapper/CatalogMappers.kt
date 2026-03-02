package id.stargan.intikasirfnb.data.mapper

import id.stargan.intikasirfnb.data.local.entity.CategoryEntity
import id.stargan.intikasirfnb.data.local.entity.MenuItemEntity
import id.stargan.intikasirfnb.domain.catalog.Category
import id.stargan.intikasirfnb.domain.catalog.CategoryId
import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.shared.Money
import java.math.BigDecimal

fun CategoryEntity.toDomain(): Category = Category(
    id = CategoryId(id),
    tenantId = TenantId(tenantId),
    name = name,
    parentId = parentId?.let { CategoryId(it) },
    sortOrder = sortOrder,
    isActive = isActive
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id.value,
    tenantId = tenantId.value,
    name = name,
    parentId = parentId?.value,
    sortOrder = sortOrder,
    isActive = isActive
)

fun MenuItemEntity.toDomain(): MenuItem = MenuItem(
    id = ProductId(id),
    tenantId = TenantId(tenantId),
    categoryId = CategoryId(categoryId),
    name = name,
    description = description,
    basePrice = Money(BigDecimal(basePriceAmount), basePriceCurrency),
    taxCode = taxCode,
    modifierGroups = emptyList(),
    recipe = null,
    sortOrder = sortOrder,
    isActive = isActive
)

fun MenuItem.toEntity(): MenuItemEntity = MenuItemEntity(
    id = id.value,
    tenantId = tenantId.value,
    categoryId = categoryId.value,
    name = name,
    description = description,
    basePriceAmount = basePrice.amount.toPlainString(),
    basePriceCurrency = basePrice.currencyCode,
    taxCode = taxCode,
    modifierGroupsJson = "[]",
    recipeJson = null,
    sortOrder = sortOrder,
    isActive = isActive
)
