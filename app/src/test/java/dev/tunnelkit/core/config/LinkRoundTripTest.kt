package dev.tunnelkit.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkRoundTripTest {

    private val none = "vless://e8e837d4-1a71-4af4-b4f4-4891207a1821@arvancloud.ir:80" +
        "?type=ws&encryption=none&path=%2F&host=mr.meowbile.ir&security=none#MrServerP3-765572"

    @Test
    fun securityNoneIsAWarningNotAnError() {
        val v = VlessLink.parse(none).getOrThrow()
        assertTrue(v.validate().isEmpty())              // importable
        assertTrue(v.warnings().contains("security_none_public"))
        assertFalse(v.isPrivateHost())
    }

    @Test
    fun tlsTwinKeepsRoutingFields() {
        val v = VlessLink.parse(none).getOrThrow()
        val twin = VlessLink.parse(v.toShareLink(forceTls = true)).getOrThrow()
        assertEquals(443, twin.port)
        assertEquals("tls", twin.security)
        assertEquals("ws", twin.network)
        assertEquals("/", twin.path)
        assertEquals("mr.meowbile.ir", twin.hostHeader)
        assertEquals("mr.meowbile.ir", twin.sni)
        assertEquals(v.uuid, twin.uuid)
        assertTrue(twin.validate().isEmpty())
        assertTrue(twin.warnings().isEmpty())
    }

    @Test
    fun roundTripPreservesRealityFields() {
        val reality = "vless://5783a3e7-e373-51cd-8642-c83782b807c5@1.2.3.4:443" +
            "?encryption=none&security=reality&sni=www.example.com&fp=chrome&pbk=PUBKEY&sid=ab12" +
            "&type=tcp&flow=xtls-rprx-vision#Node"
        val v = VlessLink.parse(reality).getOrThrow()
        val again = VlessLink.parse(v.toShareLink()).getOrThrow()
        assertEquals("reality", again.security)
        assertEquals("PUBKEY", again.publicKey)
        assertEquals("ab12", again.shortId)
        assertEquals("xtls-rprx-vision", again.flow)
        assertEquals("www.example.com", again.sni)
    }
}
