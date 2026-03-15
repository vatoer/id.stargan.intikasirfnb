package id.stargan.intikasirfnb.domain.catalog

import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.shared.UlidGenerator
import id.stargan.intikasirfnb.domain.transaction.OrderLine
import id.stargan.intikasirfnb.domain.transaction.OrderLineId
import id.stargan.intikasirfnb.domain.transaction.SelectedAddOn
import id.stargan.intikasirfnb.domain.transaction.SelectedModifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit tests for AddOnGroup domain models, SelectedAddOn VO,
 * and OrderLine price calculations with add-ons (1.4.25).
 */
class AddOnModelTest {

    private val tenantId = TenantId.generate()

    // ==================== ID generation ====================

    @Test
    fun `AddOnGroupId generate creates unique ids`() {
        val id1 = AddOnGroupId.generate()
        val id2 = AddOnGroupId.generate()
        assertNotEquals(id1.value, id2.value)
        assertTrue(id1.value.isNotBlank())
    }

    @Test
    fun `AddOnItemId generate creates unique ids`() {
        val id1 = AddOnItemId.generate()
        val id2 = AddOnItemId.generate()
        assertNotEquals(id1.value, id2.value)
    }

    // ==================== AddOnItem ====================

    @Test
    fun `AddOnItem defaults`() {
        val groupId = AddOnGroupId.generate()
        val item = AddOnItem(
            id = AddOnItemId.generate(),
            groupId = groupId,
            name = "Telur Ceplok"
        )
        assertTrue(item.price.isZero())
        assertEquals(5, item.maxQty)
        assertNull(item.inventoryItemId)
        assertEquals(0, item.sortOrder)
        assertTrue(item.isActive)
    }

    @Test
    fun `AddOnItem with price and maxQty`() {
        val groupId = AddOnGroupId.generate()
        val item = AddOnItem(
            id = AddOnItemId.generate(),
            groupId = groupId,
            name = "Extra Cheese",
            price = Money(BigDecimal("5000")),
            maxQty = 3,
            sortOrder = 2
        )
        assertEquals("Extra Cheese", item.name)
        assertEquals(BigDecimal("5000"), item.price.amount)
        assertEquals(3, item.maxQty)
        assertEquals(2, item.sortOrder)
    }

    @Test
    fun `AddOnItem with inventoryItemId`() {
        val item = AddOnItem(
            id = AddOnItemId.generate(),
            groupId = AddOnGroupId.generate(),
            name = "Sosis",
            price = Money(BigDecimal("8000")),
            inventoryItemId = "inv-sosis-001"
        )
        assertEquals("inv-sosis-001", item.inventoryItemId)
    }

    // ==================== AddOnGroup ====================

    @Test
    fun `AddOnGroup defaults`() {
        val group = AddOnGroup(
            id = AddOnGroupId.generate(),
            tenantId = tenantId,
            name = "Topping"
        )
        assertTrue(group.items.isEmpty())
        assertEquals(0, group.sortOrder)
        assertTrue(group.isActive)
    }

    @Test
    fun `AddOnGroup with items`() {
        val groupId = AddOnGroupId.generate()
        val items = listOf(
            AddOnItem(AddOnItemId.generate(), groupId, "Telur", Money(BigDecimal("5000")), sortOrder = 1),
            AddOnItem(AddOnItemId.generate(), groupId, "Keju", Money(BigDecimal("7000")), sortOrder = 2),
            AddOnItem(AddOnItemId.generate(), groupId, "Sosis", Money(BigDecimal("8000")), sortOrder = 3)
        )
        val group = AddOnGroup(
            id = groupId,
            tenantId = tenantId,
            name = "Extra",
            items = items,
            sortOrder = 1
        )
        assertEquals(3, group.items.size)
        assertEquals("Telur", group.items[0].name)
        assertEquals(groupId, group.items[0].groupId)
        assertEquals(groupId, group.items[2].groupId)
    }

    @Test
    fun `AddOnGroup copy changes isActive`() {
        val group = AddOnGroup(
            id = AddOnGroupId.generate(),
            tenantId = tenantId,
            name = "Topping",
            isActive = true
        )
        val inactive = group.copy(isActive = false)
        assertFalse(inactive.isActive)
        assertEquals(group.id, inactive.id)
    }

    // ==================== MenuItemAddOnLink ====================

    @Test
    fun `MenuItemAddOnLink defaults`() {
        val link = MenuItemAddOnLink(
            id = UlidGenerator.generate(),
            menuItemId = ProductId.generate(),
            addOnGroupId = AddOnGroupId.generate()
        )
        assertEquals(0, link.sortOrder)
    }

    @Test
    fun `MenuItemAddOnLink with sortOrder`() {
        val link = MenuItemAddOnLink(
            id = "link-1",
            menuItemId = ProductId.generate(),
            addOnGroupId = AddOnGroupId.generate(),
            sortOrder = 3
        )
        assertEquals(3, link.sortOrder)
    }

    // ==================== SelectedAddOn ====================

    @Test
    fun `SelectedAddOn totalPrice computed from unitPrice x quantity`() {
        val addOn = SelectedAddOn(
            addOnName = "Telur Ceplok",
            quantity = 2,
            unitPrice = Money(BigDecimal("5000"))
        )
        assertEquals(Money(BigDecimal("10000")), addOn.totalPrice)
    }

    @Test
    fun `SelectedAddOn quantity 1 totalPrice equals unitPrice`() {
        val addOn = SelectedAddOn(
            addOnName = "Keju",
            quantity = 1,
            unitPrice = Money(BigDecimal("7000"))
        )
        assertEquals(Money(BigDecimal("7000")), addOn.totalPrice)
    }

    @Test
    fun `SelectedAddOn explicit totalPrice overrides computed`() {
        val addOn = SelectedAddOn(
            addOnName = "Promo Sosis",
            quantity = 2,
            unitPrice = Money(BigDecimal("8000")),
            totalPrice = Money(BigDecimal("12000")) // discounted
        )
        assertEquals(Money(BigDecimal("12000")), addOn.totalPrice)
    }

    // ==================== OrderLine with AddOns ====================

    private val baseProductRef = ProductRef(
        productId = ProductId.generate(),
        name = "Nasi Goreng",
        price = Money(BigDecimal("25000"))
    )

    @Test
    fun `OrderLine addOnTotal sums all add-on totalPrices`() {
        val line = OrderLine(
            productRef = baseProductRef,
            quantity = 1,
            unitPrice = baseProductRef.price,
            selectedAddOns = listOf(
                SelectedAddOn("Telur", 1, Money(BigDecimal("5000"))),
                SelectedAddOn("Keju", 2, Money(BigDecimal("3000")))
            )
        )
        // addOnTotal = 5000 + 6000 = 11000
        assertEquals(Money(BigDecimal("11000")), line.addOnTotal())
    }

    @Test
    fun `OrderLine addOnTotal zero when no add-ons`() {
        val line = OrderLine(
            productRef = baseProductRef,
            quantity = 1,
            unitPrice = baseProductRef.price
        )
        assertTrue(line.addOnTotal().isZero())
    }

    @Test
    fun `OrderLine lineTotal includes add-ons`() {
        val line = OrderLine(
            productRef = baseProductRef,
            quantity = 1,
            unitPrice = baseProductRef.price,
            selectedAddOns = listOf(
                SelectedAddOn("Telur", 1, Money(BigDecimal("5000")))
            )
        )
        // lineTotal = (25000 * 1) + 5000 = 30000
        assertEquals(Money(BigDecimal("30000")), line.lineTotal())
    }

    @Test
    fun `OrderLine lineTotal addOns NOT multiplied by quantity`() {
        val line = OrderLine(
            productRef = baseProductRef,
            quantity = 3,
            unitPrice = baseProductRef.price,
            selectedAddOns = listOf(
                SelectedAddOn("Telur", 1, Money(BigDecimal("5000")))
            )
        )
        // lineTotal = (25000 * 3) + 5000 = 80000
        // NOT (25000 + 5000) * 3 = 90000
        assertEquals(Money(BigDecimal("80000")), line.lineTotal())
    }

    @Test
    fun `OrderLine lineTotal with modifiers AND add-ons`() {
        val line = OrderLine(
            productRef = baseProductRef,
            quantity = 2,
            unitPrice = baseProductRef.price,
            selectedModifiers = listOf(
                SelectedModifier("Ukuran", "Large", Money(BigDecimal("5000")))
            ),
            selectedAddOns = listOf(
                SelectedAddOn("Telur", 1, Money(BigDecimal("5000"))),
                SelectedAddOn("Keju", 2, Money(BigDecimal("3000")))
            )
        )
        // effectiveUnitPrice = 25000 + 5000 = 30000
        // lineTotal = (30000 * 2) + (5000 + 6000) = 60000 + 11000 = 71000
        assertEquals(Money(BigDecimal("30000")), line.effectiveUnitPrice())
        assertEquals(Money(BigDecimal("5000")), line.modifierTotal())
        assertEquals(Money(BigDecimal("11000")), line.addOnTotal())
        assertEquals(Money(BigDecimal("71000")), line.lineTotal())
    }

    @Test
    fun `OrderLine lineTotal with modifiers, add-ons, and discount`() {
        val line = OrderLine(
            productRef = baseProductRef,
            quantity = 1,
            unitPrice = baseProductRef.price,
            discountAmount = Money(BigDecimal("5000")),
            selectedModifiers = listOf(
                SelectedModifier("Ukuran", "Large", Money(BigDecimal("5000")))
            ),
            selectedAddOns = listOf(
                SelectedAddOn("Telur", 1, Money(BigDecimal("5000")))
            )
        )
        // effectiveUnitPrice = 25000 + 5000 = 30000
        // lineTotal = (30000 * 1) + 5000 - 5000 = 30000
        assertEquals(Money(BigDecimal("30000")), line.lineTotal())
    }

    @Test
    fun `OrderLine lineTotal with only free modifiers and add-ons`() {
        val line = OrderLine(
            productRef = baseProductRef,
            quantity = 1,
            unitPrice = baseProductRef.price,
            selectedModifiers = listOf(
                SelectedModifier("Level Pedas", "Extra Pedas", Money.zero())
            ),
            selectedAddOns = listOf(
                SelectedAddOn("Sambal", 1, Money.zero())
            )
        )
        // No price changes
        assertEquals(Money(BigDecimal("25000")), line.lineTotal())
        assertTrue(line.modifierTotal().isZero())
        assertTrue(line.addOnTotal().isZero())
    }

    // ==================== MenuItem with AddOn Links ====================

    @Test
    fun `MenuItem with addOnLinks`() {
        val itemId = ProductId.generate()
        val groupId1 = AddOnGroupId.generate()
        val groupId2 = AddOnGroupId.generate()

        val links = listOf(
            MenuItemAddOnLink("l1", itemId, groupId1, sortOrder = 1),
            MenuItemAddOnLink("l2", itemId, groupId2, sortOrder = 2)
        )

        val item = MenuItem(
            id = itemId,
            tenantId = tenantId,
            categoryId = CategoryId.generate(),
            name = "Nasi Goreng",
            basePrice = Money(BigDecimal("25000")),
            addOnLinks = links
        )

        assertEquals(2, item.addOnLinks.size)
        assertEquals(1, item.addOnLinks[0].sortOrder)
        assertEquals(2, item.addOnLinks[1].sortOrder)
    }
}
