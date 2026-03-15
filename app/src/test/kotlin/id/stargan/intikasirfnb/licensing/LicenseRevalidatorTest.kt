package id.stargan.intikasirfnb.licensing

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for LicenseRevalidator grace period logic (1.7.14).
 */
class LicenseRevalidatorTest {

    private val api = mockk<AppRegApi>()
    private val storage = mockk<LicenseStorage>(relaxed = true)
    private val deviceIdProvider = mockk<DeviceIdProvider>()

    private val revalidator = LicenseRevalidator(api, storage, deviceIdProvider)

    private val testLicense = SignedLicenseDto(
        sn = "SN-001",
        applicationId = "intikasir-fnb",
        deviceId = "device-abc",
        licenseType = "PERPETUAL",
        maxDevices = 3,
        boundDevices = 1,
        expiry = null,
        signature = "aabbccdd",
        payloadBase64 = "dGVzdA==",
        publicKeyHex = "0000"
    )

    private fun setupStorageWithLicense() {
        every { storage.load() } returns testLicense
        every { deviceIdProvider.getDeviceId() } returns "device-abc"
    }

    // ==================== Online: valid ====================

    @Test
    fun `online valid - returns true and updates last check`() = runTest {
        setupStorageWithLicense()
        coEvery { api.validate("SN-001", "device-abc") } returns ValidationResponse(
            valid = true, sn = "SN-001", applicationId = "intikasir-fnb",
            status = "active", licenseType = "PERPETUAL",
            maxDevices = 3, boundDevices = 1, boundDeviceIds = listOf("device-abc")
        )

        val result = revalidator.checkOnline()
        assertTrue(result)
        verify { storage.updateLastOnlineCheck() }
    }

    // ==================== Online: revoked ====================

    @Test
    fun `online revoked - returns false and clears storage`() = runTest {
        setupStorageWithLicense()
        coEvery { api.validate("SN-001", "device-abc") } returns ValidationResponse(
            valid = false, sn = "SN-001", applicationId = "intikasir-fnb",
            status = "revoked", licenseType = "PERPETUAL",
            maxDevices = 3, boundDevices = 1, boundDeviceIds = emptyList(),
            error = "License revoked"
        )

        val result = revalidator.checkOnline()
        assertFalse(result)
        verify { storage.clear() }
    }

    // ==================== No stored license ====================

    @Test
    fun `no stored license - returns false`() = runTest {
        every { storage.load() } returns null

        val result = revalidator.checkOnline()
        assertFalse(result)
    }

    // ==================== Offline: within grace period ====================

    @Test
    fun `offline within grace period - returns true`() = runTest {
        setupStorageWithLicense()
        coEvery { api.validate(any(), any()) } throws IOException("No network")
        // Last check was 1 day ago (within 7-day grace)
        every { storage.getLastOnlineCheck() } returns
                System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000L)

        val result = revalidator.checkOnline()
        assertTrue(result)
    }

    @Test
    fun `offline 6 days - still within grace period`() = runTest {
        setupStorageWithLicense()
        coEvery { api.validate(any(), any()) } throws IOException("No network")
        every { storage.getLastOnlineCheck() } returns
                System.currentTimeMillis() - (6 * 24 * 60 * 60 * 1000L)

        val result = revalidator.checkOnline()
        assertTrue(result)
    }

    // ==================== Offline: grace period exceeded ====================

    @Test
    fun `offline 8 days - grace period exceeded, clears and returns false`() = runTest {
        setupStorageWithLicense()
        coEvery { api.validate(any(), any()) } throws IOException("No network")
        every { storage.getLastOnlineCheck() } returns
                System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000L)

        val result = revalidator.checkOnline()
        assertFalse(result)
        verify { storage.clear() }
    }

    @Test
    fun `offline 30 days - grace period exceeded`() = runTest {
        setupStorageWithLicense()
        coEvery { api.validate(any(), any()) } throws IOException("No network")
        every { storage.getLastOnlineCheck() } returns
                System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)

        val result = revalidator.checkOnline()
        assertFalse(result)
    }

    // ==================== Edge: never checked online ====================

    @Test
    fun `offline never checked (lastCheck = 0) - allows app (no grace period starts from 0)`() = runTest {
        setupStorageWithLicense()
        coEvery { api.validate(any(), any()) } throws IOException("No network")
        every { storage.getLastOnlineCheck() } returns 0L

        val result = revalidator.checkOnline()
        // lastCheck=0 means grace period check: elapsed = now - 0 > 7 days → true
        // But the code checks `lastCheck > 0` first, so 0 means "never checked" → allow
        assertTrue(result)
    }

    // ==================== Grace period constant ====================

    @Test
    fun `grace period is exactly 7 days`() {
        val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
        assertTrue(LicenseRevalidator.OFFLINE_GRACE_PERIOD_MS == sevenDaysMs)
    }
}
