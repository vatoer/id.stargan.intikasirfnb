package id.stargan.intikasirfnb.domain.transaction

import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.catalog.ProductRef
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.shared.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class TaxServiceChargeTipTest {

    private fun createSale() = Sale(
        id = SaleId.generate(),
        outletId = OutletId.generate(),
        channelId = SalesChannelId.generate()
    )

    private fun createLine(price: Long = 25000, qty: Int = 1) = OrderLine(
        productRef = ProductRef(ProductId.generate(), "Item", Money(BigDecimal(price)), null),
        quantity = qty,
        unitPrice = Money(BigDecimal(price))
    )

    // --- TaxLine.compute ---

    @Test
    fun `TaxLine compute exclusive tax`() {
        // PPN 11% on 100000 = 11000
        val tax = TaxLine.compute("PPN", BigDecimal("11"), false, Money(BigDecimal("100000")))
        assertEquals("PPN", tax.taxName)
        assertEquals(BigDecimal("11"), tax.taxRate)
        assertEquals(false, tax.isIncludedInPrice)
        assertEquals(BigDecimal("100000"), tax.taxableAmount.amount)
        assertEquals(BigDecimal("11000"), tax.taxAmount.amount)
    }

    @Test
    fun `TaxLine compute inclusive tax`() {
        // PPN 11% inclusive on 111000 → tax = 111000 * 11 / 111 = 11000
        val tax = TaxLine.compute("PPN", BigDecimal("11"), true, Money(BigDecimal("111000")))
        assertEquals(BigDecimal("11000"), tax.taxAmount.amount)
    }

    @Test
    fun `TaxLine compute PB1 10 percent exclusive`() {
        // PB1 10% on 50000 = 5000
        val tax = TaxLine.compute("PB1", BigDecimal("10"), false, Money(BigDecimal("50000")))
        assertEquals(BigDecimal("5000"), tax.taxAmount.amount)
    }

    @Test
    fun `TaxLine compute inclusive rounding`() {
        // 11% inclusive on 25000 → 25000 * 11 / 111 = 2477.477... → rounds to 2477
        val tax = TaxLine.compute("PPN", BigDecimal("11"), true, Money(BigDecimal("25000")))
        assertEquals(BigDecimal("2477"), tax.taxAmount.amount)
    }

    @Test
    fun `TaxLine compute zero amount`() {
        val tax = TaxLine.compute("PPN", BigDecimal("11"), false, Money.zero())
        assertEquals(BigDecimal("0"), tax.taxAmount.amount)
    }

    // --- ServiceChargeLine.compute ---

    @Test
    fun `ServiceChargeLine compute exclusive 5 percent`() {
        val sc = ServiceChargeLine.compute(BigDecimal("5"), false, Money(BigDecimal("100000")))
        assertEquals(BigDecimal("5"), sc.rate)
        assertEquals(false, sc.isIncludedInPrice)
        assertEquals(BigDecimal("100000"), sc.baseAmount.amount)
        assertEquals(BigDecimal("5000"), sc.chargeAmount.amount)
    }

    @Test
    fun `ServiceChargeLine compute inclusive 5 percent`() {
        // 5% inclusive on 105000 → 105000 * 5 / 105 = 5000
        val sc = ServiceChargeLine.compute(BigDecimal("5"), true, Money(BigDecimal("105000")))
        assertEquals(BigDecimal("5000"), sc.chargeAmount.amount)
    }

    @Test
    fun `ServiceChargeLine compute rounding`() {
        // 7.5% on 33333 → 33333 * 7.5 / 100 = 2499.975 → 2500
        val sc = ServiceChargeLine.compute(BigDecimal("7.5"), false, Money(BigDecimal("33333")))
        assertEquals(BigDecimal("2500"), sc.chargeAmount.amount)
    }

    // --- Sale with tax/SC/tip ---

    @Test
    fun `Sale defaults have empty tax SC tip`() {
        val sale = createSale()
        assertTrue(sale.taxLines.isEmpty())
        assertNull(sale.serviceCharge)
        assertNull(sale.tip)
        assertEquals(BigDecimal.ZERO, sale.taxTotal().amount)
        assertEquals(BigDecimal.ZERO, sale.serviceChargeAmount().amount)
        assertEquals(BigDecimal.ZERO, sale.tipAmount().amount)
    }

    @Test
    fun `totalAmount equals subtotal when no tax SC tip`() {
        val sale = createSale().addLine(createLine(25000, 2))
        assertEquals(sale.subtotal(), sale.totalAmount())
        assertEquals(BigDecimal("50000"), sale.totalAmount().amount)
    }

    @Test
    fun `totalAmount includes exclusive tax`() {
        val taxLine = TaxLine.compute("PPN", BigDecimal("11"), false, Money(BigDecimal("100000")))
        val sale = createSale()
            .addLine(createLine(100000))
            .applyTotals(taxLines = listOf(taxLine), serviceCharge = null)
        assertEquals(BigDecimal("100000"), sale.subtotal().amount)
        assertEquals(BigDecimal("11000"), sale.taxTotal().amount)
        assertEquals(BigDecimal("111000"), sale.totalAmount().amount)
    }

    @Test
    fun `totalAmount excludes inclusive tax`() {
        val taxLine = TaxLine.compute("PPN", BigDecimal("11"), true, Money(BigDecimal("100000")))
        val sale = createSale()
            .addLine(createLine(100000))
            .applyTotals(taxLines = listOf(taxLine), serviceCharge = null)
        assertEquals(BigDecimal("100000"), sale.subtotal().amount)
        assertEquals(BigDecimal.ZERO, sale.taxTotal().amount) // inclusive → not added
        assertEquals(BigDecimal("100000"), sale.totalAmount().amount)
        assertTrue(sale.inclusiveTaxTotal().amount > BigDecimal.ZERO) // but info is available
    }

    @Test
    fun `totalAmount includes exclusive service charge`() {
        val sc = ServiceChargeLine.compute(BigDecimal("5"), false, Money(BigDecimal("100000")))
        val sale = createSale()
            .addLine(createLine(100000))
            .applyTotals(taxLines = emptyList(), serviceCharge = sc)
        assertEquals(BigDecimal("5000"), sale.serviceChargeAmount().amount)
        assertEquals(BigDecimal("105000"), sale.totalAmount().amount)
    }

    @Test
    fun `totalAmount excludes inclusive service charge`() {
        val sc = ServiceChargeLine.compute(BigDecimal("5"), true, Money(BigDecimal("100000")))
        val sale = createSale()
            .addLine(createLine(100000))
            .applyTotals(taxLines = emptyList(), serviceCharge = sc)
        assertEquals(BigDecimal.ZERO, sale.serviceChargeAmount().amount)
        assertEquals(BigDecimal("100000"), sale.totalAmount().amount)
        assertTrue(sale.inclusiveServiceChargeAmount().amount > BigDecimal.ZERO)
    }

    @Test
    fun `totalAmount includes tip`() {
        val sale = createSale()
            .addLine(createLine(100000))
            .addTip(TipLine(Money(BigDecimal("10000"))))
        assertEquals(BigDecimal("10000"), sale.tipAmount().amount)
        assertEquals(BigDecimal("110000"), sale.totalAmount().amount)
    }

    @Test
    fun `totalAmount with exclusive tax plus SC plus tip`() {
        // subtotal=100000, PPN 11%=11000, SC 5%=5000, tip=10000
        val taxLine = TaxLine.compute("PPN", BigDecimal("11"), false, Money(BigDecimal("100000")))
        val sc = ServiceChargeLine.compute(BigDecimal("5"), false, Money(BigDecimal("100000")))
        val sale = createSale()
            .addLine(createLine(100000))
            .applyTotals(taxLines = listOf(taxLine), serviceCharge = sc)
            .addTip(TipLine(Money(BigDecimal("10000"))))
        assertEquals(BigDecimal("100000"), sale.subtotal().amount)
        assertEquals(BigDecimal("11000"), sale.taxTotal().amount)
        assertEquals(BigDecimal("5000"), sale.serviceChargeAmount().amount)
        assertEquals(BigDecimal("10000"), sale.tipAmount().amount)
        // 100000 + 11000 + 5000 + 10000 = 126000
        assertEquals(BigDecimal("126000"), sale.totalAmount().amount)
    }

    @Test
    fun `multiple tax lines accumulate`() {
        val ppn = TaxLine.compute("PPN", BigDecimal("11"), false, Money(BigDecimal("100000")))
        val pb1 = TaxLine.compute("PB1", BigDecimal("10"), false, Money(BigDecimal("100000")))
        val sale = createSale()
            .addLine(createLine(100000))
            .applyTotals(taxLines = listOf(ppn, pb1), serviceCharge = null)
        // 11000 + 10000 = 21000
        assertEquals(BigDecimal("21000"), sale.taxTotal().amount)
        assertEquals(BigDecimal("121000"), sale.totalAmount().amount)
    }

    @Test
    fun `mixed inclusive and exclusive taxes`() {
        val ppnExcl = TaxLine.compute("PPN", BigDecimal("11"), false, Money(BigDecimal("100000")))
        val pb1Incl = TaxLine.compute("PB1", BigDecimal("10"), true, Money(BigDecimal("100000")))
        val sale = createSale()
            .addLine(createLine(100000))
            .applyTotals(taxLines = listOf(ppnExcl, pb1Incl), serviceCharge = null)
        // Only exclusive tax adds to total
        assertEquals(BigDecimal("11000"), sale.taxTotal().amount)
        assertEquals(BigDecimal("111000"), sale.totalAmount().amount)
        // Inclusive tax info available
        assertEquals(BigDecimal("9091"), sale.inclusiveTaxTotal().amount)
    }

    // --- applyTotals / addTip state guards ---

    @Test
    fun `applyTotals works on draft sale`() {
        val sale = createSale().addLine(createLine(50000))
        val updated = sale.applyTotals(taxLines = emptyList(), serviceCharge = null)
        assertTrue(updated.taxLines.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `applyTotals fails on confirmed sale`() {
        createSale()
            .addLine(createLine(50000))
            .copy(status = SaleStatus.CONFIRMED)
            .applyTotals(taxLines = emptyList(), serviceCharge = null)
    }

    @Test
    fun `addTip works on draft sale`() {
        val sale = createSale().addTip(TipLine(Money(BigDecimal("5000"))))
        assertEquals(BigDecimal("5000"), sale.tipAmount().amount)
    }

    @Test
    fun `addTip works on confirmed sale`() {
        val sale = createSale()
            .addLine(createLine(25000))
            .copy(status = SaleStatus.CONFIRMED)
            .addTip(TipLine(Money(BigDecimal("5000"))))
        assertEquals(BigDecimal("5000"), sale.tipAmount().amount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addTip fails on paid sale`() {
        createSale()
            .addLine(createLine(10000))
            .copy(status = SaleStatus.PAID)
            .addTip(TipLine(Money(BigDecimal("5000"))))
    }

    @Test
    fun `removeTip clears tip`() {
        val sale = createSale()
            .addTip(TipLine(Money(BigDecimal("5000"))))
            .removeTip()
        assertNull(sale.tip)
        assertEquals(BigDecimal.ZERO, sale.tipAmount().amount)
    }

    @Test
    fun `addTip replaces existing tip`() {
        val sale = createSale()
            .addTip(TipLine(Money(BigDecimal("5000"))))
            .addTip(TipLine(Money(BigDecimal("10000"))))
        assertEquals(BigDecimal("10000"), sale.tipAmount().amount)
    }

    // --- Payment considers grandTotal with tax/SC/tip ---

    @Test
    fun `isFullyPaid considers tax and SC`() {
        val taxLine = TaxLine.compute("PPN", BigDecimal("10"), false, Money(BigDecimal("100000")))
        val sc = ServiceChargeLine.compute(BigDecimal("5"), false, Money(BigDecimal("100000")))
        val sale = createSale()
            .addLine(createLine(100000))
            .applyTotals(taxLines = listOf(taxLine), serviceCharge = sc)
            .copy(status = SaleStatus.CONFIRMED)
        // grandTotal = 100000 + 10000 + 5000 = 115000
        assertEquals(BigDecimal("115000"), sale.totalAmount().amount)

        val partial = sale.addPayment(Payment(method = PaymentMethod.CASH, amount = Money(BigDecimal("100000"))))
        assertEquals(SaleStatus.CONFIRMED, partial.status) // not enough

        val full = partial.addPayment(Payment(method = PaymentMethod.CASH, amount = Money(BigDecimal("15000"))))
        assertEquals(SaleStatus.PAID, full.status)
        assertTrue(full.isFullyPaid())
    }

    @Test
    fun `changeDue with tax and tip`() {
        val taxLine = TaxLine.compute("PPN", BigDecimal("10"), false, Money(BigDecimal("50000")))
        val sale = createSale()
            .addLine(createLine(50000))
            .applyTotals(taxLines = listOf(taxLine), serviceCharge = null)
            .addTip(TipLine(Money(BigDecimal("5000"))))
            .copy(status = SaleStatus.CONFIRMED)
        // grandTotal = 50000 + 5000 + 5000 = 60000
        assertEquals(BigDecimal("60000"), sale.totalAmount().amount)

        val paid = sale.addPayment(Payment(method = PaymentMethod.CASH, amount = Money(BigDecimal("100000"))))
        assertEquals(BigDecimal("40000"), paid.changeDue().amount)
    }

    // --- Real-world F&B scenario ---

    @Test
    fun `realistic restaurant bill`() {
        // 2x Nasi Goreng @25000 = 50000, 1x Es Teh @8000 = 8000
        // Subtotal = 58000
        // PB1 10% exclusive = 5800
        // SC 5% exclusive = 2900
        // Tip = 5000
        // Grand total = 58000 + 5800 + 2900 + 5000 = 71700
        val sale = createSale()
            .addLine(createLine(25000, 2))
            .addLine(createLine(8000, 1))

        assertEquals(BigDecimal("58000"), sale.subtotal().amount)

        val ppn = TaxLine.compute("PB1", BigDecimal("10"), false, Money(BigDecimal("58000")))
        val sc = ServiceChargeLine.compute(BigDecimal("5"), false, Money(BigDecimal("58000")))

        val calculated = sale
            .applyTotals(taxLines = listOf(ppn), serviceCharge = sc)
            .addTip(TipLine(Money(BigDecimal("5000"))))

        assertEquals(BigDecimal("5800"), calculated.taxTotal().amount)
        assertEquals(BigDecimal("2900"), calculated.serviceChargeAmount().amount)
        assertEquals(BigDecimal("5000"), calculated.tipAmount().amount)
        assertEquals(BigDecimal("71700"), calculated.totalAmount().amount)

        // Customer pays 80000 cash
        val confirmed = calculated.copy(status = SaleStatus.CONFIRMED)
        val paid = confirmed.addPayment(
            Payment(method = PaymentMethod.CASH, amount = Money(BigDecimal("80000")))
        )
        assertEquals(SaleStatus.PAID, paid.status)
        assertEquals(BigDecimal("8300"), paid.changeDue().amount)
    }
}
