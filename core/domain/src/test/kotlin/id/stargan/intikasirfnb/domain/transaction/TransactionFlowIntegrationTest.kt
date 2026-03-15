package id.stargan.intikasirfnb.domain.transaction

import id.stargan.intikasirfnb.domain.catalog.CategoryId
import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.UserId
import id.stargan.intikasirfnb.domain.settings.OutletSettings
import id.stargan.intikasirfnb.domain.settings.OutletSettingsRepository
import id.stargan.intikasirfnb.domain.settings.ServiceChargeConfig
import id.stargan.intikasirfnb.domain.settings.TaxConfig
import id.stargan.intikasirfnb.domain.settings.TaxConfigId
import id.stargan.intikasirfnb.domain.settings.TaxConfigRepository
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.usecase.transaction.AddLineItemUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.AddPaymentUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.CompleteSaleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.ConfirmSaleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.CreateSaleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.RemoveLineItemUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.UpdateLineItemUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.VoidSaleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Integration test: full transaction flow end-to-end using fake repositories.
 * Tests the happy path through all use cases without Room/Android dependencies.
 */
class TransactionFlowIntegrationTest {

    private val tenantId = TenantId.generate()
    private val outletId = OutletId.generate()
    private val cashierId = UserId.generate()

    private lateinit var saleRepo: FakeSaleRepository
    private lateinit var channelRepo: FakeSalesChannelRepository
    private lateinit var tableRepo: FakeTableRepository
    private lateinit var taxRepo: FakeTaxConfigRepository
    private lateinit var outletSettingsRepo: FakeOutletSettingsRepository

    private lateinit var createSale: CreateSaleUseCase
    private lateinit var addLineItem: AddLineItemUseCase
    private lateinit var updateLineItem: UpdateLineItemUseCase
    private lateinit var removeLineItem: RemoveLineItemUseCase
    private lateinit var confirmSale: ConfirmSaleUseCase
    private lateinit var addPayment: AddPaymentUseCase
    private lateinit var completeSale: CompleteSaleUseCase
    private lateinit var voidSale: VoidSaleUseCase

    private lateinit var dineInChannel: SalesChannel
    private lateinit var takeAwayChannel: SalesChannel

    private fun makeMenuItem(name: String, price: Long) = MenuItem(
        id = ProductId.generate(),
        tenantId = tenantId,
        categoryId = CategoryId.generate(),
        name = name,
        basePrice = Money(BigDecimal(price))
    )

    @Before
    fun setup() {
        saleRepo = FakeSaleRepository()
        channelRepo = FakeSalesChannelRepository()
        tableRepo = FakeTableRepository()
        taxRepo = FakeTaxConfigRepository()
        outletSettingsRepo = FakeOutletSettingsRepository()

        createSale = CreateSaleUseCase(saleRepo, channelRepo, tableRepo)
        addLineItem = AddLineItemUseCase(saleRepo)
        updateLineItem = UpdateLineItemUseCase(saleRepo)
        removeLineItem = RemoveLineItemUseCase(saleRepo)
        confirmSale = ConfirmSaleUseCase(saleRepo, taxRepo, outletSettingsRepo)
        addPayment = AddPaymentUseCase(saleRepo)
        completeSale = CompleteSaleUseCase(saleRepo, tableRepository = tableRepo)
        voidSale = VoidSaleUseCase(saleRepo, tableRepo)

        dineInChannel = SalesChannel.dineIn(tenantId)
        takeAwayChannel = SalesChannel.takeAway(tenantId)
        channelRepo.items[dineInChannel.id] = dineInChannel
        channelRepo.items[takeAwayChannel.id] = takeAwayChannel
    }

    // ==================== Happy Path: Take Away ====================

    @Test
    fun `full take away flow - create, add items, confirm, pay, complete`() = runTest {
        val nasiGoreng = makeMenuItem("Nasi Goreng", 25000)
        val esTeh = makeMenuItem("Es Teh", 8000)

        // 1. Create sale
        val sale = createSale(outletId, takeAwayChannel.id, cashierId = cashierId).getOrThrow()
        assertEquals(SaleStatus.DRAFT, sale.status)
        assertEquals(takeAwayChannel.id, sale.channelId)
        assertEquals(OrderFlowType.PAY_FIRST, sale.orderFlow)

        // 2. Add items
        val s2 = addLineItem(sale.id, nasiGoreng, 2).getOrThrow()
        assertEquals(1, s2.lines.size)
        assertEquals(2, s2.lines[0].quantity)
        assertEquals(Money(BigDecimal(50000)), s2.subtotal())

        val s3 = addLineItem(sale.id, esTeh, 1).getOrThrow()
        assertEquals(2, s3.lines.size)
        assertEquals(Money(BigDecimal(58000)), s3.subtotal())

        // 3. Confirm (computes tax)
        taxRepo.taxes.add(TaxConfig(
            id = TaxConfigId.generate(), tenantId = tenantId,
            name = "PPN", rate = BigDecimal("10"), isActive = true
        ))
        val s4 = confirmSale(sale.id, tenantId).getOrThrow()
        assertEquals(SaleStatus.CONFIRMED, s4.status)
        assertEquals(1, s4.taxLines.size)
        assertEquals("PPN", s4.taxLines[0].taxName)
        assertTrue(s4.totalAmount().amount > s4.subtotal().amount)

        // 4. Pay (cash, exact amount)
        val total = s4.totalAmount()
        val s5 = addPayment(sale.id, PaymentMethod.CASH, total).getOrThrow()
        assertEquals(SaleStatus.PAID, s5.status)
        assertTrue(s5.isFullyPaid())
        assertTrue(s5.changeDue().isZero())

        // 5. Complete
        val s6 = completeSale(sale.id).getOrThrow()
        assertEquals(SaleStatus.COMPLETED, s6.status)
    }

    // ==================== Happy Path: Dine In with Table ====================

    @Test
    fun `dine in with table - create, add, confirm, pay, complete releases table`() = runTest {
        // Setup table
        val table = Table(
            id = TableId("T1"), outletId = outletId, name = "Meja 1", capacity = 4
        )
        tableRepo.items[table.id] = table

        // Need to use NONE table mode for this test since default dine-in is REQUIRED
        // but we're providing a table anyway
        val sale = createSale(outletId, dineInChannel.id, tableId = table.id, cashierId = cashierId).getOrThrow()
        assertEquals(SaleStatus.DRAFT, sale.status)
        assertEquals(table.id, sale.tableId)

        // Verify table occupied
        val occupiedTable = tableRepo.items[table.id]!!
        assertEquals(sale.id, occupiedTable.currentSaleId)

        // Add item
        val item = makeMenuItem("Ayam Bakar", 35000)
        addLineItem(sale.id, item, 1).getOrThrow()

        // Confirm + pay + complete
        val s2 = confirmSale(sale.id, tenantId).getOrThrow()
        val s3 = addPayment(sale.id, PaymentMethod.CASH, s2.totalAmount()).getOrThrow()
        val s4 = completeSale(sale.id).getOrThrow()

        assertEquals(SaleStatus.COMPLETED, s4.status)
        // Table should be released
        val releasedTable = tableRepo.items[table.id]!!
        assertTrue(releasedTable.isAvailable)
    }

    // ==================== Modifiers & Add-ons ====================

    @Test
    fun `add item with modifiers and add-ons computes correct total`() = runTest {
        val sale = createSale(outletId, takeAwayChannel.id).getOrThrow()
        val nasiGoreng = makeMenuItem("Nasi Goreng", 25000)

        val modifiers = listOf(
            SelectedModifier("Ukuran", "Large", Money(BigDecimal(5000))),
            SelectedModifier("Level Pedas", "Extra Pedas", Money.zero())
        )
        val addOns = listOf(
            SelectedAddOn("Telur Ceplok", 1, Money(BigDecimal(5000))),
            SelectedAddOn("Keju", 2, Money(BigDecimal(3000)), Money(BigDecimal(6000)))
        )

        val s2 = addLineItem(sale.id, nasiGoreng, 1, modifiers, addOns).getOrThrow()
        val line = s2.lines[0]

        // effectiveUnitPrice = 25000 + 5000 (Large) = 30000
        assertEquals(Money(BigDecimal(30000)), line.effectiveUnitPrice())
        // modifierTotal = 5000
        assertEquals(Money(BigDecimal(5000)), line.modifierTotal())
        // addOnTotal = 5000 + 6000 = 11000
        assertEquals(Money(BigDecimal(11000)), line.addOnTotal())
        // lineTotal = (30000 * 1) + 11000 = 41000
        assertEquals(Money(BigDecimal(41000)), line.lineTotal())
        assertEquals(2, line.selectedModifiers.size)
        assertEquals(2, line.selectedAddOns.size)
    }

    // ==================== Update & Remove Lines ====================

    @Test
    fun `update line quantity changes subtotal`() = runTest {
        val sale = createSale(outletId, takeAwayChannel.id).getOrThrow()
        val item = makeMenuItem("Kopi", 20000)
        val s2 = addLineItem(sale.id, item, 1).getOrThrow()
        assertEquals(Money(BigDecimal(20000)), s2.subtotal())

        val s3 = updateLineItem(sale.id, s2.lines[0].id, quantity = 3).getOrThrow()
        assertEquals(Money(BigDecimal(60000)), s3.subtotal())
        assertEquals(3, s3.lines[0].quantity)
    }

    @Test
    fun `remove line item updates subtotal`() = runTest {
        val sale = createSale(outletId, takeAwayChannel.id).getOrThrow()
        val item1 = makeMenuItem("Kopi", 20000)
        val item2 = makeMenuItem("Roti", 15000)

        val s2 = addLineItem(sale.id, item1, 1).getOrThrow()
        val s3 = addLineItem(sale.id, item2, 1).getOrThrow()
        assertEquals(2, s3.lines.size)
        assertEquals(Money(BigDecimal(35000)), s3.subtotal())

        val s4 = removeLineItem(sale.id, s3.lines[0].id).getOrThrow()
        assertEquals(1, s4.lines.size)
        assertEquals(Money(BigDecimal(15000)), s4.subtotal())
    }

    // ==================== Tax & Service Charge ====================

    @Test
    fun `confirm computes tax and service charge correctly`() = runTest {
        taxRepo.taxes.add(TaxConfig(
            id = TaxConfigId.generate(), tenantId = tenantId,
            name = "PPN", rate = BigDecimal("10"), isActive = true
        ))
        outletSettingsRepo.settings[outletId] = OutletSettings(
            outletId = outletId, tenantId = tenantId,
            serviceCharge = ServiceChargeConfig(isEnabled = true, rate = BigDecimal("5"))
        )

        val sale = createSale(outletId, takeAwayChannel.id).getOrThrow()
        addLineItem(sale.id, makeMenuItem("Item", 100000), 1).getOrThrow()
        val confirmed = confirmSale(sale.id, tenantId).getOrThrow()

        assertEquals(SaleStatus.CONFIRMED, confirmed.status)
        // Subtotal = 100000
        assertEquals(Money(BigDecimal(100000)), confirmed.subtotal())
        // Tax = 10% of 100000 = 10000
        assertEquals(1, confirmed.taxLines.size)
        // SC = 5% of 100000 = 5000
        assertNotNull(confirmed.serviceCharge)
        // Total = 100000 + 10000 + 5000 = 115000
        assertEquals(Money(BigDecimal(115000)), confirmed.totalAmount())
    }

    // ==================== Multi-Payment ====================

    @Test
    fun `multi-payment with cash and card`() = runTest {
        val sale = createSale(outletId, takeAwayChannel.id).getOrThrow()
        addLineItem(sale.id, makeMenuItem("Item", 100000), 1).getOrThrow()
        val confirmed = confirmSale(sale.id, tenantId).getOrThrow()
        val total = confirmed.totalAmount()

        // Pay half with card
        val halfAmount = Money(total.amount.divide(BigDecimal(2)))
        val s2 = addPayment(sale.id, PaymentMethod.CARD, halfAmount, reference = "AUTH123").getOrThrow()
        assertEquals(SaleStatus.CONFIRMED, s2.status) // not fully paid yet
        assertEquals(1, s2.payments.size)

        // Pay rest with cash
        val remaining = s2.remainingAmount()
        val s3 = addPayment(sale.id, PaymentMethod.CASH, remaining).getOrThrow()
        assertEquals(SaleStatus.PAID, s3.status) // auto-transitions
        assertEquals(2, s3.payments.size)
        assertTrue(s3.isFullyPaid())
    }

    @Test
    fun `cash overpayment produces correct change`() = runTest {
        val sale = createSale(outletId, takeAwayChannel.id).getOrThrow()
        addLineItem(sale.id, makeMenuItem("Item", 27000), 1).getOrThrow()
        confirmSale(sale.id, tenantId).getOrThrow()

        val s2 = addPayment(sale.id, PaymentMethod.CASH, Money(BigDecimal(50000))).getOrThrow()
        assertEquals(SaleStatus.PAID, s2.status)
        assertEquals(Money(BigDecimal(23000)), s2.changeDue())
    }

    // ==================== Void ====================

    @Test
    fun `void draft sale`() = runTest {
        val sale = createSale(outletId, takeAwayChannel.id).getOrThrow()
        addLineItem(sale.id, makeMenuItem("Item", 10000), 1).getOrThrow()

        val voided = voidSale(sale.id).getOrThrow()
        assertEquals(SaleStatus.VOIDED, voided.status)
    }

    @Test
    fun `void confirmed sale releases table`() = runTest {
        val table = Table(id = TableId("T5"), outletId = outletId, name = "Meja 5", capacity = 2)
        tableRepo.items[table.id] = table

        val sale = createSale(outletId, dineInChannel.id, tableId = table.id).getOrThrow()
        addLineItem(sale.id, makeMenuItem("Item", 10000), 1).getOrThrow()
        confirmSale(sale.id, tenantId).getOrThrow()

        val voided = voidSale(sale.id).getOrThrow()
        assertEquals(SaleStatus.VOIDED, voided.status)
        assertTrue(tableRepo.items[table.id]!!.isAvailable)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `create sale with inactive channel fails`() = runTest {
        val inactive = takeAwayChannel.copy(isActive = false)
        channelRepo.items[inactive.id] = inactive

        val result = createSale(outletId, inactive.id)
        assertTrue(result.isFailure)
    }

    @Test
    fun `add payment to non-existent sale fails`() = runTest {
        val result = addPayment(SaleId("non-existent"), PaymentMethod.CASH, Money(BigDecimal(10000)))
        assertTrue(result.isFailure)
    }

    @Test
    fun `update line with modifiers replaces modifiers`() = runTest {
        val sale = createSale(outletId, takeAwayChannel.id).getOrThrow()
        val item = makeMenuItem("Kopi", 20000)
        val mods = listOf(SelectedModifier("Ukuran", "Large", Money(BigDecimal(5000))))
        val s2 = addLineItem(sale.id, item, 1, mods).getOrThrow()
        assertEquals(Money(BigDecimal(25000)), s2.lines[0].effectiveUnitPrice())

        val newMods = listOf(SelectedModifier("Ukuran", "Regular", Money.zero()))
        val s3 = updateLineItem(sale.id, s2.lines[0].id, selectedModifiers = newMods).getOrThrow()
        assertEquals(Money(BigDecimal(20000)), s3.lines[0].effectiveUnitPrice())
        assertEquals("Regular", s3.lines[0].selectedModifiers[0].optionName)
    }

    @Test
    fun `full flow persists in repository`() = runTest {
        val sale = createSale(outletId, takeAwayChannel.id).getOrThrow()
        addLineItem(sale.id, makeMenuItem("Item", 30000), 2).getOrThrow()
        confirmSale(sale.id, tenantId).getOrThrow()
        addPayment(sale.id, PaymentMethod.CASH, Money(BigDecimal(100000))).getOrThrow()
        completeSale(sale.id).getOrThrow()

        // Verify final state in repo
        val saved = saleRepo.items[sale.id]!!
        assertEquals(SaleStatus.COMPLETED, saved.status)
        assertEquals(1, saved.lines.size)
        assertEquals(2, saved.lines[0].quantity)
        assertEquals(1, saved.payments.size)
        assertEquals(Money(BigDecimal(60000)), saved.subtotal())
    }

    // ==================== Fake Repositories ====================

    private class FakeSaleRepository : SaleRepository {
        val items = mutableMapOf<SaleId, Sale>()
        override suspend fun getById(id: SaleId) = items[id]
        override suspend fun save(sale: Sale) { items[sale.id] = sale }
        override fun streamByOutlet(outletId: OutletId): Flow<List<Sale>> =
            flowOf(items.values.filter { it.outletId == outletId })
        override suspend fun listByOutlet(outletId: OutletId, limit: Int) =
            items.values.filter { it.outletId == outletId }.take(limit)
        override suspend fun listOpenByOutlet(outletId: OutletId) =
            items.values.filter { it.outletId == outletId && it.status in listOf(SaleStatus.DRAFT, SaleStatus.OPEN, SaleStatus.CONFIRMED) }
        override fun streamOpenByOutlet(outletId: OutletId): Flow<List<Sale>> =
            flowOf(listOf())
    }

    private class FakeSalesChannelRepository : SalesChannelRepository {
        val items = mutableMapOf<SalesChannelId, SalesChannel>()
        override suspend fun getById(id: SalesChannelId) = items[id]
        override suspend fun save(channel: SalesChannel) { items[channel.id] = channel }
        override suspend fun listByTenant(tenantId: TenantId) =
            items.values.filter { it.tenantId == tenantId }
        override suspend fun delete(id: SalesChannelId) { items.remove(id) }
    }

    private class FakeTableRepository : TableRepository {
        val items = mutableMapOf<TableId, Table>()
        override suspend fun getById(id: TableId) = items[id]
        override suspend fun save(table: Table) { items[table.id] = table }
        override suspend fun delete(id: TableId) { items.remove(id) }
        override suspend fun listByOutlet(outletId: OutletId): List<Table> =
            items.values.filter { it.outletId == outletId }
        override fun streamByOutlet(outletId: OutletId): Flow<List<Table>> =
            flowOf(items.values.filter { it.outletId == outletId })
        override suspend fun listAvailable(outletId: OutletId) =
            items.values.filter { it.outletId == outletId && it.isAvailable }
        override suspend fun listOccupied(outletId: OutletId) =
            items.values.filter { it.outletId == outletId && !it.isAvailable }
        override suspend fun findBySaleId(saleId: SaleId) =
            items.values.find { it.currentSaleId == saleId }
        override suspend fun occupyTable(tableId: TableId, saleId: SaleId) {
            val table = items[tableId] ?: return
            items[tableId] = table.occupy(saleId)
        }
        override suspend fun releaseTable(tableId: TableId) {
            val table = items[tableId] ?: return
            items[tableId] = table.release()
        }
        override suspend fun releaseBySaleId(saleId: SaleId) {
            val table = items.values.find { it.currentSaleId == saleId } ?: return
            items[table.id] = table.release()
        }
        override suspend fun listSections(outletId: OutletId) =
            items.values.filter { it.outletId == outletId }.mapNotNull { it.section }.distinct()
    }

    private class FakeTaxConfigRepository : TaxConfigRepository {
        val taxes = mutableListOf<TaxConfig>()
        override suspend fun getById(id: TaxConfigId) = taxes.find { it.id == id }
        override suspend fun getActiveByTenant(tenantId: TenantId) =
            taxes.filter { it.tenantId == tenantId && it.isActive }
        override suspend fun getAllByTenant(tenantId: TenantId) =
            taxes.filter { it.tenantId == tenantId }
        override suspend fun save(taxConfig: TaxConfig) {
            taxes.removeAll { it.id == taxConfig.id }
            taxes.add(taxConfig)
        }
        override suspend fun delete(id: TaxConfigId) { taxes.removeAll { it.id == id } }
    }

    private class FakeOutletSettingsRepository : OutletSettingsRepository {
        val settings = mutableMapOf<OutletId, OutletSettings>()
        override suspend fun getByOutletId(outletId: OutletId) = settings[outletId]
        override suspend fun save(s: OutletSettings) { settings[s.outletId] = s }
    }
}
