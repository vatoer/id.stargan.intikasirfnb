package id.stargan.intikasirfnb.domain.identity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalTest {

    private fun createTerminal(
        type: TerminalType = TerminalType.CASHIER,
        status: TerminalStatus = TerminalStatus.ACTIVE
    ) = Terminal(
        id = TerminalId.generate(),
        tenantId = TenantId.generate(),
        outletId = OutletId.generate(),
        deviceName = "Kasir 1",
        terminalType = type,
        status = status
    )

    @Test
    fun `new terminal has default values`() {
        val terminal = createTerminal()
        assertEquals(TerminalType.CASHIER, terminal.terminalType)
        assertEquals(TerminalStatus.ACTIVE, terminal.status)
        assertFalse(terminal.syncEnabled)
        assertNull(terminal.lastSyncAtMillis)
    }

    @Test
    fun `terminal ID uses ULID format`() {
        val terminal = createTerminal()
        assertEquals(26, terminal.id.value.length)
    }

    @Test
    fun `different terminal types can be created`() {
        val cashier = createTerminal(type = TerminalType.CASHIER)
        val waiter = createTerminal(type = TerminalType.WAITER)
        val kitchen = createTerminal(type = TerminalType.KITCHEN_DISPLAY)
        val manager = createTerminal(type = TerminalType.MANAGER)

        assertEquals(TerminalType.CASHIER, cashier.terminalType)
        assertEquals(TerminalType.WAITER, waiter.terminalType)
        assertEquals(TerminalType.KITCHEN_DISPLAY, kitchen.terminalType)
        assertEquals(TerminalType.MANAGER, manager.terminalType)
    }

    @Test
    fun `terminal status transitions via copy`() {
        val active = createTerminal()
        val suspended = active.copy(status = TerminalStatus.SUSPENDED)
        val deregistered = suspended.copy(status = TerminalStatus.DEREGISTERED)

        assertEquals(TerminalStatus.ACTIVE, active.status)
        assertEquals(TerminalStatus.SUSPENDED, suspended.status)
        assertEquals(TerminalStatus.DEREGISTERED, deregistered.status)
    }

    @Test
    fun `terminal sync can be enabled`() {
        val terminal = createTerminal().copy(syncEnabled = true)
        assertTrue(terminal.syncEnabled)
    }

    @Test
    fun `each terminal gets unique ID`() {
        val t1 = createTerminal()
        val t2 = createTerminal()
        assertNotEquals(t1.id, t2.id)
    }
}
