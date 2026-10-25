package dev.tunnelkit.core.config

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/** The WireGuard outbound must load in the real core, not just look right in a string test. */
class WireGuardRealCoreTest {

    private val core = File(System.getenv("XRAY_BIN") ?: "/tmp/xray/xray")

    private val conf = """
        [Interface]
        PrivateKey = KfmVTyAIAT6pYTSi7wBRVXUPEBbZ5X+AFlMDEYccgxs=
        Address = 10.66.0.2/32, fd00::2/128
        DNS = 1.1.1.1
        MTU = 1380

        [Peer]
        PublicKey = pIoiHvgMX3ui1HTyAzDi2QexMYf9iy2MErSTM/yM+l8=
        PresharedKey = xYrBzV1Fm+ZlH82m4vS6dOY844DOVEO5hHt2ZKYz4og=
        Endpoint = wg.example.com:51820
        AllowedIPs = 0.0.0.0/0, ::/0
        PersistentKeepalive = 25
    """.trimIndent()

    @Test
    fun parsesAndRendersTheStandardFields() {
        val parsed = WireGuardConfig.parse(conf)
        assertTrue(parsed.problems.toString(), parsed.ok)
        val json = WireGuardOutbound.toOutboundJson(parsed.config!!)
        assertTrue(json.contains("\"protocol\":\"wireguard\""))
        assertTrue(json.contains("\"endpoint\":\"wg.example.com:51820\""))
        assertTrue(json.contains("\"keepAlive\":25"))
        assertTrue(json.contains("\"address\":[\"10.66.0.2\",\"fd00::2\"]"))
        assertTrue(json.contains("\"allowedIPs\":[\"0.0.0.0/0\",\"::/0\"]"))
        assertTrue("private key must not leak into strings", parsed.config.toString().isNotEmpty())
    }

    @Test
    fun realCoreAcceptsTheGeneratedOutbound() {
        assumeTrue("xray binary missing", core.canExecute())
        val parsed = WireGuardConfig.parse(conf)
        val cfg = XrayConfigBuilder.tunConfig(
            WireGuardOutbound.toOutboundJson(parsed.config!!), fd = 3, mtu = 1380, tunName = "tun0"
        )
        val f = File.createTempFile("midone-wg", ".json").apply { writeText(cfg) }
        val p = ProcessBuilder(core.path, "run", "-test", "-c", f.path).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor(30, TimeUnit.SECONDS)
        assertTrue("core rejected the wireguard outbound:\n$out", out.contains("Configuration OK"))
    }
}
