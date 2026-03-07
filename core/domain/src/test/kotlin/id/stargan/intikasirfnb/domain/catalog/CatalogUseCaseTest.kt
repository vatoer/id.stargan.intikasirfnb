package id.stargan.intikasirfnb.domain.catalog

import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.usecase.catalog.DeleteMenuItemUseCase
import id.stargan.intikasirfnb.domain.usecase.catalog.GetCategoriesUseCase
import id.stargan.intikasirfnb.domain.usecase.catalog.GetMenuItemsByCategoryUseCase
import id.stargan.intikasirfnb.domain.usecase.catalog.GetMenuItemsUseCase
import id.stargan.intikasirfnb.domain.usecase.catalog.GetModifierGroupsUseCase
import id.stargan.intikasirfnb.domain.usecase.catalog.SaveCategoryUseCase
import id.stargan.intikasirfnb.domain.usecase.catalog.SaveMenuItemUseCase
import id.stargan.intikasirfnb.domain.usecase.catalog.SaveModifierGroupUseCase
import id.stargan.intikasirfnb.domain.usecase.catalog.SearchMenuItemsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CatalogUseCaseTest {

    private val tenantId = TenantId.generate()

    // ==================== SaveCategoryUseCase ====================

    @Test
    fun `SaveCategoryUseCase saves category`() = runTest {
        val repo = FakeCategoryRepository()
        val useCase = SaveCategoryUseCase(repo)
        val category = makeCategory("Makanan")
        useCase(category)
        assertEquals(category, repo.items[category.id])
    }

    // ==================== GetCategoriesUseCase ====================

    @Test
    fun `GetCategoriesUseCase returns categories for tenant`() = runTest {
        val repo = FakeCategoryRepository()
        val cat1 = makeCategory("Makanan")
        val cat2 = makeCategory("Minuman")
        val otherTenant = makeCategory("Other", TenantId.generate())
        repo.save(cat1)
        repo.save(cat2)
        repo.save(otherTenant)

        val useCase = GetCategoriesUseCase(repo)
        val result = useCase(tenantId)
        assertEquals(2, result.size)
        assertTrue(result.none { it.name == "Other" })
    }

    // ==================== SaveMenuItemUseCase ====================

    @Test
    fun `SaveMenuItemUseCase saves valid item`() = runTest {
        val repo = FakeMenuItemRepository()
        val useCase = SaveMenuItemUseCase(repo)
        val item = makeMenuItem("Nasi Goreng", BigDecimal("25000"))
        useCase(item)
        assertEquals(item, repo.items[item.id])
    }

    // ==================== GetMenuItemsUseCase ====================

    @Test
    fun `GetMenuItemsUseCase returns items for tenant`() = runTest {
        val repo = FakeMenuItemRepository()
        val item1 = makeMenuItem("Nasi Goreng", BigDecimal("25000"))
        val item2 = makeMenuItem("Es Teh", BigDecimal("8000"))
        repo.save(item1)
        repo.save(item2)

        val useCase = GetMenuItemsUseCase(repo)
        val result = useCase(tenantId)
        assertEquals(2, result.size)
    }

    // ==================== GetMenuItemsByCategoryUseCase ====================

    @Test
    fun `GetMenuItemsByCategoryUseCase filters by category`() = runTest {
        val repo = FakeMenuItemRepository()
        val catId1 = CategoryId.generate()
        val catId2 = CategoryId.generate()
        repo.save(makeMenuItem("Nasi Goreng", BigDecimal("25000"), catId1))
        repo.save(makeMenuItem("Mie Goreng", BigDecimal("22000"), catId1))
        repo.save(makeMenuItem("Es Teh", BigDecimal("8000"), catId2))

        val useCase = GetMenuItemsByCategoryUseCase(repo)
        val result = useCase(catId1)
        assertEquals(2, result.size)
        assertTrue(result.all { it.categoryId == catId1 })
    }

    // ==================== SearchMenuItemsUseCase ====================

    @Test
    fun `SearchMenuItemsUseCase finds matching items`() = runTest {
        val repo = FakeMenuItemRepository()
        repo.save(makeMenuItem("Nasi Goreng Spesial", BigDecimal("25000")))
        repo.save(makeMenuItem("Nasi Ayam Bakar", BigDecimal("30000")))
        repo.save(makeMenuItem("Es Teh Manis", BigDecimal("8000")))

        val useCase = SearchMenuItemsUseCase(repo)
        val result = useCase(tenantId, "Nasi")
        assertEquals(2, result.size)
        assertTrue(result.all { it.name.contains("Nasi") })
    }

    @Test
    fun `SearchMenuItemsUseCase returns empty for no match`() = runTest {
        val repo = FakeMenuItemRepository()
        repo.save(makeMenuItem("Nasi Goreng", BigDecimal("25000")))

        val useCase = SearchMenuItemsUseCase(repo)
        val result = useCase(tenantId, "Pizza")
        assertTrue(result.isEmpty())
    }

    // ==================== DeleteMenuItemUseCase ====================

    @Test
    fun `DeleteMenuItemUseCase deletes links then item`() = runTest {
        val menuRepo = FakeMenuItemRepository()
        val modRepo = FakeModifierGroupRepository()
        val item = makeMenuItem("Nasi Goreng", BigDecimal("25000"))
        menuRepo.save(item)

        // Add a modifier link
        val link = MenuItemModifierLink(
            id = "link-1",
            menuItemId = item.id,
            modifierGroupId = ModifierGroupId.generate()
        )
        modRepo.saveLink(link)
        assertEquals(1, modRepo.getLinksForItem(item.id).size)

        val useCase = DeleteMenuItemUseCase(menuRepo, modRepo)
        useCase(item.id)

        assertNull(menuRepo.items[item.id])
        assertTrue(modRepo.getLinksForItem(item.id).isEmpty())
    }

    // ==================== SaveModifierGroupUseCase ====================

    @Test
    fun `SaveModifierGroupUseCase saves valid group`() = runTest {
        val repo = FakeModifierGroupRepository()
        val useCase = SaveModifierGroupUseCase(repo)
        val group = makeModifierGroup("Ukuran")
        useCase(group)
        assertEquals(group, repo.groups[group.id])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SaveModifierGroupUseCase rejects blank name`() = runTest {
        val repo = FakeModifierGroupRepository()
        val useCase = SaveModifierGroupUseCase(repo)
        val group = makeModifierGroup("")
        useCase(group)
    }

    // ==================== GetModifierGroupsUseCase ====================

    @Test
    fun `GetModifierGroupsUseCase returns groups for tenant`() = runTest {
        val repo = FakeModifierGroupRepository()
        val g1 = makeModifierGroup("Ukuran")
        val g2 = makeModifierGroup("Level Gula")
        repo.save(g1)
        repo.save(g2)

        val useCase = GetModifierGroupsUseCase(repo)
        val result = useCase(tenantId)
        assertEquals(2, result.size)
    }

    // ==================== Helpers ====================

    private fun makeCategory(name: String, tid: TenantId = tenantId) = Category(
        id = CategoryId.generate(),
        tenantId = tid,
        name = name
    )

    private fun makeMenuItem(
        name: String,
        price: BigDecimal,
        categoryId: CategoryId = CategoryId.generate()
    ) = MenuItem(
        id = ProductId.generate(),
        tenantId = tenantId,
        categoryId = categoryId,
        name = name,
        basePrice = Money(price)
    )

    private fun makeModifierGroup(name: String) = ModifierGroup(
        id = ModifierGroupId.generate(),
        tenantId = tenantId,
        name = name
    )

    // ==================== Fake Repositories ====================

    private class FakeCategoryRepository : CategoryRepository {
        val items = mutableMapOf<CategoryId, Category>()
        override suspend fun getById(id: CategoryId) = items[id]
        override suspend fun save(category: Category) { items[category.id] = category }
        override suspend fun delete(id: CategoryId) { items.remove(id) }
        override suspend fun listByTenant(tenantId: TenantId) =
            items.values.filter { it.tenantId == tenantId }
    }

    private class FakeMenuItemRepository : MenuItemRepository {
        val items = mutableMapOf<ProductId, MenuItem>()
        override suspend fun getById(id: ProductId) = items[id]
        override suspend fun save(menuItem: MenuItem) { items[menuItem.id] = menuItem }
        override suspend fun delete(id: ProductId) { items.remove(id) }
        override suspend fun listByTenant(tenantId: TenantId) =
            items.values.filter { it.tenantId == tenantId }
        override suspend fun listByCategory(categoryId: CategoryId) =
            items.values.filter { it.categoryId == categoryId }
        override suspend fun searchByName(tenantId: TenantId, query: String) =
            items.values.filter { it.tenantId == tenantId && it.name.contains(query, ignoreCase = true) }
    }

    private class FakeModifierGroupRepository : ModifierGroupRepository {
        val groups = mutableMapOf<ModifierGroupId, ModifierGroup>()
        private val links = mutableListOf<MenuItemModifierLink>()

        override suspend fun getById(id: ModifierGroupId) = groups[id]
        override suspend fun save(group: ModifierGroup) { groups[group.id] = group }
        override suspend fun delete(id: ModifierGroupId) { groups.remove(id) }
        override suspend fun listByTenant(tenantId: TenantId) =
            groups.values.filter { it.tenantId == tenantId }
        override suspend fun getLinksForItem(menuItemId: ProductId) =
            links.filter { it.menuItemId == menuItemId }
        override suspend fun saveLink(link: MenuItemModifierLink) { links.add(link) }
        override suspend fun deleteLink(menuItemId: ProductId, modifierGroupId: ModifierGroupId) {
            links.removeAll { it.menuItemId == menuItemId && it.modifierGroupId == modifierGroupId }
        }
        override suspend fun deleteAllLinksForItem(menuItemId: ProductId) {
            links.removeAll { it.menuItemId == menuItemId }
        }
    }
}
