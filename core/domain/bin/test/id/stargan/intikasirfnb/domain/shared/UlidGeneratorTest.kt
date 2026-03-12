package id.stargan.intikasirfnb.domain.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UlidGeneratorTest {

    @Test
    fun `generate produces 26-character ULID string`() {
        val ulid = UlidGenerator.generate()
        assertEquals(26, ulid.length)
    }

    @Test
    fun `generate produces unique IDs`() {
        val ids = (1..100).map { UlidGenerator.generate() }.toSet()
        assertEquals(100, ids.size)
    }

    @Test
    fun `generated ULIDs are monotonically sortable`() {
        val ids = (1..10).map { UlidGenerator.generate() }
        val sorted = ids.sorted()
        assertEquals(ids, sorted)
    }

    @Test
    fun `ULID contains only valid characters`() {
        val ulid = UlidGenerator.generate()
        assertTrue(ulid.all { it in "0123456789ABCDEFGHJKMNPQRSTVWXYZ" })
    }
}
