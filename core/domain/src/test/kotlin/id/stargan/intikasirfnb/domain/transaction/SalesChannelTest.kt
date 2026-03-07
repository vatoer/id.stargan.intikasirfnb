package id.stargan.intikasirfnb.domain.transaction

import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.shared.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SalesChannelTest {

    private val tenantId = TenantId.generate()

    // --- ID uniqueness ---

    @Test
    fun `SalesChannelId generate creates unique ids`() {
        val id1 = SalesChannelId.generate()
        val id2 = SalesChannelId.generate()
        assertNotEquals(id1.value, id2.value)
    }

    // --- Factory methods ---

    @Test
    fun `dineIn factory creates correct channel`() {
        val ch = SalesChannel.dineIn(tenantId)
        assertEquals(ChannelType.DINE_IN, ch.channelType)
        assertEquals("Dine In", ch.name)
        assertEquals("DI", ch.code)
        assertTrue(ch.isActive)
        assertTrue(ch.requiresTable)
        assertFalse(ch.requiresExternalOrderId)
        assertNull(ch.platformConfig)
        assertNull(ch.priceAdjustmentType)
    }

    @Test
    fun `takeAway factory creates correct channel`() {
        val ch = SalesChannel.takeAway(tenantId)
        assertEquals(ChannelType.TAKE_AWAY, ch.channelType)
        assertEquals("Take Away", ch.name)
        assertEquals("TA", ch.code)
        assertFalse(ch.requiresTable)
        assertFalse(ch.requiresExternalOrderId)
    }

    // --- DELIVERY_PLATFORM requires platformConfig ---

    @Test
    fun `delivery platform with platformConfig succeeds`() {
        val ch = SalesChannel(
            id = SalesChannelId.generate(),
            tenantId = tenantId,
            channelType = ChannelType.DELIVERY_PLATFORM,
            name = "GoFood",
            code = "GF",
            platformConfig = PlatformConfig(
                platformName = "GoFood",
                commissionPercent = BigDecimal("20"),
                requiresExternalOrderId = true
            )
        )
        assertTrue(ch.requiresExternalOrderId)
        assertFalse(ch.requiresTable)
        assertEquals("GoFood", ch.platformConfig!!.platformName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `delivery platform without platformConfig fails`() {
        SalesChannel(
            id = SalesChannelId.generate(),
            tenantId = tenantId,
            channelType = ChannelType.DELIVERY_PLATFORM,
            name = "GoFood",
            code = "GF",
            platformConfig = null
        )
    }

    // --- Validation ---

    @Test(expected = IllegalArgumentException::class)
    fun `blank name fails`() {
        SalesChannel(
            id = SalesChannelId.generate(),
            tenantId = tenantId,
            channelType = ChannelType.TAKE_AWAY,
            name = "",
            code = "TA"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank code fails`() {
        SalesChannel(
            id = SalesChannelId.generate(),
            tenantId = tenantId,
            channelType = ChannelType.TAKE_AWAY,
            name = "Take Away",
            code = ""
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `adjustment type without value fails`() {
        SalesChannel(
            id = SalesChannelId.generate(),
            tenantId = tenantId,
            channelType = ChannelType.TAKE_AWAY,
            name = "Take Away",
            code = "TA",
            priceAdjustmentType = PriceAdjustmentType.MARKUP_PERCENT,
            priceAdjustmentValue = null
        )
    }

    // --- Price resolution ---

    @Test
    fun `resolvePrice with no adjustment returns base price`() {
        val ch = SalesChannel.dineIn(tenantId)
        val base = Money(BigDecimal("25000"))
        assertEquals(BigDecimal("25000"), ch.resolvePrice(base).amount)
    }

    @Test
    fun `resolvePrice MARKUP_PERCENT 20 percent`() {
        val ch = SalesChannel(
            id = SalesChannelId.generate(),
            tenantId = tenantId,
            channelType = ChannelType.DELIVERY_PLATFORM,
            name = "GoFood",
            code = "GF",
            priceAdjustmentType = PriceAdjustmentType.MARKUP_PERCENT,
            priceAdjustmentValue = BigDecimal("20"),
            platformConfig = PlatformConfig("GoFood", BigDecimal("20"))
        )
        val result = ch.resolvePrice(Money(BigDecimal("25000")))
        assertEquals(BigDecimal("30000"), result.amount)
    }

    @Test
    fun `resolvePrice MARKUP_FIXED adds fixed amount`() {
        val ch = SalesChannel(
            id = SalesChannelId.generate(),
            tenantId = tenantId,
            channelType = ChannelType.TAKE_AWAY,
            name = "Take Away",
            code = "TA",
            priceAdjustmentType = PriceAdjustmentType.MARKUP_FIXED,
            priceAdjustmentValue = BigDecimal("5000")
        )
        val result = ch.resolvePrice(Money(BigDecimal("25000")))
        assertEquals(BigDecimal("30000"), result.amount)
    }

    @Test
    fun `resolvePrice DISCOUNT_PERCENT 10 percent`() {
        val ch = SalesChannel(
            id = SalesChannelId.generate(),
            tenantId = tenantId,
            channelType = ChannelType.TAKE_AWAY,
            name = "Take Away Diskon",
            code = "TD",
            priceAdjustmentType = PriceAdjustmentType.DISCOUNT_PERCENT,
            priceAdjustmentValue = BigDecimal("10")
        )
        val result = ch.resolvePrice(Money(BigDecimal("25000")))
        assertEquals(BigDecimal("22500"), result.amount)
    }

    @Test
    fun `resolvePrice DISCOUNT_FIXED subtracts fixed amount`() {
        val ch = SalesChannel(
            id = SalesChannelId.generate(),
            tenantId = tenantId,
            channelType = ChannelType.TAKE_AWAY,
            name = "Take Away Diskon",
            code = "TD",
            priceAdjustmentType = PriceAdjustmentType.DISCOUNT_FIXED,
            priceAdjustmentValue = BigDecimal("3000")
        )
        val result = ch.resolvePrice(Money(BigDecimal("25000")))
        assertEquals(BigDecimal("22000"), result.amount)
    }

    // --- OWN_DELIVERY ---

    @Test
    fun `own delivery channel defaults`() {
        val ch = SalesChannel(
            id = SalesChannelId.generate(),
            tenantId = tenantId,
            channelType = ChannelType.OWN_DELIVERY,
            name = "Delivery Sendiri",
            code = "DS",
            priceAdjustmentType = PriceAdjustmentType.MARKUP_FIXED,
            priceAdjustmentValue = BigDecimal("5000")
        )
        assertFalse(ch.requiresTable)
        assertFalse(ch.requiresExternalOrderId)
        assertNull(ch.platformConfig)
        val result = ch.resolvePrice(Money(BigDecimal("20000")))
        assertEquals(BigDecimal("25000"), result.amount)
    }

    // --- ChannelType enum ---

    @Test
    fun `ChannelType has 4 values`() {
        assertEquals(4, ChannelType.entries.size)
        assertTrue(ChannelType.entries.map { it.name }.containsAll(
            listOf("DINE_IN", "TAKE_AWAY", "DELIVERY_PLATFORM", "OWN_DELIVERY")
        ))
    }

    // --- PriceAdjustmentType enum ---

    @Test
    fun `PriceAdjustmentType has 4 values`() {
        assertEquals(4, PriceAdjustmentType.entries.size)
    }
}
