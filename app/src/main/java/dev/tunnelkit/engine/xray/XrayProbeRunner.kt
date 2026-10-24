package dev.tunnelkit.engine.xray

import dev.tunnelkit.core.net.ProbeRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real probe: asks libXray to bring up the outbound and fetch [url] through it (`pingBatch`).
 * Success therefore means "a tunnel carried traffic", not "a port answered".
 */
class XrayProbeRunner(private val core: XrayCore) : ProbeRunner {

    data class Payload(val xrayJson: String, val outboundTag: String = "proxy")

    override suspend fun probe(config: Any, url: String, timeoutMs: Int): ProbeRunner.Probe =
        withContext(Dispatchers.IO) {
            val payload = config as? Payload
                ?: return@withContext ProbeRunner.Probe(false, -1, "unsupported payload")
            val request = XrayProtocol.pingBatchRequest(
                configs = listOf(payload.xrayJson to payload.outboundTag),
                url = url,
                timeoutMs = timeoutMs,
                apiVersion = core.apiVersion,
            )
            val response = core.invoke(request)
            val envelope = XrayProtocol.parse(response)
            if (!envelope.success) {
                return@withContext ProbeRunner.Probe(false, -1, envelope.error.ifBlank { "engine failure" })
            }
            val row = XrayProtocol.parsePingBatch(response).firstOrNull()
                ?: return@withContext ProbeRunner.Probe(false, -1, "empty probe response")
            ProbeRunner.Probe(row.ok, row.delayMs, row.error)
        }

    override fun cancel() {
        runCatching { core.invoke(XrayProtocol.stopRequest(core.apiVersion)) }
    }
}
