package dev.tunnelkit.core.net

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class EndpointScannerTest {

    private class FakeRunner(
        private val behavior: (String) -> ProbeRunner.Probe,
        private val latency: Long = 0,
    ) : ProbeRunner {
        val calls = AtomicInteger(0)
        var cancelled = false
        override suspend fun probe(config: Any, url: String, timeoutMs: Int): ProbeRunner.Probe {
            calls.incrementAndGet()
            if (latency > 0) delay(latency)
            return behavior(config as String)
        }
        override fun cancel() { cancelled = true }
    }

    private fun targets(n: Int) = (1..n).map { EndpointScanner.Target("t$it", "t$it") }

    @Test
    fun ranksSuccessesByDelayThenFailures() = runBlocking {
        val runner = FakeRunner(behavior = { id ->
            when (id) {
                "t1" -> ProbeRunner.Probe(true, 300, "")
                "t2" -> ProbeRunner.Probe(true, 90, "")
                else -> ProbeRunner.Probe(false, -1, "timeout")
            }
        })
        val scanner = EndpointScanner(runner, concurrency = 4)
        val report = scanner.scan(targets(4))
        assertEquals(listOf("t2", "t1", "t3", "t4"), report.results.map { it.id })
        assertEquals(4, report.testedCount)
    }

    @Test
    fun enforcesTargetBudget() = runBlocking {
        val runner = FakeRunner(behavior = { ProbeRunner.Probe(true, 10, "") })
        val scanner = EndpointScanner(runner)
        val report = scanner.scan(targets(10), EndpointScanner.Budget(maxTargets = 3))
        assertEquals(3, report.testedCount)
        assertTrue(report.truncated)
    }

    @Test
    fun deduplicatesTargets() = runBlocking {
        val runner = FakeRunner(behavior = { ProbeRunner.Probe(true, 10, "") })
        val scanner = EndpointScanner(runner)
        val dup = listOf(
            EndpointScanner.Target("same", "a"),
            EndpointScanner.Target("same", "b"),
        )
        val report = scanner.scan(dup)
        assertEquals(1, report.testedCount)
    }

    @Test
    fun totalBudgetCancelsAndReports() = runBlocking {
        val runner = FakeRunner(behavior = { ProbeRunner.Probe(true, 10, "") }, latency = 5_000)
        val scanner = EndpointScanner(runner, concurrency = 2)
        val report = scanner.scan(
            targets(4),
            EndpointScanner.Budget(totalBudgetMs = 300, timeoutPerTargetMs = 10_000),
        )
        assertTrue(report.timedOut)
        assertTrue(runner.cancelled)
        assertEquals(0, report.testedCount)
    }

    @Test
    fun runnerExceptionBecomesFailureNotCrash() = runBlocking {
        val runner = FakeRunner(behavior = { throw IllegalStateException("native crash") })
        val scanner = EndpointScanner(runner)
        val report = scanner.scan(targets(2))
        assertEquals(2, report.testedCount)
        assertFalse(report.results.any { it.ok })
        assertTrue(report.results.all { it.error == "IllegalStateException" })
    }
}
