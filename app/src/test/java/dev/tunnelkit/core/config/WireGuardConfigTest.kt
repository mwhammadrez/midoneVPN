package dev.tunnelkit.core.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WireGuardConfigTest {

    private val k1 = "yAnz5TF+lXXJte14tji3zlMNq+hd2rYUIgJBgB3fBmk="
    private val k2 = "xTIBA5rboUvnH4htodjb6e697QjLERt1NAB4mZqp8Dg="
    private val k3 = "TrMvSoP4jYQlY6RIzBgbssQqY3vxI2Pi+y71lOWWXX0="

    private val minimal = """
        [Interface]
        PrivateKey = $k1
        Address = 10.7.0.2/32

        [Peer]
        PublicKey = $k2
        AllowedIPs = 0.0.0.0/0
        Endpoint = vpn.example.com:51820
    """.trimIndent()

    @Test
    fun parsesMinimalConfig() {
        val r = WireGuardConfig.parse(minimal)
        assertTrue(r.ok)
        val c = r.config!!
        assertTrue(c.interfaceSection.addresses.contains("10.7.0.2/32"))
        assertTrue(c.peers.single().endpoint == "vpn.example.com:51820")
        assertTrue(c.peers.single().allowedIps.contains("0.0.0.0/0"))
    }

    @Test
    fun parsesFullConfigWithMultiplePeersAndIgnoresUnknown() {
        val text = """
            [Interface]
            PrivateKey = $k1
            Address = 10.7.0.2/32, fd00::2/128
            DNS = 1.1.1.1, 8.8.8.8
            MTU = 1420
            ListenPort = 51820

            [Peer]
            PublicKey = $k2
            PresharedKey = $k3
            AllowedIPs = 10.0.0.0/8, 192.168.0.0/16
            Endpoint = 1.2.3.4:51820
            PersistentKeepalive = 25

            [Peer]
            PublicKey = $k3
            AllowedIPs = 0.0.0.0/0
            Endpoint = [2001:db8::1]:51820
            SomethingNew = 1
        """.trimIndent()
        val r = WireGuardConfig.parse(text)
        val c = r.config!!
        assertTrue(c.peers.size == 2)
        assertTrue(c.interfaceSection.dns.size == 2)
        assertTrue(c.interfaceSection.mtu == 1420)
        assertTrue(c.peers[0].persistentKeepalive == 25)
        assertTrue(r.problems.any { it.code == WireGuardConfig.Code.UNKNOWN_FIELD })
        assertTrue(c.unknown.any { it.contains("SomethingNew") })
    }

    @Test
    fun rejectsMissingPrivateKey() {
        val r = WireGuardConfig.parse(minimal.replace("PrivateKey = $k1\n", ""))
        assertFalse(r.ok)
        assertTrue(r.problems.any { it.code == WireGuardConfig.Code.MISSING_PRIVATE_KEY })
    }

    @Test
    fun rejectsInvalidBase64Key() {
        val r = WireGuardConfig.parse(minimal.replace(k1, "not-a-real-key"))
        assertFalse(r.ok)
        assertTrue(r.problems.any { it.code == WireGuardConfig.Code.INVALID_KEY })
    }

    @Test
    fun rejectsMissingEndpoint() {
        val r = WireGuardConfig.parse(minimal.replace("Endpoint = vpn.example.com:51820", ""))
        assertFalse(r.ok)
        assertTrue(r.problems.any { it.code == WireGuardConfig.Code.MISSING_ENDPOINT })
    }

    @Test
    fun rejectsInvalidAllowedIps() {
        val r = WireGuardConfig.parse(minimal.replace("0.0.0.0/0", "999.1.1.1/33"))
        assertFalse(r.ok)
        assertTrue(r.problems.any { it.code == WireGuardConfig.Code.INVALID_ALLOWED_IPS })
    }

    @Test
    fun unknownSectionIsReportedNotDropped() {
        val r = WireGuardConfig.parse(minimal + "\n[Extra]\nFoo = bar\n")
        assertTrue(r.config!!.unknown.any { it.contains("Extra") })
        assertTrue(r.problems.any { it.code == WireGuardConfig.Code.UNKNOWN_FIELD })
    }

    @Test
    fun oversizedAndMalformedInput() {
        val big = "[Interface]\n" + "A".repeat(WireGuardConfig.MAX_BYTES)
        assertTrue(WireGuardConfig.parse(big).problems.any { it.code == WireGuardConfig.Code.OVERSIZED })
        assertTrue(WireGuardConfig.parse("this is not a config").problems.any {
            it.code == WireGuardConfig.Code.NO_INTERFACE || it.code == WireGuardConfig.Code.MALFORMED
        })
        assertTrue(WireGuardConfig.parse("   ").problems.any { it.code == WireGuardConfig.Code.MALFORMED })
    }

    @Test
    fun secretsAreRedactedEverywhere() {
        val r = WireGuardConfig.parse(minimal)
        val c = r.config!!
        val dump = c.toString() + c.redactedSummary() + c.peers.toString()
        assertFalse("private key leaked", dump.contains(k1))
        assertFalse("preshared key leaked", dump.contains(k3))
        assertTrue(c.redactedSummary().contains("wg"))
        assertTrue(c.interfaceSection.privateKey!!.reveal() == k1)
        assertTrue(c.interfaceSection.privateKey!!.toString() == "•••")
    }

    @Test
    fun endpointFormsAreValidated() {
        assertTrue(WireGuardConfig.isEndpoint("1.2.3.4:51820"))
        assertTrue(WireGuardConfig.isEndpoint("[2001:db8::1]:51820"))
        assertFalse(WireGuardConfig.isEndpoint("1.2.3.4"))
        assertFalse(WireGuardConfig.isEndpoint("1.2.3.4:99999"))
    }
}
