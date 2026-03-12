package id.stargan.intikasirfnb.domain.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMetadataTest {

    @Test
    fun `default sync metadata has PENDING status`() {
        val meta = SyncMetadata()
        assertEquals(SyncStatus.PENDING, meta.syncStatus)
        assertEquals(0L, meta.syncVersion)
        assertFalse(meta.isDeleted)
    }

    @Test
    fun `markSynced updates status and version`() {
        val meta = SyncMetadata().markSynced(5L)
        assertEquals(SyncStatus.SYNCED, meta.syncStatus)
        assertEquals(5L, meta.syncVersion)
    }

    @Test
    fun `markUpdated resets to PENDING`() {
        val meta = SyncMetadata().markSynced(5L).markUpdated("terminal-1")
        assertEquals(SyncStatus.PENDING, meta.syncStatus)
        assertEquals("terminal-1", meta.updatedByTerminalId)
    }

    @Test
    fun `markDeleted sets deletedAt and isDeleted`() {
        val meta = SyncMetadata().markDeleted("terminal-1")
        assertTrue(meta.isDeleted)
        assertNotNull(meta.deletedAt)
        assertEquals(SyncStatus.PENDING, meta.syncStatus)
    }

    @Test
    fun `non-deleted metadata returns isDeleted false`() {
        val meta = SyncMetadata()
        assertNull(meta.deletedAt)
        assertFalse(meta.isDeleted)
    }
}
