package dev.tunnelkit.core.config

import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultRouteTest {
    private val outbound = """{"tag":"proxy","protocol":"vless","settings":{}}"""

    @Test
    fun proxyIsTheFirstOutbound() {
        // Xray falls back to outbounds[0] when no rule matches. If "direct" sits there, every
        // packet leaves unproxied while the UI still says Connected.
        listOf(XrayConfigBuilder.socksConfig(outbound), XrayConfigBuilder.tunConfig(outbound, fd = 7)).forEach {
            val proxy = it.indexOf("\"tag\":\"proxy\"")
            val direct = it.indexOf("\"tag\":\"direct\"")
            assertTrue("proxy must precede direct", proxy in 1..<direct)
        }
    }

    @Test
    fun catchAllRuleRoutesToProxy() {
        val cfg = XrayConfigBuilder.tunConfig(outbound, fd = 7)
        assertTrue(cfg.contains("\"network\":\"tcp,udp\",\"outboundTag\":\"proxy\""))
    }
}
