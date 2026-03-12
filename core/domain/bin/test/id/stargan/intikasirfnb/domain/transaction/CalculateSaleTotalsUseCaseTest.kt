package id.stargan.intikasirfnb.domain.transaction

import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.catalog.ProductRef
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.settings.OutletSettings
import id.stargan.intikasirfnb.domain.settings.OutletSettingsRepository
import id.stargan.intikasirfnb.domain.settings.ServiceChargeConfig
import id.stargan.intikasirfnb.domain.settings.TaxConfig
import id.stargan.intikasirfnb.domain.settings.TaxConfigId
import id.stargan.intikasirfnb.domain.settings.TaxConfigRepository
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.usecase.transaction.CalculateSaleTotalsUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CalculateSaleTotalsUseCaseTest {

    private val tenantId = TenantId("test-tenant")
    private val outletId = OutletId("test-outlet")

    private fun createSale(lines: List<OrderLine> = emptyList()) = Sale(
        id = SaleId.generate(),
        outletId = outletId,
        channelId = SalesChannelId.generate(),
        lines = lines
    )

    private fun createLine(price: Long, qty: Int = 1) = OrderLine(
        productRef = ProductRef(ProductId.generate(), "Item", Money(BigDecimal(price)), null),
        quantity = qty,
        unitPrice = Money(BigDecimal(price))
    )

    // --- Fake repositories ---

    private class FakeTaxConfigRepo(private val configs: List<TaxConfig> = emptyList()) : TaxConfigRepository {
        override suspend fun getById(id: TaxConfigId) = configs.find { it.id == id }
        override suspend fun getActiveByTenant(tenantId: TenantId) = configs.filter { it.isActive }
        override suspend fun getAllByTenant(tenantId: TenantId) = configs
        override suspend fun save(taxConfig: TaxConfig) {}
        override suspend fun delete(id: TaxConfigId) {}
    }

    private class FakeOutletSettingsRepo(private val settings: OutletSettings? = null) : OutletSettingsRepository {
        override suspend fun getByOutletId(outletId: OutletId) = settings
        override suspend fun save(settings: OutletSettings) {}
    }

    // --- Tests ---

    @Test
    fun `no tax no SC returns unchanged sale`() = runBlocking {
        val useCase = CalculateSaleTotalsUseCase(FakeTaxConfigRepo(), FakeOutletSettingsRepo())
        val sale = createSale(listOf(createLine(50000)))
        val result = useCase(sale, tenantId).getOrThrow()
        assertTrue(result.taxLines.isEmpty())
        assertNull(result.serviceCharge)
        assertEquals(BigDecimal("50000"), result.totalAmount().amount)
    }

    @Test
    fun `single exclusive tax applied`() = runBlocking {
        val taxRepo = FakeTaxConfigRepo(listOf(
            TaxConfig(TaxConfigId.generate(), tenantId, "PPN", BigDecimal("11"), isActive = true)
        ))
        val useCase = CalculateSaleTotalsUseCase(taxRepo, FakeOutletSettingsRepo())
        val sale = createSale(listOf(createLine(100000)))
        val result = useCase(sale, tenantId).getOrThrow()

        assertEquals(1, result.taxLines.size)
        assertEquals("PPN", result.taxLines[0].taxName)
        assertEquals(BigDecimal("11000"), result.taxLines[0].taxAmount.amount)
        assertEquals(BigDecimal("111000"), result.totalAmount().amount)
    }

    @Test
    fun `inclusive tax does not increase total`() = runBlocking {
        val taxRepo = FakeTaxConfigRepo(listOf(
            TaxConfig(TaxConfigId.generate(), tenantId, "PPN", BigDecimal("11"),
                isIncludedInPrice = true, isActive = true)
        ))
        val useCase = CalculateSaleTotalsUseCase(taxRepo, FakeOutletSettingsRepo())
        val sale = createSale(listOf(createLine(100000)))
        val result = useCase(sale, tenantId).getOrThrow()

        assertEquals(1, result.taxLines.size)
        assertTrue(result.taxLines[0].isIncludedInPrice)
        assertEquals(BigDecimal("100000"), result.totalAmount().amount) // unchanged
    }

    @Test
    fun `service charge applied when enabled`() = runBlocking {
        val settings = OutletSettings(
            outletId = outletId,
            tenantId = tenantId,
            serviceCharge = ServiceChargeConfig(isEnabled = true, rate = BigDecimal("5"))
        )
        val useCase = CalculateSaleTotalsUseCase(FakeTaxConfigRepo(), FakeOutletSettingsRepo(settings))
        val sale = createSale(listOf(createLine(100000)))
        val result = useCase(sale, tenantId).getOrThrow()

        assertNotNull(result.serviceCharge)
        assertEquals(BigDecimal("5000"), result.serviceCharge!!.chargeAmount.amount)
        assertEquals(BigDecimal("105000"), result.totalAmount().amount)
    }

    @Test
    fun `SC not applied when disabled`() = runBlocking {
        val settings = OutletSettings(
            outletId = outletId,
            tenantId = tenantId,
            serviceCharge = ServiceChargeConfig(isEnabled = false, rate = BigDecimal("5"))
        )
        val useCase = CalculateSaleTotalsUseCase(FakeTaxConfigRepo(), FakeOutletSettingsRepo(settings))
        val sale = createSale(listOf(createLine(100000)))
        val result = useCase(sale, tenantId).getOrThrow()

        assertNull(result.serviceCharge)
        assertEquals(BigDecimal("100000"), result.totalAmount().amount)
    }

    @Test
    fun `tax plus SC combined`() = runBlocking {
        val taxRepo = FakeTaxConfigRepo(listOf(
            TaxConfig(TaxConfigId.generate(), tenantId, "PB1", BigDecimal("10"), isActive = true)
        ))
        val settings = OutletSettings(
            outletId = outletId,
            tenantId = tenantId,
            serviceCharge = ServiceChargeConfig(isEnabled = true, rate = BigDecimal("5"))
        )
        val useCase = CalculateSaleTotalsUseCase(taxRepo, FakeOutletSettingsRepo(settings))
        val sale = createSale(listOf(createLine(80000)))
        val result = useCase(sale, tenantId).getOrThrow()

        // subtotal = 80000
        // PB1 10% = 8000
        // SC 5% = 4000
        // total = 80000 + 8000 + 4000 = 92000
        assertEquals(BigDecimal("80000"), result.subtotal().amount)
        assertEquals(BigDecimal("8000"), result.taxTotal().amount)
        assertEquals(BigDecimal("4000"), result.serviceChargeAmount().amount)
        assertEquals(BigDecimal("92000"), result.totalAmount().amount)
    }

    @Test
    fun `multiple taxes applied`() = runBlocking {
        val taxRepo = FakeTaxConfigRepo(listOf(
            TaxConfig(TaxConfigId.generate(), tenantId, "PPN", BigDecimal("11"), isActive = true),
            TaxConfig(TaxConfigId.generate(), tenantId, "PB1", BigDecimal("10"), isActive = true)
        ))
        val useCase = CalculateSaleTotalsUseCase(taxRepo, FakeOutletSettingsRepo())
        val sale = createSale(listOf(createLine(100000)))
        val result = useCase(sale, tenantId).getOrThrow()

        assertEquals(2, result.taxLines.size)
        // 11000 + 10000 = 21000
        assertEquals(BigDecimal("21000"), result.taxTotal().amount)
        assertEquals(BigDecimal("121000"), result.totalAmount().amount)
    }

    @Test
    fun `inactive tax not applied`() = runBlocking {
        val taxRepo = FakeTaxConfigRepo(listOf(
            TaxConfig(TaxConfigId.generate(), tenantId, "PPN", BigDecimal("11"), isActive = false)
        ))
        val useCase = CalculateSaleTotalsUseCase(taxRepo, FakeOutletSettingsRepo())
        val sale = createSale(listOf(createLine(100000)))
        val result = useCase(sale, tenantId).getOrThrow()

        assertTrue(result.taxLines.isEmpty())
    }

    @Test
    fun `null outlet settings means no SC`() = runBlocking {
        val useCase = CalculateSaleTotalsUseCase(FakeTaxConfigRepo(), FakeOutletSettingsRepo(null))
        val sale = createSale(listOf(createLine(50000)))
        val result = useCase(sale, tenantId).getOrThrow()
        assertNull(result.serviceCharge)
    }

    @Test
    fun `preserves existing tip when calculating totals`() = runBlocking {
        val taxRepo = FakeTaxConfigRepo(listOf(
            TaxConfig(TaxConfigId.generate(), tenantId, "PPN", BigDecimal("10"), isActive = true)
        ))
        val useCase = CalculateSaleTotalsUseCase(taxRepo, FakeOutletSettingsRepo())
        val sale = createSale(listOf(createLine(100000)))
            .addTip(TipLine(Money(BigDecimal("10000"))))
        val result = useCase(sale, tenantId).getOrThrow()

        // tip should still be there
        assertEquals(BigDecimal("10000"), result.tipAmount().amount)
        // total = 100000 + 10000 (tax) + 10000 (tip) = 120000
        assertEquals(BigDecimal("120000"), result.totalAmount().amount)
    }
}
