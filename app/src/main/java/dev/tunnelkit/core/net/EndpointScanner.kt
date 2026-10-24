package dev.tunnelkit.core.net

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Bounded endpoint testing.
 *
 * The runner is expected to perform a *real* protocol probe (for Xray: `pingBatch`, which
 * establishes the outbound and fetches a URL through it), never a bare TCP connect. A TCP
 * handshake says nothing about whether a tunnel works.
 */
interface ProbeRunner {
    data class Probe(val ok: Boolean, val delayMs: Long, val error: String)

    /** Must respect [timeoutMs] and must not throw for ordinary network failures. */
    suspend fun probe(config: Any, url: String, timeoutMs: Int): Probe

    /** Cooperative cancellation point for implementations that run native calls. */
    fun cancel()
}

class EndpointScanner(
    private val runner: ProbeRunner,
    private val concurrency: Int = 4,
) {

    data class Budget(
        val maxTargets: Int = 64,
        val timeoutPerTargetMs: Int = 4_000,
        val totalBudgetMs: Long = 60_000,
        val probeUrl: String = "https://www.gstatic.com/generate_204",
    )

    data class Target(val id: String, val payload: Any)

    data class Result(val id: String, val ok: Boolean, val delayMs: Long, val error: String)

    data class Report(
        val results: List<Result>,      // ranked: successes by delay, then failures
        val truncated: Boolean,
        val timedOut: Boolean,
        val testedCount: Int,
    )

    /**
     * @param onProgress called after each finished target (0-based count).
     */
    suspend fun scan(
        targets: List<Target>,
        budget: Budget = Budget(),
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): Report {
        val unique = targets.distinctBy { it.id }
        val selected = unique.take(budget.maxTargets)
        val truncated = unique.size > selected.size

        var done = 0
        val results = ArrayList<Result>(selected.size)

        val finished = withTimeoutOrNull(budget.totalBudgetMs) {
            coroutineScope {
                selected.map { target ->
                    async {
                        val probe = try {
                            runner.probe(target.payload, budget.probeUrl, budget.timeoutPerTargetMs)
                        } catch (c: CancellationException) {
                            throw c
                        } catch (t: Throwable) {
                            ProbeRunner.Probe(false, -1, t.javaClass.simpleName)
                        }
                        synchronized(results) {
                            results += Result(target.id, probe.ok, probe.delayMs, probe.error)
                            done++
                            onProgress(done, selected.size)
                        }
                    }
                }.awaitAll()
            }
        }

        val timedOut = finished == null
        if (timedOut) runner.cancel()

        val ranked = results.sortedWith(
            compareBy<Result> { !it.ok }.thenBy { if (it.delayMs < 0) Long.MAX_VALUE else it.delayMs }
        )
        return Report(ranked, truncated, timedOut, results.size)
    }
}
