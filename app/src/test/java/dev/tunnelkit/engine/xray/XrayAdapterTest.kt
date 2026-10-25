package dev.tunnelkit.engine.xray

import dev.tunnelkit.core.engine.ConfigFormat
import dev.tunnelkit.core.engine.EngineConfig
import dev.tunnelkit.core.engine.EngineFailureException
import dev.tunnelkit.core.model.Failure
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XrayAdapterTest {

    private class FakeCore(private val response: (String) -> String) : XrayCore {
        override val apiVersion = 3L
        val requests = mutableListOf<String>()
        var dnsServer: String? = null
        var protectorRegistered = false
        var resetCount = 0

        override fun invoke(requestJson: String): String {
            requests += requestJson
            return response(requestJson)
        }

        override fun setDns(server: String) { dnsServer = server }
        override fun resetDns() { resetCount++ }
        override fun registerProtector(protect: (Long) -> Boolean) { protectorRegistered = true }
    }

    private val ok = "{\"success\":true,\"data\":{},\"error\":\"\"}"
    private val link = "vless://5783a3e7-e373-51cd-8642-c83782b807c5@1.2.3.4:443" +
        "?encryption=none&security=reality&sni=www.cloudflare.com&pbk=PUB&sid=ab12" +
        "&type=tcp&flow=xtls-rprx-vision#Node"

    private fun cfg(text: String = link) = EngineConfig(ConfigFormat.VLESS_LINK, text)

    @Test
    fun descriptorCarriesProvenanceAndLicense() {
        val adapter = XrayAdapter(FakeCore { ok })
        val d = adapter.descriptor
        assertEquals("MIT", d.licenseId)
        assertEquals("26.10.21", d.engineVersion)
        assertTrue(d.provenanceCommit.length == 40)
        assertEquals(dev.tunnelkit.core.engine.TunOwnership.APP_TUN_FD_PASSED, d.tunOwnership)
    }

    @Test
    fun validatesRealLinkAndRejectsGarbage() = runBlocking {
        val adapter = XrayAdapter(FakeCore { ok })
        assertTrue(adapter.validate(cfg()).ok)
        assertFalse(adapter.validate(cfg("vless://nope")).ok)
    }

    @Test
    fun startSendsApiV3RunXrayWithSocksPort() = runBlocking {
        val core = FakeCore { ok }
        val adapter = XrayAdapter(core, socksPort = 19090)
        adapter.prepare()
        adapter.start(cfg())

        val request = core.requests.single()
        assertTrue(request.contains("\"apiVersion\":3"))
        assertTrue(request.contains("\"method\":\"runXray\""))
        assertTrue(request.contains("19090"))
        assertTrue(request.contains("vless"))
        assertTrue(core.protectorRegistered)
    }

    @Test
    fun failureEnvelopeBecomesTypedFailure() = runBlocking {
        val core = FakeCore { "{\"success\":false,\"data\":null,\"error\":\"authentication failed\"}" }
        val adapter = XrayAdapter(core)
        val thrown = runCatching { adapter.start(cfg()) }.exceptionOrNull()
        assertTrue(thrown is EngineFailureException)
        assertTrue((thrown as EngineFailureException).failure is Failure.AuthenticationFailure)
    }

    @Test
    fun tunModePassesFdThroughEnv() = runBlocking {
        val core = FakeCore { ok }
        val adapter = XrayAdapter(core, mtu = 1400)
        adapter.prepare()
        adapter.start(
            EngineConfig(
                ConfigFormat.VLESS_LINK, link,
                runtime = mapOf(dev.tunnelkit.core.engine.EngineConfig.RUNTIME_TUN_FD to "42"),
            )
        )
        val request = core.requests.last()
        assertTrue(request.contains("xray.tun.fd"))
        assertTrue(request.contains("tun"))
        assertTrue(request.contains("42"))
        assertTrue(request.contains("1400"))
    }

    @Test
    fun stopSendsStopXrayAndResetsDns() = runBlocking {
        val core = FakeCore { ok }
        val adapter = XrayAdapter(core, dnsServer = "1.1.1.1:53")
        adapter.prepare()
        adapter.start(cfg())
        adapter.stop()

        assertTrue(core.requests.any { it.contains("\"method\":\"stopXray\"") })
        assertEquals("1.1.1.1:53", core.dnsServer)
        assertTrue(core.resetCount >= 1)
    }
}
