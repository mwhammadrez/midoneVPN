package dev.tunnelkit.core.net

import dev.tunnelkit.core.config.VlessLink
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Hits the real internet; enable with RUN_NET_TESTS=1. */
class PreflightLiveTest {
    @Test
    fun reportsTcpTlsAndHttpSeparately() {
        assumeTrue(System.getenv("RUN_NET_TESTS") == "1")
        val link = VlessLink.parse(
            "vless://5783a3e7-e373-51cd-8642-c83782b807c5@www.gstatic.com:443" +
                "?encryption=none&security=tls&sni=www.gstatic.com&type=ws&path=%2F&host=www.gstatic.com#t"
        ).getOrThrow()
        val r = Preflight.check(link, timeoutMs = 8000)
        println(r.describe())
        assertNotNull("tcp must connect", r.tcpMs)
        assertTrue("tls must complete", r.tls == true)
        assertNotNull("server must answer HTTP", r.httpStatus)
    }
}
