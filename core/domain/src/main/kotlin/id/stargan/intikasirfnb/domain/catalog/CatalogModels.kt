package id.stargan.intikasirfnb.domain.catalog

import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.shared.UlidGenerator
import java.math.BigDecimal

// --- Value objects / IDs ---

@JvmInline
value class ProductId(val value: String) {
    companion object {
        fun generate() = ProductId(UlidGenerator.generate())
    }
}

@JvmInline
value class CategoryId(val value: String) {
    companion object {
        fun generate() = CategoryId(UlidGenerator.generate())
    }
}

@JvmInline
value class ModifierGroupId(val value: String) {
    companion object {
        fun generate() = ModifierGroupId(UlidGenerator.generate())
    }
}

@JvmInline
value class ModifierOptionId(val value: String) {
    companion object {
        fun generate() = ModifierOptionId(UlidGenerator.generate())
    }
}

@JvmInline
value class IngredientId(val value: String) {
    companion object {
        fun generate() = IngredientId(UlidGenerator.generate())
    }
}

enum class UnitOfMeasure { PCS, KG, GRAM, LITER, ML, PORTION, PACK, HOUR }

// --- Category aggregate ---

data class Category(
    val id: CategoryId,
    val tenantId: TenantId,
    val name: String,
    val parentId: CategoryId? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

// --- Modifier (F&B add-on / option) — separate entities, reusable across items ---

data class ModifierOption(
    val id: ModifierOptionId,
    val groupId: ModifierGroupId,
    val name: String,
    val priceDelta: Money = Money.zero(),
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

data class ModifierGroup(
    val id: ModifierGroupId,
    val tenantId: TenantId,
    val name: String,
    val options: List<ModifierOption> = emptyList(),
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

/**
 * Junction: links a ModifierGroup to a MenuItem with per-item overrides.
 */
data class MenuItemModifierLink(
    val id: String,
    val menuItemId: ProductId,
    val modifierGroupId: ModifierGroupId,
    val sortOrder: Int = 0,
    val isRequired: Boolean = false,
    val minSelection: Int = 0,
    val maxSelection: Int = 1
)

// --- Add-on (F&B qty-based extras) — separate entities, reusable across items ---

@JvmInline
value class AddOnGroupId(val value: String) {
    companion object {
        fun generate() = AddOnGroupId(UlidGenerator.generate())
    }
}

@JvmInline
value class AddOnItemId(val value: String) {
    companion object {
        fun generate() = AddOnItemId(UlidGenerator.generate())
    }
}

data class AddOnItem(
    val id: AddOnItemId,
    val groupId: AddOnGroupId,
    val name: String,
    val price: Money = Money.zero(),
    val maxQty: Int = 5,
    val inventoryItemId: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

data class AddOnGroup(
    val id: AddOnGroupId,
    val tenantId: TenantId,
    val name: String,
    val items: List<AddOnItem> = emptyList(),
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

/**
 * Junction: links an AddOnGroup to a MenuItem.
 * Simpler than ModifierLink — no required/min/max since add-ons are qty-based.
 */
data class MenuItemAddOnLink(
    val id: String,
    val menuItemId: ProductId,
    val addOnGroupId: AddOnGroupId,
    val sortOrder: Int = 0
)

// --- Recipe (optional, for COGS) ---

data class RecipeLine(
    val ingredientId: IngredientId,
    val quantityPerPortion: BigDecimal,
    val unitOfMeasure: UnitOfMeasure
)

data class Recipe(
    val lines: List<RecipeLine>
)

// --- MenuItem (F&B) aggregate root ---

data class MenuItem(
    val id: ProductId,
    val tenantId: TenantId,
    val categoryId: CategoryId,
    val name: String,
    val description: String? = null,
    val imageUri: String? = null,
    val basePrice: Money,
    val taxCode: String? = null,
    val modifierLinks: List<MenuItemModifierLink> = emptyList(),
    val addOnLinks: List<MenuItemAddOnLink> = emptyList(),
    val recipe: Recipe? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)
