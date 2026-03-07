package id.stargan.intikasirfnb.domain.settings

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.usecase.settings.SaveOutletSettingsUseCase
import id.stargan.intikasirfnb.domain.usecase.settings.SaveTaxConfigUseCase
import id.stargan.intikasirfnb.domain.usecase.settings.SaveTenantSettingsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SettingsTest {

    // --- TaxConfig ---

    @Test
    fun `TaxConfigId generate creates unique ids`() {
        val id1 = TaxConfigId.generate()
        val id2 = TaxConfigId.generate()
        assertTrue(id1.value != id2.value)
    }

    @Test
    fun `TaxConfig default values`() {
        val config = TaxConfig(
            id = TaxConfigId.generate(),
            tenantId = TenantId.generate(),
            name = "PPN",
            rate = BigDecimal("11")
        )
        assertEquals(TaxScope.ALL_ITEMS, config.scope)
        assertFalse(config.isIncludedInPrice)
        assertTrue(config.isActive)
        assertEquals(0, config.sortOrder)
    }

    @Test
    fun `SaveTaxConfigUseCase rejects blank name`() = runTest {
        val useCase = SaveTaxConfigUseCase(FakeTaxConfigRepository())
        val config = TaxConfig(
            id = TaxConfigId.generate(),
            tenantId = TenantId.generate(),
            name = "",
            rate = BigDecimal("10")
        )
        try {
            useCase(config)
            assertTrue("Should have thrown", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("blank"))
        }
    }

    @Test
    fun `SaveTaxConfigUseCase rejects zero rate`() = runTest {
        val useCase = SaveTaxConfigUseCase(FakeTaxConfigRepository())
        val config = TaxConfig(
            id = TaxConfigId.generate(),
            tenantId = TenantId.generate(),
            name = "PPN",
            rate = BigDecimal.ZERO
        )
        try {
            useCase(config)
            assertTrue("Should have thrown", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("positive"))
        }
    }

    @Test
    fun `SaveTaxConfigUseCase rejects rate over 100`() = runTest {
        val useCase = SaveTaxConfigUseCase(FakeTaxConfigRepository())
        val config = TaxConfig(
            id = TaxConfigId.generate(),
            tenantId = TenantId.generate(),
            name = "PPN",
            rate = BigDecimal("101")
        )
        try {
            useCase(config)
            assertTrue("Should have thrown", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("100"))
        }
    }

    @Test
    fun `SaveTaxConfigUseCase saves valid config`() = runTest {
        val repo = FakeTaxConfigRepository()
        val useCase = SaveTaxConfigUseCase(repo)
        val config = TaxConfig(
            id = TaxConfigId.generate(),
            tenantId = TenantId.generate(),
            name = "PPN",
            rate = BigDecimal("11")
        )
        useCase(config)
        assertEquals(config, repo.saved)
    }

    // --- ServiceChargeConfig ---

    @Test
    fun `ServiceChargeConfig defaults are disabled`() {
        val sc = ServiceChargeConfig()
        assertFalse(sc.isEnabled)
        assertEquals(BigDecimal.ZERO, sc.rate)
        assertFalse(sc.isIncludedInPrice)
    }

    // --- TipConfig ---

    @Test
    fun `TipConfig defaults`() {
        val tip = TipConfig()
        assertFalse(tip.isEnabled)
        assertEquals(3, tip.suggestedPercentages.size)
        assertTrue(tip.allowCustomAmount)
    }

    // --- SaveOutletSettingsUseCase ---

    @Test
    fun `SaveOutletSettingsUseCase rejects enabled SC with zero rate`() = runTest {
        val useCase = SaveOutletSettingsUseCase(FakeOutletSettingsRepository())
        val settings = OutletSettings(
            outletId = OutletId.generate(),
            tenantId = TenantId.generate(),
            serviceCharge = ServiceChargeConfig(isEnabled = true, rate = BigDecimal.ZERO)
        )
        try {
            useCase(settings)
            assertTrue("Should have thrown", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("positive"))
        }
    }

    @Test
    fun `SaveOutletSettingsUseCase allows disabled SC with zero rate`() = runTest {
        val repo = FakeOutletSettingsRepository()
        val useCase = SaveOutletSettingsUseCase(repo)
        val settings = OutletSettings(
            outletId = OutletId.generate(),
            tenantId = TenantId.generate(),
            serviceCharge = ServiceChargeConfig(isEnabled = false, rate = BigDecimal.ZERO)
        )
        useCase(settings)
        assertEquals(settings, repo.saved)
    }

    // --- SaveTenantSettingsUseCase ---

    @Test
    fun `SaveTenantSettingsUseCase rejects invalid currency code`() = runTest {
        val useCase = SaveTenantSettingsUseCase(FakeTenantSettingsRepository())
        val settings = TenantSettings(
            tenantId = TenantId.generate(),
            defaultCurrencyCode = "ID"
        )
        try {
            useCase(settings)
            assertTrue("Should have thrown", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("3 characters"))
        }
    }

    @Test
    fun `SaveTenantSettingsUseCase saves valid settings`() = runTest {
        val repo = FakeTenantSettingsRepository()
        val useCase = SaveTenantSettingsUseCase(repo)
        val settings = TenantSettings(
            tenantId = TenantId.generate(),
            defaultCurrencyCode = "IDR"
        )
        useCase(settings)
        assertEquals(settings, repo.saved)
    }

    // --- ReceiptConfig ---

    @Test
    fun `ReceiptConfig defaults`() {
        val rc = ReceiptConfig()
        assertTrue(rc.header.showBusinessName)
        assertTrue(rc.header.showAddress)
        assertTrue(rc.header.showPhone)
        assertFalse(rc.header.showNpwp)
        assertFalse(rc.header.showLogo)
        assertEquals(LogoSize.MEDIUM, rc.header.logoSize)
        assertEquals(emptyList<String>(), rc.header.customHeaderLines)
    }

    @Test
    fun `ReceiptBodyConfig defaults show relevant fields`() {
        val body = ReceiptBodyConfig()
        assertTrue(body.showItemNotes)
        assertTrue(body.showDiscountBreakdown)
        assertTrue(body.showTaxDetail)
        assertTrue(body.showCashierName)
        assertTrue(body.showOrderNumber)
        assertFalse(body.showCustomerName)
    }

    @Test
    fun `ReceiptFooterConfig defaults`() {
        val footer = ReceiptFooterConfig()
        assertEquals(ReceiptBarcodeType.NONE, footer.barcodeType)
        assertTrue(footer.showThankYouMessage)
        assertEquals("Terima kasih atas kunjungan Anda", footer.thankYouMessage)
        assertFalse(footer.showSocialMedia)
    }

    @Test
    fun `PaperWidth provides correct chars per line`() {
        assertEquals(32, PaperWidth.THERMAL_58MM.defaultCharsPerLine)
        assertEquals(48, PaperWidth.THERMAL_80MM.defaultCharsPerLine)
    }

    @Test
    fun `ReceiptConfig with custom header`() {
        val rc = ReceiptConfig(
            header = ReceiptHeaderConfig(
                showLogo = true,
                logoSize = LogoSize.LARGE,
                showNpwp = true,
                customHeaderLines = listOf("Cabang Jakarta Pusat")
            )
        )
        assertTrue(rc.header.showLogo)
        assertEquals(LogoSize.LARGE, rc.header.logoSize)
        assertTrue(rc.header.showNpwp)
        assertEquals(1, rc.header.customHeaderLines.size)
    }

    // --- OutletProfile ---

    @Test
    fun `OutletProfile defaults`() {
        val profile = OutletProfile()
        assertEquals("", profile.name)
        assertEquals(null, profile.address)
        assertEquals(null, profile.phone)
        assertEquals(null, profile.npwp)
        assertEquals(null, profile.logoImagePath)
    }

    @Test
    fun `OutletSettings contains outletProfile`() {
        val settings = OutletSettings(
            outletId = OutletId.generate(),
            tenantId = TenantId.generate(),
            outletProfile = OutletProfile(
                name = "Warung Padang",
                address = "Jl. Merdeka No. 1",
                phone = "021-1234567",
                logoImagePath = "/data/logos/logo.jpg"
            )
        )
        assertEquals("Warung Padang", settings.outletProfile.name)
        assertEquals("/data/logos/logo.jpg", settings.outletProfile.logoImagePath)
    }

    // --- LogoSize ---

    @Test
    fun `LogoSize has three entries`() {
        assertEquals(3, LogoSize.entries.size)
        assertEquals(LogoSize.SMALL, LogoSize.entries[0])
        assertEquals(LogoSize.MEDIUM, LogoSize.entries[1])
        assertEquals(LogoSize.LARGE, LogoSize.entries[2])
    }

    // --- PrinterConfig ---

    @Test
    fun `PrinterConfig defaults to NONE`() {
        val pc = PrinterConfig()
        assertEquals(PrinterConnectionType.NONE, pc.connectionType)
        assertEquals(null, pc.address)
        assertTrue(pc.autoCut)
        assertEquals(5, pc.printDensity)
        assertTrue(pc.autoPrintReceipt)
        assertTrue(pc.autoPrintKitchenTicket)
        assertEquals(1, pc.receiptCopies)
        assertEquals(1, pc.kitchenTicketCopies)
        assertFalse(pc.openCashDrawer)
    }

    @Test
    fun `PrinterConfig bluetooth setup`() {
        val pc = PrinterConfig(
            connectionType = PrinterConnectionType.BLUETOOTH,
            address = "00:11:22:33:44:55",
            name = "EPSON TM-T82X"
        )
        assertEquals(PrinterConnectionType.BLUETOOTH, pc.connectionType)
        assertEquals("00:11:22:33:44:55", pc.address)
        assertEquals("EPSON TM-T82X", pc.name)
    }

    // --- TerminalSettings ---

    @Test
    fun `TerminalSettings defaults`() {
        val ts = TerminalSettings(
            terminalId = TerminalId.generate(),
            outletId = OutletId.generate()
        )
        assertEquals(PrinterConnectionType.NONE, ts.printer.connectionType)
        assertTrue(ts.printer.autoPrintReceipt)
        assertTrue(ts.printer.autoPrintKitchenTicket)
    }

    // --- NumberingSequenceConfig ---

    @Test
    fun `NumberingSequenceConfig defaults`() {
        val nsc = NumberingSequenceConfig(prefix = "RCP")
        assertEquals("RCP", nsc.prefix)
        assertEquals(6, nsc.paddingLength)
        assertEquals(1L, nsc.nextNumber)
    }

    // --- Fake repositories ---

    private class FakeTaxConfigRepository : TaxConfigRepository {
        var saved: TaxConfig? = null
        override suspend fun getById(id: TaxConfigId): TaxConfig? = null
        override suspend fun getActiveByTenant(tenantId: TenantId): List<TaxConfig> = emptyList()
        override suspend fun getAllByTenant(tenantId: TenantId): List<TaxConfig> = emptyList()
        override suspend fun save(taxConfig: TaxConfig) { saved = taxConfig }
        override suspend fun delete(id: TaxConfigId) {}
    }

    private class FakeOutletSettingsRepository : OutletSettingsRepository {
        var saved: OutletSettings? = null
        override suspend fun getByOutletId(outletId: id.stargan.intikasirfnb.domain.identity.OutletId): OutletSettings? = null
        override suspend fun save(settings: OutletSettings) { saved = settings }
    }

    private class FakeTenantSettingsRepository : TenantSettingsRepository {
        var saved: TenantSettings? = null
        override suspend fun getByTenantId(tenantId: TenantId): TenantSettings? = null
        override suspend fun save(settings: TenantSettings) { saved = settings }
    }
}
