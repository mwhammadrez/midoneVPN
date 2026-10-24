package dev.tunnelkit.core.config

import org.junit.Test
import java.io.File

/** Writes the exact client configs this app generates, so they can be handed to a real server. */
class EmitFixturesTest {
    @Test
    fun emit() {
        val dir = File("/tmp/gen").apply { mkdirs() }
        val link = VlessLink.parse(
            "vless://5783a3e7-e373-51cd-8642-c83782b807c5@127.0.0.1:8080" +
                "?encryption=none&security=none&type=ws&path=%2Fws#local-ws"
        ).getOrThrow()
        // Production config, but private IPs must reach the outbound during this test.
        File(dir, "client-raw.json").writeText(link.toXrayJson())
        File(dir, "client-outbound.json").writeText(link.toOutboundJson())
    }
}
