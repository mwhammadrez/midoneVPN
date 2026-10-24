package dev.tunnelkit.core.config

import org.junit.Assume.assumeTrue
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Feeds the configs this app actually generates to the real Xray binary (same version as libXray:
 * 26.10.21) and asserts the core accepts them.
 *
 * This is the class of bug that reached the field once already: `geoip:private` parsed fine in our
 * own tests, then Xray refused the whole config at startup and the app reported a dead server.
 */
class RealCoreConfigTest {

    private val core = File(System.getenv("XRAY_BIN") ?: "/tmp/xray/xray")

    private fun runConfig(json: String): Pair<Int, String> {
        val f = File.createTempFile("midone-cfg", ".json").apply { writeText(json) }
        val p = ProcessBuilder(core.path, "run", "-test", "-c", f.path)
            .redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor(30, TimeUnit.SECONDS)
        f.delete()
        return p.exitValue() to out
    }

    private fun assertCoreAccepts(name: String, json: String) {
        val (code, out) = runConfig(json)
        assertTrue("$name rejected by real Xray:\n$out", code == 0)
        assertTrue("$name: unexpected output\n$out", out.contains("Configuration OK", ignoreCase = true))
    }

    private val wsTls = "vless://5783a3e7-e373-51cd-8642-c83782b807c5@example.com:443" +
        "?encryption=none&security=tls&sni=example.com&fp=chrome&type=ws&path=%2Fws&host=example.com#ws-tls"

    private val wsPlainPrivate = "vless://5783a3e7-e373-51cd-8642-c83782b807c5@127.0.0.1:8080" +
        "?encryption=none&security=none&type=ws&path=%2F#ws-plain"

    private val reality = "vless://5783a3e7-e373-51cd-8642-c83782b807c5@1.2.3.4:443" +
        "?encryption=none&security=reality&sni=www.example.com&fp=chrome" +
        "&pbk=tRm19XafUz5RmZHsBdC3HlSvDFyTFciP7fLUzQDTPS4&sid=ab12&type=tcp&flow=xtls-rprx-vision#reality"

    private val grpcTls = "vless://5783a3e7-e373-51cd-8642-c83782b807c5@example.com:443" +
        "?encryption=none&security=tls&sni=example.com&type=grpc&serviceName=grpcsvc#grpc"

    /** `toXrayJson()` already returns a complete config; wrapping it again yields "unknown config id". */
    private fun socksFor(link: String): String = VlessLink.parse(link).getOrThrow().toXrayJson()

    @Test
    fun realCoreAcceptsEveryTransportWeEmit() {
        assumeTrue("xray binary not present", core.canExecute())
        assertCoreAccepts("ws+tls", socksFor(wsTls))
        assertCoreAccepts("ws+plain(private)", socksFor(wsPlainPrivate))
        assertCoreAccepts("reality", socksFor(reality))
        assertCoreAccepts("grpc+tls", socksFor(grpcTls))
    }

    /**
     * The tun inbound cannot open a device on a Linux CI box, but config *parsing* (the stage that
     * killed every connection in the field) happens first: a routing/asset error would show up here.
     */
    @Test
    fun tunConfigParsesTheSameRouting() {
        assumeTrue("xray binary not present", core.canExecute())
        val parsed = VlessLink.parse(wsTls).getOrThrow()
        val (code, out) = runConfig(XrayConfigBuilder.tunConfig(parsed.toOutboundJson(), fd = 42))
        // Every config-layer error carries the "infra/conf" prefix. Anything left must be the tun
        // device itself, which cannot be opened on a build box without /dev/net/tun.
        assertTrue("config layer rejected our tun shape:\n$out", out.contains("Configuration OK"))
        // The WS transport itself is deprecated upstream (unavoidable, protocol level); the config
        // form we emit must not be: no "host in headers" warning.
        assertTrue("deprecated header form still emitted:\n$out", !out.contains("\"host\" in \"headers\""))
        assertTrue("geo asset referenced:\n$out", !out.contains("geoip.dat"))
        if (code != 0) {
            assertTrue("unexpected failure reason:\n$out", out.contains("permission denied", true) ||
                out.contains("bad file descriptor", true) || out.contains("no such device", true))
        }
    }
}

/** Certificate handling, verified against the real binary: allowInsecure is gone in Xray >= 26. */
class TlsCertificateTest {
    private val core = File(System.getenv("XRAY_BIN") ?: "/tmp/xray/xray")

    private fun accepted(json: String): Pair<Int, String> {
        val f = File.createTempFile("midone-cfg", ".json").apply { writeText(json) }
        val p = ProcessBuilder(core.path, "run", "-test", "-c", f.path).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor(30, TimeUnit.SECONDS)
        return p.exitValue() to out
    }

    private fun link(extra: String) = VlessLink.parse(
        "vless://5783a3e7-e373-51cd-8642-c83782b807c5@example.com:443" +
            "?encryption=none&security=tls&sni=example.com&type=ws&path=%2Fws$extra#t"
    ).getOrThrow()

    @Test
    fun pinnedCertificateIsAcceptedByRealCore() {
        assumeTrue(core.canExecute())
        val pin = "3071a42b7290b9be8d16b618b65b85b23cd8fbfa1efb84309b79a462787fb4fd"
        val (code, out) = accepted(link("&pcs=$pin").toXrayJson())
        assertTrue("pinned cert config rejected:\n$out", code == 0)
        assertTrue(out, out.contains("Configuration OK"))
    }

    @Test
    fun allowInsecureIsReportedNotSilentlyDropped() {
        val v = link("&allowInsecure=1")
        assertTrue(v.warnings().contains("cert_not_trusted"))
        assertTrue("must not emit the removed option", !v.toXrayJson().contains("allowInsecure"))
    }
}
