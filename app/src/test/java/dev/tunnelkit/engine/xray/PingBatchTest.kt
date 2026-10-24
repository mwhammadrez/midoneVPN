package dev.tunnelkit.engine.xray

import dev.tunnelkit.core.net.ProbeRunner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PingBatchTest {

    private class FakeCore(private val response: String) : XrayCore {
        override val apiVersion = 3L
        var lastRequest: String = ""
        override fun invoke(requestJson: String): String { lastRequest = requestJson; return response }
        override fun setDns(server: String) {}
        override fun resetDns() {}
        override fun registerProtector(protect: (Long) -> Boolean) {}
    }

    @Test
    fun requestCarriesConfigsTimeoutAndUrl() {
        val req = XrayProtocol.pingBatchRequest(
            configs = listOf("""{"outbounds":[]}""" to "proxy"),
            url = "https://www.gstatic.com/generate_204",
            timeoutMs = 3000,
        )
        assertTrue(req.contains("\"method\":\"pingBatch\""))
        assertTrue(req.contains("\"configs\""))
        assertTrue(req.contains("\"outboundTag\":\"proxy\""))
        assertTrue(req.contains("\"timeout\":3000"))
        assertTrue(req.contains("generate_204"))
    }

    @Test
    fun parsesResultRows() {
        val response = """{"success":true,"data":{"results":[{"success":true,"delay":128,"error":""},
            |{"success":false,"delay":-1,"error":"timeout"}]},"error":""}""".trimMargin()
        val rows = XrayProtocol.parsePingBatch(response)
        assertEquals(2, rows.size)
        assertTrue(rows[0].ok)
        assertEquals(128, rows[0].delayMs)
        assertEquals("timeout", rows[1].error)
    }

    @Test
    fun runnerReturnsEngineFailure() = runBlocking {
        val core = FakeCore("""{"success":false,"data":null,"error":"bad config"}""")
        val probe = XrayProbeRunner(core).probe(XrayProbeRunner.Payload("{}"), "https://x", 1000)
        assertEquals(false, probe.ok)
        assertEquals("bad config", probe.error)
    }

    @Test
    fun runnerMapsSuccessRow() = runBlocking {
        val core = FakeCore("""{"success":true,"data":{"results":[{"success":true,"delay":42,"error":""}]},"error":""}""")
        val probe = XrayProbeRunner(core).probe(XrayProbeRunner.Payload("{}"), "https://x", 1000)
        assertEquals(true, probe.ok)
        assertEquals(42, probe.delayMs)
        assertTrue(core.lastRequest.contains("pingBatch"))
    }
}
