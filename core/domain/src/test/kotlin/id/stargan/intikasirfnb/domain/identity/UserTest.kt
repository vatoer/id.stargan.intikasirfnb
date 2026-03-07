package id.stargan.intikasirfnb.domain.identity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserTest {

    private val tenantId = TenantId.generate()
    private val outlet1 = OutletId.generate()
    private val outlet2 = OutletId.generate()

    private fun createUser(outletIds: List<OutletId> = emptyList()) = User(
        id = UserId.generate(),
        tenantId = tenantId,
        email = "staff@test.com",
        displayName = "Staff",
        outletIds = outletIds
    )

    @Test
    fun `user with empty outletIds has access to any outlet`() {
        val user = createUser()
        assertTrue(user.hasAccessToOutlet(outlet1))
        assertTrue(user.hasAccessToOutlet(outlet2))
    }

    @Test
    fun `user with specific outletIds only has access to those outlets`() {
        val user = createUser(outletIds = listOf(outlet1))
        assertTrue(user.hasAccessToOutlet(outlet1))
        assertFalse(user.hasAccessToOutlet(outlet2))
    }

    @Test
    fun `user with multiple outlets has access to all assigned`() {
        val user = createUser(outletIds = listOf(outlet1, outlet2))
        assertTrue(user.hasAccessToOutlet(outlet1))
        assertTrue(user.hasAccessToOutlet(outlet2))
    }
}
