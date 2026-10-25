package dev.tunnelkit.core.net

import dev.tunnelkit.core.config.VlessLink
import dev.tunnelkit.core.model.Failure
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Checks the endpoint *before* the engine starts.
 *
 * Xray reports success as soon as its config loads; the outbound connects lazily on the first
 * packet. That is why a dead CDN still produces "Connected" in the UI. This preflight turns a
 * silent no-op into a specific, actionable failure.
 */
object Preflight {

    data class Result(
        val tcpMs: Int?,
        val tls: Boolean?,
        val httpStatus: Int?,
        val failure: Failure?,
        val reason: String? = null,
    ) {
        fun describe(): String =
            "preflight tcp=${tcpMs?.let { "${it}ms" } ?: "fail"}" +
                (reason?.let { "($it)" } ?: "") +
                " tls=${tls?.toString() ?: "n/a"} http=${httpStatus ?: "none"}" +
                (failure?.let { " -> ${it.code}" } ?: "")
    }

    /**
     * Every way this profile could plausibly be reached, best first.
     *
     * CDN-fronted links often carry the CDN's own apex as the address while the real site sits in the
     * Host header. If that combination is refused, the same credentials usually work when we dial the
     * site's own domain over TLS/443 — so the app tries both instead of asking the user to guess.
     */
    fun candidates(link: VlessLink): List<VlessLink> = buildList {
        add(link)
        val header = link.hostHeader?.takeIf { it.isNotBlank() && it != link.host }
        if (link.security == "none" && !link.isPrivateHost()) {
            add(link.copy(port = 443, security = "tls", sni = header ?: link.host))
        }
        if (header != null && link.security != "reality") {
            add(link.copy(host = header, port = 443, security = "tls", sni = header))
        }
    }

    /** Tries each candidate and returns the first that answers, or the furthest one with its failure. */
    fun firstReachable(link: VlessLink, timeoutMs: Int = 5000): Pair<VlessLink, Result> {
        var best: Pair<VlessLink, Result>? = null
        val tried = mutableListOf<String>()
        for (candidate in candidates(link)) {
            val r = check(candidate, timeoutMs)
            tried += "${candidate.host}:${candidate.port}/${candidate.security} ${r.describe()}"
            if (r.failure == null) return candidate to r
            // keep the candidate that got furthest (tcp, then tls, then http)
            val score = (if (r.tcpMs != null) 1 else 0) + (if (r.tls == true) 1 else 0) + (if (r.httpStatus != null) 1 else 0)
            val bestScore = best?.let { (_, b) ->
                (if (b.tcpMs != null) 1 else 0) + (if (b.tls == true) 1 else 0) + (if (b.httpStatus != null) 1 else 0)
            } ?: -1
            if (score > bestScore) best = candidate to r
        }
        val (c, r) = best ?: (link to check(link, timeoutMs))
        return c to r.copy(reason = r.reason)
    }

    fun check(link: VlessLink, timeoutMs: Int = 6000): Result {
        // 1. TCP
        val started = System.currentTimeMillis()
        val socket = Socket()
        val tcpError = runCatching {
            socket.connect(InetSocketAddress(link.host, link.port), timeoutMs)
        }.exceptionOrNull()
        val tcpMs = (System.currentTimeMillis() - started).toInt()
        if (tcpError != null) {
            runCatching { socket.close() }
            val why = tcpError.javaClass.simpleName +
                (tcpError.message?.let { ": ${it.take(60)}" } ?: "")
            return Result(
                null, null, null,
                Failure.EndpointUnreachable("tcp ${link.host}:${link.port} — $why"),
                reason = why,
            )
        }

        // 2. TLS (REALITY cannot be preflighted with a plain TLS handshake)
        var tlsResult: Boolean? = null
        var stream: Socket = socket
        if (link.security == "tls") {
            val sni = link.sni ?: link.hostHeader ?: link.host
            tlsResult = runCatching {
                val s = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                    .createSocket(socket, sni, link.port, true) as SSLSocket
                s.sslParameters = s.sslParameters.apply {
                    serverNames = listOf(SNIHostName(sni))
                    applicationProtocols = arrayOf("http/1.1")
                }
                s.soTimeout = timeoutMs
                s.startHandshake()
                stream = s
                true
            }.getOrElse { false }
            if (tlsResult != true) {
                runCatching { socket.close() }
                return Result(tcpMs, false, null, Failure.TlsFailure("tls handshake failed with SNI $sni"))
            }
        } else if (link.security == "reality") {
            // leave as null: reality has its own handshake the JVM cannot imitate
        }

        // 3. one HTTP request on the transport path — any status line proves the CDN routes us
        val status = runCatching {
            val path = link.path ?: "/"
            val hostHeader = link.hostHeader ?: link.host
            val out = """
                GET $path HTTP/1.1
                Host: $hostHeader
                User-Agent: Mozilla/5.0
                Connection: close

            """.trimIndent().replace("\n", "\r\n") + "\r\n\r\n"
            stream.getOutputStream().write(out.toByteArray())
            stream.getOutputStream().flush()
            val line = stream.getInputStream().bufferedReader().readLine() ?: ""
            Regex("""HTTP/1\.[01] (\d{3})""").find(line)?.groupValues?.get(1)?.toInt()
        }.getOrNull()
        runCatching { stream.close() }

        // Any status line proves the CDN routed our request. A 4xx here is normal: a WebSocket
        // endpoint answers 400/404 to a plain GET without the Upgrade headers.
        val failure = if (status == null) {
            Failure.Unknown("no HTTP response from ${link.host}${link.path ?: "/"}")
        } else null
        return Result(tcpMs, tlsResult, status, failure)
    }
}
