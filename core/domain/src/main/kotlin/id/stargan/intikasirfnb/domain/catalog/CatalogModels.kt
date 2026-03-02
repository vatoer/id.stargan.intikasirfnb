package id.stargan.intikasirfnb.domain.catalog

import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.shared.Money
import java.math.BigDecimal
import java.util.UUID

// --- Value objects / IDs ---

@JvmInline
value class ProductId(val value: String) {
    companion object {
        fun generate() = ProductId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class CategoryId(val value: String) {
    companion object {
        fun generate() = CategoryId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class IngredientId(val value: String) {
    companion object {
        fun generate() = IngredientId(UUID.randomUUID().toString())
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

// --- Modifier (F&B add-on / option) ---

data class ModifierOption(
    val id: String,
    val name: String,
    val priceDelta: Money = Money.zero()
)

data class ModifierGroup(
    val id: String,
    val name: String,
    val options: List<ModifierOption>,
    val minSelection: Int = 0,
    val maxSelection: Int = 1
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
    val basePrice: Money,
    val taxCode: String? = null,
    val modifierGroups: List<ModifierGroup> = emptyList(),
    val recipe: Recipe? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)
