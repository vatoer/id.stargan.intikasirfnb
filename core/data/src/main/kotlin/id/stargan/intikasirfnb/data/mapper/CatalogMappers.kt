package id.stargan.intikasirfnb.data.mapper

import id.stargan.intikasirfnb.data.local.entity.CategoryEntity
import id.stargan.intikasirfnb.data.local.entity.MenuItemEntity
import id.stargan.intikasirfnb.data.local.entity.MenuItemModifierGroupEntity
import id.stargan.intikasirfnb.data.local.entity.ModifierGroupEntity
import id.stargan.intikasirfnb.data.local.entity.ModifierOptionEntity
import id.stargan.intikasirfnb.domain.catalog.Category
import id.stargan.intikasirfnb.domain.catalog.CategoryId
import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.MenuItemModifierLink
import id.stargan.intikasirfnb.domain.catalog.ModifierGroup
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupId
import id.stargan.intikasirfnb.domain.catalog.ModifierOption
import id.stargan.intikasirfnb.domain.catalog.ModifierOptionId
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.shared.Money
import java.math.BigDecimal

// --- Category ---

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

// --- MenuItem ---

fun MenuItemEntity.toDomain(
    modifierLinks: List<MenuItemModifierLink> = emptyList()
): MenuItem = MenuItem(
    id = ProductId(id),
    tenantId = TenantId(tenantId),
    categoryId = CategoryId(categoryId),
    name = name,
    description = description,
    imageUri = imageUri,
    basePrice = Money(BigDecimal(basePriceAmount), basePriceCurrency),
    taxCode = taxCode,
    modifierLinks = modifierLinks,
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
    imageUri = imageUri,
    basePriceAmount = basePrice.amount.toPlainString(),
    basePriceCurrency = basePrice.currencyCode,
    taxCode = taxCode,
    recipeJson = null,
    sortOrder = sortOrder,
    isActive = isActive
)

// --- ModifierGroup ---

fun ModifierGroupEntity.toDomain(
    options: List<ModifierOption> = emptyList()
): ModifierGroup = ModifierGroup(
    id = ModifierGroupId(id),
    tenantId = TenantId(tenantId),
    name = name,
    options = options,
    sortOrder = sortOrder,
    isActive = isActive
)

fun ModifierGroup.toEntity(): ModifierGroupEntity = ModifierGroupEntity(
    id = id.value,
    tenantId = tenantId.value,
    name = name,
    sortOrder = sortOrder,
    isActive = isActive
)

// --- ModifierOption ---

fun ModifierOptionEntity.toDomain(): ModifierOption = ModifierOption(
    id = ModifierOptionId(id),
    groupId = ModifierGroupId(groupId),
    name = name,
    priceDelta = Money(BigDecimal(priceDeltaAmount), priceDeltaCurrency),
    sortOrder = sortOrder,
    isActive = isActive
)

fun ModifierOption.toEntity(): ModifierOptionEntity = ModifierOptionEntity(
    id = id.value,
    groupId = groupId.value,
    name = name,
    priceDeltaAmount = priceDelta.amount.toPlainString(),
    priceDeltaCurrency = priceDelta.currencyCode,
    sortOrder = sortOrder,
    isActive = isActive
)

// --- MenuItemModifierLink (junction) ---

fun MenuItemModifierGroupEntity.toDomain(): MenuItemModifierLink = MenuItemModifierLink(
    id = id,
    menuItemId = ProductId(menuItemId),
    modifierGroupId = ModifierGroupId(modifierGroupId),
    sortOrder = sortOrder,
    isRequired = isRequired,
    minSelection = minSelection,
    maxSelection = maxSelection
)

fun MenuItemModifierLink.toEntity(): MenuItemModifierGroupEntity = MenuItemModifierGroupEntity(
    id = id,
    menuItemId = menuItemId.value,
    modifierGroupId = modifierGroupId.value,
    sortOrder = sortOrder,
    isRequired = isRequired,
    minSelection = minSelection,
    maxSelection = maxSelection
)
