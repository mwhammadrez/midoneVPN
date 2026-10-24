package dev.tunnelkit.core.config

import org.junit.Test
import java.io.File

class EmitTlsFixtureTest {
    @Test
    fun emit() {
        val l = VlessLink.parse(
            "vless://5783a3e7-e373-51cd-8642-c83782b807c5@127.0.0.1:8443" +
                "?encryption=none&security=tls&sni=localhost&type=ws&path=%2Fws&host=localhost#local-tls"
        ).getOrThrow()
        File("/tmp/gen/client-tls.json").writeText(l.toXrayJson())
    }
}
