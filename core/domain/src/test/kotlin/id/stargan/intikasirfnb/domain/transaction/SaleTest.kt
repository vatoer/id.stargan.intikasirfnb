package id.stargan.intikasirfnb.domain.transaction

import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.catalog.ProductRef
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.shared.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SaleTest {

    private fun createSale() = Sale(
        id = SaleId.generate(),
        outletId = OutletId.generate(),
        channelId = SalesChannelId.generate()
    )

    private fun createOrderLine(price: Long = 25000, qty: Int = 1) = OrderLine(
        productRef = ProductRef(
            productId = ProductId.generate(),
            name = "Nasi Goreng",
            price = Money(BigDecimal(price), "IDR"),
            taxCode = null
        ),
        quantity = qty,
        unitPrice = Money(BigDecimal(price), "IDR")
    )

    // --- Basic state ---

    @Test
    fun `new sale is DRAFT`() {
        val sale = createSale()
        assertEquals(SaleStatus.DRAFT, sale.status)
    }

    @Test
    fun `addLine adds to draft sale`() {
        val sale = createSale().addLine(createOrderLine())
        assertEquals(1, sale.lines.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addLine fails on confirmed sale`() {
        createSale()
            .addLine(createOrderLine())
            .confirm()
            .addLine(createOrderLine())
    }

    @Test
    fun `confirm changes status to CONFIRMED`() {
        val sale = createSale().addLine(createOrderLine()).confirm()
        assertEquals(SaleStatus.CONFIRMED, sale.status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `confirm fails on empty sale`() {
        createSale().confirm()
    }

    @Test
    fun `totalAmount sums all lines`() {
        val sale = createSale()
            .addLine(createOrderLine(price = 25000, qty = 2))
            .addLine(createOrderLine(price = 15000, qty = 1))
        assertEquals(BigDecimal("65000"), sale.totalAmount().amount)
    }

    @Test
    fun `addPayment with full amount transitions to PAID`() {
        val sale = createSale()
            .addLine(createOrderLine(price = 25000, qty = 1))
            .confirm()
            .addPayment(Payment(method = PaymentMethod.CASH, amount = Money(BigDecimal("25000"), "IDR")))
        assertEquals(SaleStatus.PAID, sale.status)
    }

    @Test
    fun `complete works on paid sale`() {
        val sale = createSale()
            .addLine(createOrderLine(price = 10000))
            .confirm()
            .addPayment(Payment(method = PaymentMethod.CASH, amount = Money(BigDecimal("10000"), "IDR")))
            .complete()
        assertEquals(SaleStatus.COMPLETED, sale.status)
    }

    @Test
    fun `void works on draft`() {
        val sale = createSale().void()
        assertEquals(SaleStatus.VOIDED, sale.status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `void fails on paid sale`() {
        createSale()
            .addLine(createOrderLine(price = 10000))
            .confirm()
            .addPayment(Payment(method = PaymentMethod.CASH, amount = Money(BigDecimal("10000"), "IDR")))
            .void()
    }

    @Test
    fun `isFullyPaid returns true when total paid covers total amount`() {
        val sale = createSale()
            .addLine(createOrderLine(price = 10000))
            .confirm()
            .addPayment(Payment(method = PaymentMethod.CASH, amount = Money(BigDecimal("10000"), "IDR")))
        assertTrue(sale.isFullyPaid())
    }

    // --- ID uniqueness ---

    @Test
    fun `SaleId generate creates unique ids`() {
        val id1 = SaleId.generate()
        val id2 = SaleId.generate()
        assertNotEquals(id1.value, id2.value)
    }

    @Test
    fun `OrderLineId generate creates unique ids`() {
        val id1 = OrderLineId.generate()
        val id2 = OrderLineId.generate()
        assertNotEquals(id1.value, id2.value)
    }

    @Test
    fun `PaymentId generate creates unique ids`() {
        val id1 = PaymentId.generate()
        val id2 = PaymentId.generate()
        assertNotEquals(id1.value, id2.value)
    }

    @Test
    fun `CashierSessionId generate creates unique ids`() {
        val id1 = CashierSessionId.generate()
        val id2 = CashierSessionId.generate()
        assertNotEquals(id1.value, id2.value)
    }

    // --- OrderLine with modifiers ---

    @Test
    fun `OrderLine lineTotal includes modifier price deltas`() {
        val line = OrderLine(
            productRef = ProductRef(ProductId.generate(), "Kopi", Money(BigDecimal("20000")), null),
            quantity = 2,
            unitPrice = Money(BigDecimal("20000")),
            selectedModifiers = listOf(
                SelectedModifier("Size", "Large", Money(BigDecimal("5000"))),
                SelectedModifier("Topping", "Cheese", Money(BigDecimal("3000")))
            )
        )
        // effectiveUnit = 20000 + 5000 + 3000 = 28000
        // lineTotal = 28000 * 2 = 56000
        assertEquals(BigDecimal("56000"), line.lineTotal().amount)
    }

    @Test
    fun `OrderLine modifierTotal sums all deltas`() {
        val line = OrderLine(
            productRef = ProductRef(ProductId.generate(), "Test", Money(BigDecimal("10000")), null),
            quantity = 1,
            unitPrice = Money(BigDecimal("10000")),
            selectedModifiers = listOf(
                SelectedModifier("A", "Opt1", Money(BigDecimal("2000"))),
                SelectedModifier("B", "Opt2", Money(BigDecimal("3000")))
            )
        )
        assertEquals(BigDecimal("5000"), line.modifierTotal().amount)
        assertEquals(BigDecimal("15000"), line.effectiveUnitPrice().amount)
    }

    @Test
    fun `OrderLine without modifiers works as before`() {
        val line = createOrderLine(price = 25000, qty = 3)
        assertEquals(BigDecimal("75000"), line.lineTotal().amount)
        assertTrue(line.selectedModifiers.isEmpty())
    }

    // --- Sale updateLine / removeLine ---

    @Test
    fun `updateLine changes quantity`() {
        val line = createOrderLine(price = 10000, qty = 1)
        val sale = createSale().addLine(line)
        val updated = sale.updateLine(line.id) { it.copy(quantity = 3) }
        assertEquals(3, updated.lines[0].quantity)
        assertEquals(BigDecimal("30000"), updated.totalAmount().amount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateLine fails on confirmed sale`() {
        val line = createOrderLine()
        createSale().addLine(line).confirm().updateLine(line.id) { it.copy(quantity = 2) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateLine fails for unknown lineId`() {
        createSale().addLine(createOrderLine()).updateLine(OrderLineId.generate()) { it }
    }

    @Test
    fun `removeLine removes the line`() {
        val line1 = createOrderLine(price = 10000)
        val line2 = createOrderLine(price = 20000)
        val sale = createSale().addLine(line1).addLine(line2)
        val updated = sale.removeLine(line1.id)
        assertEquals(1, updated.lines.size)
        assertEquals(line2.id, updated.lines[0].id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `removeLine fails on confirmed sale`() {
        val line = createOrderLine()
        createSale().addLine(line).confirm().removeLine(line.id)
    }

    // --- Sale subtotal and changeDue ---

    @Test
    fun `subtotal equals totalAmount without tax`() {
        val sale = createSale()
            .addLine(createOrderLine(price = 10000, qty = 2))
            .addLine(createOrderLine(price = 5000, qty = 1))
        assertEquals(BigDecimal("25000"), sale.subtotal().amount)
        assertEquals(sale.subtotal(), sale.totalAmount())
    }

    @Test
    fun `changeDue calculates overpayment`() {
        val sale = createSale()
            .addLine(createOrderLine(price = 15000))
            .confirm()
            .addPayment(Payment(method = PaymentMethod.CASH, amount = Money(BigDecimal("20000"), "IDR")))
        assertEquals(BigDecimal("5000"), sale.changeDue().amount)
    }

    @Test
    fun `changeDue is zero when exact payment`() {
        val sale = createSale()
            .addLine(createOrderLine(price = 10000))
            .confirm()
            .addPayment(Payment(method = PaymentMethod.CASH, amount = Money(BigDecimal("10000"), "IDR")))
        assertEquals(BigDecimal.ZERO, sale.changeDue().amount)
    }

    // --- Sale notes and receiptNumber ---

    @Test
    fun `sale notes and receiptNumber defaults`() {
        val sale = createSale()
        assertNull(sale.notes)
        assertNull(sale.receiptNumber)
    }

    @Test
    fun `sale with notes`() {
        val sale = createSale().copy(notes = "Extra spicy")
        assertEquals("Extra spicy", sale.notes)
    }

    // --- Payment with ID ---

    @Test
    fun `Payment has auto-generated id`() {
        val p1 = Payment(method = PaymentMethod.CASH, amount = Money(BigDecimal("10000")))
        val p2 = Payment(method = PaymentMethod.CASH, amount = Money(BigDecimal("10000")))
        assertNotEquals(p1.id.value, p2.id.value)
        assertTrue(p1.id.value.isNotBlank())
    }

    // --- CashierSession ---

    @Test
    fun `CashierSession defaults`() {
        val session = CashierSession(
            id = CashierSessionId.generate(),
            terminalId = id.stargan.intikasirfnb.domain.identity.TerminalId.generate(),
            outletId = OutletId.generate(),
            userId = id.stargan.intikasirfnb.domain.identity.UserId.generate(),
            openAtMillis = System.currentTimeMillis(),
            openingFloat = Money(BigDecimal("500000"))
        )
        assertEquals(CashierSessionStatus.OPEN, session.status)
        assertNull(session.closeAtMillis)
        assertNull(session.closingCash)
        assertNull(session.expectedCash)
        assertNull(session.cashDifference())
    }

    @Test
    fun `CashierSession cashDifference when closed`() {
        val session = CashierSession(
            id = CashierSessionId.generate(),
            terminalId = id.stargan.intikasirfnb.domain.identity.TerminalId.generate(),
            outletId = OutletId.generate(),
            userId = id.stargan.intikasirfnb.domain.identity.UserId.generate(),
            openAtMillis = System.currentTimeMillis(),
            openingFloat = Money(BigDecimal("500000")),
            closingCash = Money(BigDecimal("750000")),
            expectedCash = Money(BigDecimal("800000")),
            status = CashierSessionStatus.CLOSED
        )
        val diff = session.cashDifference()!!
        // 750000 - 800000 = -50000 (short)
        assertEquals(BigDecimal("-50000"), diff.amount)
    }

    @Test
    fun `CashierSession cashDifference positive means over`() {
        val session = CashierSession(
            id = CashierSessionId.generate(),
            terminalId = id.stargan.intikasirfnb.domain.identity.TerminalId.generate(),
            outletId = OutletId.generate(),
            userId = id.stargan.intikasirfnb.domain.identity.UserId.generate(),
            openAtMillis = System.currentTimeMillis(),
            openingFloat = Money(BigDecimal("500000")),
            closingCash = Money(BigDecimal("850000")),
            expectedCash = Money(BigDecimal("800000")),
            status = CashierSessionStatus.CLOSED
        )
        assertEquals(BigDecimal("50000"), session.cashDifference()!!.amount)
    }

    // --- Multiple payments ---

    @Test
    fun `multiple payments accumulate`() {
        val sale = createSale()
            .addLine(createOrderLine(price = 50000))
            .confirm()
            .addPayment(Payment(method = PaymentMethod.CASH, amount = Money(BigDecimal("20000"), "IDR")))
        assertFalse(sale.isFullyPaid())
        assertEquals(SaleStatus.CONFIRMED, sale.status)

        val paid = sale.addPayment(Payment(method = PaymentMethod.E_WALLET, amount = Money(BigDecimal("30000"), "IDR")))
        assertTrue(paid.isFullyPaid())
        assertEquals(SaleStatus.PAID, paid.status)
        assertEquals(2, paid.payments.size)
    }

    // --- OrderLine discount ---

    @Test
    fun `OrderLine lineTotal with discount`() {
        val line = OrderLine(
            productRef = ProductRef(ProductId.generate(), "Item", Money(BigDecimal("20000")), null),
            quantity = 2,
            unitPrice = Money(BigDecimal("20000")),
            discountAmount = Money(BigDecimal("5000"))
        )
        // (20000 * 2) - 5000 = 35000
        assertEquals(BigDecimal("35000"), line.lineTotal().amount)
    }
}
