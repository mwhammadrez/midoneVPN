package dev.tunnelkit.core.config

import org.junit.Test
import java.io.File

/** Emits a client config for whatever link is in MIDONE_LINK (operator debugging aid). */
class EmitUserLinkTest {
    @Test
    fun emit() {
        val raw = System.getenv("MIDONE_LINK") ?: return
        val link = VlessLink.parse(raw).getOrElse { error("parse failed: ${it.message}") }
        File("/tmp/gen").mkdirs()
        File("/tmp/gen/user.json").writeText(link.toXrayJson())
        File("/tmp/gen/user-meta.txt").writeText(
            "validate=${link.validate()}\nwarnings=${link.warnings()}\n" +
                "twin=${link.toShareLink(forceTls = true)}\n"
        )
    }
}
