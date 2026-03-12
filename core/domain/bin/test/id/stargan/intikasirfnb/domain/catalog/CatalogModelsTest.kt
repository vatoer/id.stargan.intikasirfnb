package id.stargan.intikasirfnb.domain.catalog

import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.shared.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CatalogModelsTest {

    private val tenantId = TenantId.generate()

    // ==================== ID generation ====================

    @Test
    fun `ProductId generate creates unique ids`() {
        val id1 = ProductId.generate()
        val id2 = ProductId.generate()
        assertNotEquals(id1.value, id2.value)
        assertTrue(id1.value.isNotBlank())
    }

    @Test
    fun `CategoryId generate creates unique ids`() {
        val id1 = CategoryId.generate()
        val id2 = CategoryId.generate()
        assertNotEquals(id1.value, id2.value)
    }

    @Test
    fun `ModifierGroupId generate creates unique ids`() {
        val id1 = ModifierGroupId.generate()
        val id2 = ModifierGroupId.generate()
        assertNotEquals(id1.value, id2.value)
    }

    @Test
    fun `ModifierOptionId generate creates unique ids`() {
        val id1 = ModifierOptionId.generate()
        val id2 = ModifierOptionId.generate()
        assertNotEquals(id1.value, id2.value)
    }

    // ==================== Category ====================

    @Test
    fun `Category defaults`() {
        val cat = Category(
            id = CategoryId.generate(),
            tenantId = tenantId,
            name = "Makanan"
        )
        assertNull(cat.parentId)
        assertEquals(0, cat.sortOrder)
        assertTrue(cat.isActive)
    }

    @Test
    fun `Category with parent`() {
        val parentId = CategoryId.generate()
        val child = Category(
            id = CategoryId.generate(),
            tenantId = tenantId,
            name = "Hot Drinks",
            parentId = parentId,
            sortOrder = 2
        )
        assertEquals(parentId, child.parentId)
        assertEquals(2, child.sortOrder)
    }

    @Test
    fun `Category copy changes isActive`() {
        val cat = Category(
            id = CategoryId.generate(),
            tenantId = tenantId,
            name = "Minuman",
            isActive = true
        )
        val inactive = cat.copy(isActive = false)
        assertFalse(inactive.isActive)
        assertEquals(cat.id, inactive.id)
        assertEquals(cat.name, inactive.name)
    }

    // ==================== MenuItem ====================

    @Test
    fun `MenuItem defaults`() {
        val item = MenuItem(
            id = ProductId.generate(),
            tenantId = tenantId,
            categoryId = CategoryId.generate(),
            name = "Nasi Goreng",
            basePrice = Money(BigDecimal("25000"))
        )
        assertNull(item.description)
        assertNull(item.imageUri)
        assertNull(item.taxCode)
        assertNull(item.recipe)
        assertEquals(0, item.sortOrder)
        assertTrue(item.isActive)
        assertTrue(item.modifierLinks.isEmpty())
    }

    @Test
    fun `MenuItem with all fields`() {
        val catId = CategoryId.generate()
        val item = MenuItem(
            id = ProductId.generate(),
            tenantId = tenantId,
            categoryId = catId,
            name = "Kopi Latte",
            description = "Hot/iced latte",
            imageUri = "/data/images/latte.jpg",
            basePrice = Money(BigDecimal("25000")),
            taxCode = "PPN",
            sortOrder = 5,
            isActive = false
        )
        assertEquals("Kopi Latte", item.name)
        assertEquals("Hot/iced latte", item.description)
        assertEquals("/data/images/latte.jpg", item.imageUri)
        assertEquals(BigDecimal("25000"), item.basePrice.amount)
        assertEquals("PPN", item.taxCode)
        assertEquals(catId, item.categoryId)
        assertEquals(5, item.sortOrder)
        assertFalse(item.isActive)
    }

    @Test
    fun `MenuItem basePrice is Money with IDR`() {
        val item = MenuItem(
            id = ProductId.generate(),
            tenantId = tenantId,
            categoryId = CategoryId.generate(),
            name = "Test",
            basePrice = Money(BigDecimal("15000"))
        )
        assertEquals("IDR", item.basePrice.currencyCode)
        assertTrue(item.basePrice.isPositive())
    }

    // ==================== ModifierOption ====================

    @Test
    fun `ModifierOption defaults`() {
        val groupId = ModifierGroupId.generate()
        val opt = ModifierOption(
            id = ModifierOptionId.generate(),
            groupId = groupId,
            name = "Regular"
        )
        assertTrue(opt.priceDelta.isZero())
        assertEquals(0, opt.sortOrder)
        assertTrue(opt.isActive)
    }

    @Test
    fun `ModifierOption with price delta`() {
        val groupId = ModifierGroupId.generate()
        val opt = ModifierOption(
            id = ModifierOptionId.generate(),
            groupId = groupId,
            name = "Large",
            priceDelta = Money(BigDecimal("5000")),
            sortOrder = 2
        )
        assertEquals("Large", opt.name)
        assertEquals(BigDecimal("5000"), opt.priceDelta.amount)
        assertTrue(opt.priceDelta.isPositive())
        assertEquals(2, opt.sortOrder)
    }

    // ==================== ModifierGroup ====================

    @Test
    fun `ModifierGroup defaults`() {
        val group = ModifierGroup(
            id = ModifierGroupId.generate(),
            tenantId = tenantId,
            name = "Ukuran"
        )
        assertTrue(group.options.isEmpty())
        assertEquals(0, group.sortOrder)
        assertTrue(group.isActive)
    }

    @Test
    fun `ModifierGroup with options`() {
        val groupId = ModifierGroupId.generate()
        val options = listOf(
            ModifierOption(ModifierOptionId.generate(), groupId, "Regular", Money.zero(), 1),
            ModifierOption(ModifierOptionId.generate(), groupId, "Large", Money(BigDecimal("5000")), 2)
        )
        val group = ModifierGroup(
            id = groupId,
            tenantId = tenantId,
            name = "Ukuran",
            options = options,
            sortOrder = 1
        )
        assertEquals(2, group.options.size)
        assertEquals("Regular", group.options[0].name)
        assertEquals("Large", group.options[1].name)
        assertEquals(groupId, group.options[0].groupId)
        assertEquals(groupId, group.options[1].groupId)
    }

    // ==================== MenuItemModifierLink ====================

    @Test
    fun `MenuItemModifierLink defaults`() {
        val link = MenuItemModifierLink(
            id = "link-1",
            menuItemId = ProductId.generate(),
            modifierGroupId = ModifierGroupId.generate()
        )
        assertEquals(0, link.sortOrder)
        assertFalse(link.isRequired)
        assertEquals(0, link.minSelection)
        assertEquals(1, link.maxSelection)
    }

    @Test
    fun `MenuItemModifierLink required with multi-select`() {
        val link = MenuItemModifierLink(
            id = "link-2",
            menuItemId = ProductId.generate(),
            modifierGroupId = ModifierGroupId.generate(),
            sortOrder = 1,
            isRequired = true,
            minSelection = 1,
            maxSelection = 3
        )
        assertTrue(link.isRequired)
        assertEquals(1, link.minSelection)
        assertEquals(3, link.maxSelection)
    }

    // ==================== MenuItem with modifierLinks ====================

    @Test
    fun `MenuItem with modifier links`() {
        val itemId = ProductId.generate()
        val groupId1 = ModifierGroupId.generate()
        val groupId2 = ModifierGroupId.generate()

        val links = listOf(
            MenuItemModifierLink("l1", itemId, groupId1, sortOrder = 1, isRequired = true),
            MenuItemModifierLink("l2", itemId, groupId2, sortOrder = 2, isRequired = false)
        )

        val item = MenuItem(
            id = itemId,
            tenantId = tenantId,
            categoryId = CategoryId.generate(),
            name = "Kopi",
            basePrice = Money(BigDecimal("20000")),
            modifierLinks = links
        )

        assertEquals(2, item.modifierLinks.size)
        assertTrue(item.modifierLinks[0].isRequired)
        assertFalse(item.modifierLinks[1].isRequired)
        assertEquals(itemId, item.modifierLinks[0].menuItemId)
    }
}
