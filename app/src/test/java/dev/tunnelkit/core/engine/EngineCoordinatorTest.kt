package dev.tunnelkit.core.engine

import dev.tunnelkit.core.model.ConnectionState
import dev.tunnelkit.core.model.EngineId
import dev.tunnelkit.core.model.Failure
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineCoordinatorTest {

    private class FakeEngine(
        id: String,
        private val valid: Boolean = true,
        private val failOnStart: Throwable? = null,
    ) : VpnEngine {
        override val descriptor = EngineDescriptor(
            id = EngineId(id),
            engineName = "fake",
            engineVersion = "0",
            provenanceUrl = "",
            provenanceCommit = "",
            licenseId = "MIT",
            formats = setOf(ConfigFormat.VLESS_LINK),
            tunOwnership = TunOwnership.LOCAL_PROXY,
            supportsUdp = true,
            status = IntegrationStatus.IMPLEMENTED_BUT_UNVERIFIED,
        )
        var prepared = 0
        var started = 0
        var stopped = 0

        override suspend fun validate(config: EngineConfig) =
            if (valid) ValidationResult.Valid else ValidationResult.invalid("bad")

        override suspend fun prepare() { prepared++ }
        override suspend fun start(config: EngineConfig) {
            started++
            failOnStart?.let { throw it }
        }
        override suspend fun stop() { stopped++ }
    }

    private val cfg = EngineConfig(ConfigFormat.VLESS_LINK, "vless://x")

    @Test
    fun connectSuccess() = runBlocking {
        val a = FakeEngine("a")
        val c = EngineCoordinator(mapOf(EngineId("a") to a))
        c.connect(EngineId("a"), cfg)
        assertTrue(c.state.value is ConnectionState.Connected)
        assertEquals(1, a.started)
        assertEquals(EngineId("a"), c.activeEngineId())
    }

    @Test
    fun invalidConfigNeverStarts() = runBlocking {
        val a = FakeEngine("a", valid = false)
        val c = EngineCoordinator(mapOf(EngineId("a") to a))
        c.connect(EngineId("a"), cfg)
        val s = c.state.value as ConnectionState.Failed
        assertEquals(Failure.InvalidConfig, s.failure)
        assertEquals(0, a.started)
    }

    @Test
    fun startFailureCleansUp() = runBlocking {
        val a = FakeEngine("a", failOnStart = java.net.ConnectException("refused"))
        val c = EngineCoordinator(mapOf(EngineId("a") to a))
        c.connect(EngineId("a"), cfg)
        val s = c.state.value as ConnectionState.Failed
        assertTrue(s.failure is Failure.EndpointUnreachable)
        assertEquals(1, a.stopped)
        assertNull(c.activeEngineId())
    }

    @Test
    fun switchingStopsThePreviousEngine() = runBlocking {
        val a = FakeEngine("a")
        val b = FakeEngine("b")
        val c = EngineCoordinator(mapOf(EngineId("a") to a, EngineId("b") to b))
        c.connect(EngineId("a"), cfg)
        c.switchTo(EngineId("b"), cfg)
        assertEquals(1, a.stopped)
        assertEquals(1, b.started)
        assertEquals(EngineId("b"), c.activeEngineId())
    }

    @Test
    fun disconnectIsIdempotent() = runBlocking {
        val a = FakeEngine("a")
        val c = EngineCoordinator(mapOf(EngineId("a") to a))
        c.connect(EngineId("a"), cfg)
        c.disconnect()
        c.disconnect()
        assertTrue(c.state.value is ConnectionState.Stopped)
        assertEquals(1, a.stopped)
    }

    @Test
    fun unknownEngineFailsTyped() = runBlocking {
        val c = EngineCoordinator(emptyMap())
        c.connect(EngineId("nope"), cfg)
        val s = c.state.value as ConnectionState.Failed
        assertTrue(s.failure is Failure.UnsupportedFeature)
    }

    @Test
    fun reconnectSameEngineDoesNotRestart() = runBlocking {
        val a = FakeEngine("a")
        val c = EngineCoordinator(mapOf(EngineId("a") to a))
        c.connect(EngineId("a"), cfg)
        c.connect(EngineId("a"), cfg)
        assertEquals(1, a.started)
        assertEquals(0, a.stopped)
    }

    @Test
    fun licenseBlockedBecomesBlockedState() = runBlocking {
        val a = FakeEngine("a", failOnStart = EngineBlockedException("GPL-3.0"))
        val c = EngineCoordinator(mapOf(EngineId("a") to a))
        c.connect(EngineId("a"), cfg)
        val s = c.state.value as ConnectionState.Blocked
        assertEquals(Failure.LicenseBlocked, s.failure)
    }
}
