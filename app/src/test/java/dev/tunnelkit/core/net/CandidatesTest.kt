package dev.tunnelkit.core.net

import dev.tunnelkit.core.config.VlessLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidatesTest {

    private fun link(s: String) = VlessLink.parse(s).getOrThrow()

    @Test
    fun cdnFrontedPlainLinkOffersTlsOnTheSiteDomain() {
        val l = link(
            "vless://5783a3e7-e373-51cd-8642-c83782b807c5@cdn.example.net:80" +
                "?encryption=none&security=none&type=ws&path=%2F&host=site.example.com#n"
        )
        val c = Preflight.candidates(l)
        assertEquals(3, c.size)
        assertEquals("cdn.example.net", c[0].host)
        assertEquals(443 to "tls", c[1].port to c[1].security)
        assertEquals("site.example.com", c[1].sni)
        // dial the real domain directly
        assertEquals("site.example.com", c[2].host)
        assertEquals(443, c[2].port)
    }

    @Test
    fun privatePlainLinkIsNotRewritten() {
        val l = link(
            "vless://5783a3e7-e373-51cd-8642-c83782b807c5@127.0.0.1:8080" +
                "?encryption=none&security=none&type=ws&path=%2F#local"
        )
        assertEquals(1, Preflight.candidates(l).size)
    }

    @Test
    fun alreadyTlsLinkAddsOnlyTheDirectDomainVariant() {
        val l = link(
            "vless://5783a3e7-e373-51cd-8642-c83782b807c5@cdn.example.net:443" +
                "?encryption=none&security=tls&sni=site.example.com&type=ws&path=%2F&host=site.example.com#n"
        )
        val c = Preflight.candidates(l)
        assertEquals(2, c.size)
        assertEquals("site.example.com", c[1].host)
    }

    @Test
    fun realityIsNeverDowngraded() {
        val l = link(
            "vless://5783a3e7-e373-51cd-8642-c83782b807c5@1.2.3.4:443" +
                "?encryption=none&security=reality&sni=www.example.com&pbk=tRm19XafUz5RmZHsBdC3HlSvDFyTFciP7fLUzQDTPS4&type=tcp#r"
        )
        assertTrue(Preflight.candidates(l).all { it.security == "reality" })
    }
}
