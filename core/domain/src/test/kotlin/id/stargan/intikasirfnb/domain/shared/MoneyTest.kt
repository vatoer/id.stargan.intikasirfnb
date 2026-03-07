package id.stargan.intikasirfnb.domain.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun `zero returns zero amount`() {
        val money = Money.zero()
        assertTrue(money.isZero())
        assertEquals("IDR", money.currencyCode)
    }

    @Test
    fun `plus adds amounts`() {
        val a = Money(BigDecimal("10000"), "IDR")
        val b = Money(BigDecimal("5000"), "IDR")
        val result = a + b
        assertEquals(BigDecimal("15000"), result.amount)
    }

    @Test
    fun `minus subtracts amounts`() {
        val a = Money(BigDecimal("10000"), "IDR")
        val b = Money(BigDecimal("3000"), "IDR")
        val result = a - b
        assertEquals(BigDecimal("7000"), result.amount)
    }

    @Test
    fun `times multiplies by factor`() {
        val money = Money(BigDecimal("5000"), "IDR")
        val result = money * 3
        assertEquals(BigDecimal("15000"), result.amount)
    }

    @Test
    fun `isPositive returns true for positive amounts`() {
        assertTrue(Money(BigDecimal("100"), "IDR").isPositive())
        assertFalse(Money(BigDecimal.ZERO, "IDR").isPositive())
        assertFalse(Money(BigDecimal("-100"), "IDR").isPositive())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cannot add different currencies`() {
        Money(BigDecimal("100"), "IDR") + Money(BigDecimal("100"), "USD")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cannot subtract different currencies`() {
        Money(BigDecimal("100"), "IDR") - Money(BigDecimal("100"), "USD")
    }
}
