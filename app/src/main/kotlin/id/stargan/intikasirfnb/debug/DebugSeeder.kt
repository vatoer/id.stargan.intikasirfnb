package id.stargan.intikasirfnb.debug

import id.stargan.intikasirfnb.domain.catalog.Category
import id.stargan.intikasirfnb.domain.catalog.CategoryId
import id.stargan.intikasirfnb.domain.catalog.CategoryRepository
import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.MenuItemRepository
import id.stargan.intikasirfnb.domain.catalog.ModifierGroup
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupId
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupRepository
import id.stargan.intikasirfnb.domain.catalog.ModifierOption
import id.stargan.intikasirfnb.domain.catalog.ModifierOptionId
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.Outlet
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.TenantRepository
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.usecase.identity.CompleteOnboardingUseCase
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug-only seeder. Populates the database with sample data so you don't
 * have to fill in onboarding every time during development.
 *
 * PIN: 1234
 */
@Singleton
class DebugSeeder @Inject constructor(
    private val tenantRepository: TenantRepository,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val sessionManager: SessionManager,
    private val categoryRepository: CategoryRepository,
    private val menuItemRepository: MenuItemRepository,
    private val modifierGroupRepository: ModifierGroupRepository
) {
    /**
     * Returns true if the database is empty (no tenant).
     */
    suspend fun needsSeed(): Boolean = tenantRepository.listAll().isEmpty()

    /**
     * Seeds onboarding data + sample catalog.
     * After this call the user is logged in and outlet is selected — ready to go to Landing.
     */
    suspend fun seed() {
        // 1. Complete onboarding
        completeOnboardingUseCase(
            businessName = "Warung Debug",
            outletName = "Outlet Utama",
            outletAddress = "Jl. Debug No. 1, Jakarta",
            ownerName = "Admin Dev",
            ownerEmail = "dev@intikasir.id",
            pin = "1234"
        )

        val outlet = sessionManager.getCurrentOutlet() ?: return
        val tenantId = outlet.tenantId

        // 2. Seed categories
        val catMakanan = Category(
            id = CategoryId.generate(), tenantId = tenantId,
            name = "Makanan", sortOrder = 1
        )
        val catMinuman = Category(
            id = CategoryId.generate(), tenantId = tenantId,
            name = "Minuman", sortOrder = 2
        )
        val catSnack = Category(
            id = CategoryId.generate(), tenantId = tenantId,
            name = "Snack", sortOrder = 3
        )
        val catDessert = Category(
            id = CategoryId.generate(), tenantId = tenantId,
            name = "Dessert", sortOrder = 4
        )
        listOf(catMakanan, catMinuman, catSnack, catDessert).forEach {
            categoryRepository.save(it)
        }

        // 3. Seed modifier groups
        val sizeGroupId = ModifierGroupId.generate()
        val sizeGroup = ModifierGroup(
            id = sizeGroupId, tenantId = tenantId, name = "Ukuran", sortOrder = 1,
            options = listOf(
                ModifierOption(ModifierOptionId.generate(), sizeGroupId, "Regular", Money.zero(), 1),
                ModifierOption(ModifierOptionId.generate(), sizeGroupId, "Large", Money(BigDecimal("5000")), 2)
            )
        )

        val sugarGroupId = ModifierGroupId.generate()
        val sugarGroup = ModifierGroup(
            id = sugarGroupId, tenantId = tenantId, name = "Level Gula", sortOrder = 2,
            options = listOf(
                ModifierOption(ModifierOptionId.generate(), sugarGroupId, "Normal", Money.zero(), 1),
                ModifierOption(ModifierOptionId.generate(), sugarGroupId, "Less Sugar", Money.zero(), 2),
                ModifierOption(ModifierOptionId.generate(), sugarGroupId, "No Sugar", Money.zero(), 3)
            )
        )

        val iceGroupId = ModifierGroupId.generate()
        val iceGroup = ModifierGroup(
            id = iceGroupId, tenantId = tenantId, name = "Level Es", sortOrder = 3,
            options = listOf(
                ModifierOption(ModifierOptionId.generate(), iceGroupId, "Normal", Money.zero(), 1),
                ModifierOption(ModifierOptionId.generate(), iceGroupId, "Less Ice", Money.zero(), 2),
                ModifierOption(ModifierOptionId.generate(), iceGroupId, "No Ice", Money.zero(), 3)
            )
        )

        val spiceGroupId = ModifierGroupId.generate()
        val spiceGroup = ModifierGroup(
            id = spiceGroupId, tenantId = tenantId, name = "Level Pedas", sortOrder = 4,
            options = listOf(
                ModifierOption(ModifierOptionId.generate(), spiceGroupId, "Tidak Pedas", Money.zero(), 1),
                ModifierOption(ModifierOptionId.generate(), spiceGroupId, "Sedang", Money.zero(), 2),
                ModifierOption(ModifierOptionId.generate(), spiceGroupId, "Pedas", Money.zero(), 3),
                ModifierOption(ModifierOptionId.generate(), spiceGroupId, "Extra Pedas", Money.zero(), 4)
            )
        )

        listOf(sizeGroup, sugarGroup, iceGroup, spiceGroup).forEach {
            modifierGroupRepository.save(it)
        }

        // 4. Seed menu items
        val menuItems = listOf(
            // Makanan
            MenuItem(ProductId.generate(), tenantId, catMakanan.id, "Nasi Goreng", "Nasi goreng spesial", basePrice = Money(BigDecimal("25000")), sortOrder = 1),
            MenuItem(ProductId.generate(), tenantId, catMakanan.id, "Mie Goreng", "Mie goreng telur", basePrice = Money(BigDecimal("22000")), sortOrder = 2),
            MenuItem(ProductId.generate(), tenantId, catMakanan.id, "Nasi Ayam Bakar", "Ayam bakar + nasi + sambal", basePrice = Money(BigDecimal("30000")), sortOrder = 3),
            MenuItem(ProductId.generate(), tenantId, catMakanan.id, "Soto Ayam", "Soto ayam kampung", basePrice = Money(BigDecimal("20000")), sortOrder = 4),
            MenuItem(ProductId.generate(), tenantId, catMakanan.id, "Gado-Gado", "Gado-gado bumbu kacang", basePrice = Money(BigDecimal("18000")), sortOrder = 5),

            // Minuman
            MenuItem(ProductId.generate(), tenantId, catMinuman.id, "Es Teh Manis", null, basePrice = Money(BigDecimal("8000")), sortOrder = 1),
            MenuItem(ProductId.generate(), tenantId, catMinuman.id, "Kopi Latte", "Hot/iced latte", basePrice = Money(BigDecimal("25000")), sortOrder = 2),
            MenuItem(ProductId.generate(), tenantId, catMinuman.id, "Jus Alpukat", null, basePrice = Money(BigDecimal("18000")), sortOrder = 3),
            MenuItem(ProductId.generate(), tenantId, catMinuman.id, "Air Mineral", null, basePrice = Money(BigDecimal("5000")), sortOrder = 4),
            MenuItem(ProductId.generate(), tenantId, catMinuman.id, "Es Jeruk", null, basePrice = Money(BigDecimal("10000")), sortOrder = 5),

            // Snack
            MenuItem(ProductId.generate(), tenantId, catSnack.id, "Kentang Goreng", "French fries", basePrice = Money(BigDecimal("15000")), sortOrder = 1),
            MenuItem(ProductId.generate(), tenantId, catSnack.id, "Pisang Goreng", "Pisang goreng crispy", basePrice = Money(BigDecimal("12000")), sortOrder = 2),
            MenuItem(ProductId.generate(), tenantId, catSnack.id, "Tahu Crispy", null, basePrice = Money(BigDecimal("10000")), sortOrder = 3),

            // Dessert
            MenuItem(ProductId.generate(), tenantId, catDessert.id, "Es Krim Coklat", null, basePrice = Money(BigDecimal("15000")), sortOrder = 1),
            MenuItem(ProductId.generate(), tenantId, catDessert.id, "Pancake", "Pancake + maple syrup", basePrice = Money(BigDecimal("20000")), sortOrder = 2)
        )

        menuItems.forEach { menuItemRepository.save(it) }
    }

    companion object {
        const val DEBUG_PIN = "1234"
    }
}
