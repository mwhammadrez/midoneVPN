package dev.tunnelkit.core.config

import org.junit.Test
import java.io.File

/** Writes the app's tun config with a placeholder fd so a real core can be handed a live device. */
class EmitTunFixtureTest {
    @Test
    fun emit() {
        val l = VlessLink.parse(
            "vless://5783a3e7-e373-51cd-8642-c83782b807c5@127.0.0.1:8080" +
                "?encryption=none&security=none&type=ws&path=%2Fws&host=localhost#local-tun"
        ).getOrThrow()
        File("/tmp/gen").mkdirs()
        File("/tmp/gen/tun-app.json").writeText(
            XrayConfigBuilder.tunConfig(l.toOutboundJson(), fd = 0, mtu = 1400, tunName = "tun9")
        )
    }
}
