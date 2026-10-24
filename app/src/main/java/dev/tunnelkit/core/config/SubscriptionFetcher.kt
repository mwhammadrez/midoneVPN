package dev.tunnelkit.core.config

import java.net.InetAddress
import java.net.URI

/** Transport seam so the fetcher is testable without a network. */
interface HttpFetcher {
    data class Response(val statusCode: Int, val body: String, val finalUrl: String)
    fun get(url: String, maxBytes: Int): Response
}

/**
 * SSRF guard. A subscription URL is untrusted input; it must not be able to make the app talk to
 * loopback, link-local or private ranges — directly or through a redirect.
 */
object UrlGuard {

    fun check(url: String, resolver: (String) -> List<InetAddress>, allowHttp: Boolean = false): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return "malformed URL"
        val scheme = uri.scheme?.lowercase() ?: return "missing scheme"
        if (scheme != "https" && !(allowHttp && scheme == "http")) return "scheme not allowed: $scheme"

        val host = uri.host ?: return "missing host"
        if (host.equals("localhost", ignoreCase = true)) return "loopback host rejected"

        val addresses = runCatching { resolver(host) }.getOrElse { return "cannot resolve host" }
        if (addresses.isEmpty()) return "cannot resolve host"
        for (address in addresses) {
            if (isForbidden(address)) return "target resolves to a non-public address: ${address.hostAddress}"
        }
        return null
    }

    fun isForbidden(address: InetAddress): Boolean =
        address.isAnyLocalAddress ||
            address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress ||
            address.isMulticastAddress ||
            isCGNAT(address) ||
            isUniqueLocalV6(address)

    private fun isCGNAT(a: InetAddress): Boolean {
        val b = a.address
        return b.size == 4 && (b[0].toInt() and 0xFF) == 100 && (b[1].toInt() and 0xC0) == 64
    }

    private fun isUniqueLocalV6(a: InetAddress): Boolean {
        val b = a.address
        return b.size == 16 && (b[0].toInt() and 0xFE) == 0xFC
    }
}

/**
 * Fetches and parses a subscription with hard bounds.
 * On any failure the last known-good body is returned instead of destroying the user's profile.
 */
class SubscriptionFetcher(
    private val http: HttpFetcher,
    private val resolver: (String) -> List<InetAddress>,
    private val allowHttp: Boolean = false,
    private val onCacheWrite: (url: String, body: String) -> Unit = { _, _ -> },
    private val onCacheRead: (url: String) -> String? = { null },
) {

    sealed interface Outcome {
        data class Fresh(val parsed: SubscriptionParser.Result) : Outcome
        data class FromCache(val parsed: SubscriptionParser.Result, val reason: String) : Outcome
        data class Failed(val reason: String) : Outcome
    }

    fun refresh(url: String): Outcome {
        UrlGuard.check(url, resolver, allowHttp)?.let { reason ->
            return fallback(url, reason)
        }

        val response = runCatching { http.get(url, MAX_BYTES) }.getOrElse { e ->
            return fallback(url, "fetch failed: ${e.javaClass.simpleName}")
        }

        // A redirect is a new target: re-check it.
        if (response.finalUrl != url) {
            UrlGuard.check(response.finalUrl, resolver, allowHttp)?.let { reason ->
                return fallback(url, "redirect rejected: $reason")
            }
        }

        if (response.statusCode !in 200..299) return fallback(url, "HTTP ${response.statusCode}")
        if (response.body.length > MAX_BYTES) return fallback(url, "body exceeds $MAX_BYTES bytes")

        val parsed = runCatching { SubscriptionParser.parse(response.body) }.getOrElse { e ->
            return fallback(url, "parse failed: ${e.message ?: e.javaClass.simpleName}")
        }
        if (parsed.links.isEmpty()) return fallback(url, "no usable links")

        onCacheWrite(url, response.body)
        return Outcome.Fresh(parsed)
    }

    private fun fallback(url: String, reason: String): Outcome {
        val cached = runCatching { onCacheRead(url) }.getOrNull() ?: return Outcome.Failed(reason)
        val parsed = runCatching { SubscriptionParser.parse(cached) }.getOrNull()
            ?: return Outcome.Failed(reason)
        return Outcome.FromCache(parsed, reason)
    }

    companion object {
        const val MAX_BYTES = 2 * 1024 * 1024
    }
}
