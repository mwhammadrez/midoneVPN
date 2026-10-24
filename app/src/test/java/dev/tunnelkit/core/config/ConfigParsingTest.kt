package dev.tunnelkit.core.config

import dev.tunnelkit.core.engine.ConfigFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class ConfigParsingTest {

    private val linkA = "vless://5783a3e7-e373-51cd-8642-c83782b807c5@1.2.3.4:443" +
        "?security=tls&sni=a.com&type=tcp#A"
    private val linkB = "vless://5783a3e7-e373-51cd-8642-c83782b807c5@5.6.7.8:443" +
        "?security=tls&sni=b.com&type=tcp#B"

    @Test
    fun detectsSingleShareLink() {
        val d = ConfigDetector.detect(linkA)
        assertTrue(d is ConfigDetector.Detection.Single)
        assertEquals(ConfigFormat.VLESS_LINK, (d as ConfigDetector.Detection.Single).format)
    }

    @Test
    fun detectsXrayJson() {
        val d = ConfigDetector.detect("""{"outbounds":[{"protocol":"vless"}],"inbounds":[]}""")
        assertTrue(d is ConfigDetector.Detection.Single)
        assertEquals(ConfigFormat.XRAY_JSON, (d as ConfigDetector.Detection.Single).format)
    }

    @Test
    fun detectsWireguardConf() {
        val d = ConfigDetector.detect("[Interface]\nPrivateKey = abc\nAddress = 10.0.0.2/32")
        assertEquals(ConfigFormat.WIREGUARD_CONF, (d as ConfigDetector.Detection.Single).format)
    }

    @Test
    fun plainMultiLinkIsSubscription() {
        val d = ConfigDetector.detect("$linkA\n$linkB")
        assertTrue(d is ConfigDetector.Detection.Many)
        assertEquals(2, (d as ConfigDetector.Detection.Many).count)
    }

    @Test
    fun base64SubscriptionIsDetected() {
        val encoded = Base64.getEncoder().encodeToString("$linkA\n$linkB".toByteArray())
        val d = ConfigDetector.detect(encoded)
        assertTrue(d is ConfigDetector.Detection.Many)
        assertEquals(2, (d as ConfigDetector.Detection.Many).count)
    }

    @Test
    fun garbageIsUnsupportedNotGuessed() {
        assertTrue(ConfigDetector.detect("hello world") is ConfigDetector.Detection.Unsupported)
        assertTrue(ConfigDetector.detect("") is ConfigDetector.Detection.Unsupported)
    }

    @Test
    fun parserKeepsValidLinksAndCountsSkipped() {
        val raw = "$linkA\nnot-a-link\n$linkB\n"
        val r = SubscriptionParser.parse(raw)
        assertEquals(2, r.links.size)
        assertEquals(1, r.skipped)
        assertEquals(SubscriptionParser.Encoding.PLAIN, r.encodedAs)
    }

    @Test
    fun parserRejectsOversizedInput() {
        val huge = "x".repeat(SubscriptionParser.MAX_BYTES + 10)
        val threw = runCatching { SubscriptionParser.parse(huge) }.isFailure
        assertTrue(threw)
    }

    @Test
    fun parserCapsLinkCount() {
        val many = (1..SubscriptionParser.MAX_LINKS + 5).joinToString("\n") {
            "vless://5783a3e7-e373-51cd-8642-c83782b807c5@1.2.3.4:443?security=tls#n$it"
        }
        val r = SubscriptionParser.parse(many)
        assertEquals(SubscriptionParser.MAX_LINKS, r.links.size)
        assertTrue(r.truncated)
    }
}
