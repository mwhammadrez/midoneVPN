package dev.tunnelkit.core.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileCodecTest {

    @Test
    fun roundTripPreservesFields() {
        val items = listOf(
            Profile("1", "Amsterdam", "vless://uuid@h:443?security=tls", "tcp", "tls"),
            Profile("2", "Tabriz", "vless://uuid@h:80?type=ws", "ws", "none"),
        )
        assertEquals(items, ProfileCodec.decode(ProfileCodec.encode(items)))
    }

    @Test
    fun tabsInNameAreSanitised() {
        val p = Profile("1", "bad\tname", "vless://x", "tcp", "tls")
        val decoded = ProfileCodec.decode(ProfileCodec.encode(listOf(p)))
        assertEquals(1, decoded.size)
        assertEquals("bad name", decoded.single().name)
    }

    @Test
    fun malformedLinesAreDroppedNotCrashing() {
        assertEquals(1, ProfileCodec.decode("1\tok\tvless://x\ttcp\ttls\nnot-enough-fields\n\n").size)
    }

    @Test
    fun emptyInputDecodesToEmptyList() {
        assertTrue(ProfileCodec.decode("").isEmpty())
    }

    @Test
    fun redactedSummaryHidesLink() {
        val p = Profile("1", "Node", "vless://secret-uuid@host:443", "tcp", "reality")
        assertTrue(p.redacted().contains("Node"))
        assertFalse(p.redacted().contains("secret-uuid"))
    }
}
