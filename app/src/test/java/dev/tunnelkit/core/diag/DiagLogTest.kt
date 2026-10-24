package dev.tunnelkit.core.diag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagLogTest {

    @Test
    fun redactsShareLinks() {
        val out = DiagLog.redact("profile=vless://e8e8-1a71@host:80?security=none#x loaded")
        assertFalse(out.contains("e8e8-1a71"))
        assertTrue(out.contains("vless://<redacted>"))
    }

    @Test
    fun redactsUuids() {
        val out = DiagLog.redact("id 5783a3e7-e373-51cd-8642-c83782b807c5 failed")
        assertFalse(out.contains("5783a3e7"))
        assertTrue(out.contains("<uuid>"))
    }

    @Test
    fun redactsKeysAndPasswords() {
        val out = DiagLog.redact("PrivateKey = yAnz5TF+lXXJte14tji3zlMNq+hd2rYUIgJBgB3fBmk=")
        assertFalse(out.contains("yAnz5TF"))
        assertTrue(out.contains("<redacted>") || out.contains("<key>"))

        val out2 = DiagLog.redact("password=hunter2 host=x")
        assertFalse(out2.contains("hunter2"))
    }

    @Test
    fun bufferIsBoundedAndOrdered() {
        DiagLog.clear()
        repeat(250) { DiagLog.event("event $it") }
        val lines = DiagLog.lines.value
        assertEquals(200, lines.size)
        assertTrue(lines.last().contains("event 249"))
        assertFalse(lines.any { it.contains("event 0") })
    }

    @Test
    fun dumpIsCopyableText() {
        DiagLog.clear()
        DiagLog.header("0.7 · Pixel · Android 14 · arm64-v8a")
        DiagLog.event("state=Connected")
        val dump = DiagLog.dump()
        assertTrue(dump.contains("MidONe 0.7"))
        assertTrue(dump.contains("state=Connected"))
        assertTrue(dump.lines().size >= 2)
    }
}
