package dev.tunnelkit.engine.xray

import dev.tunnelkit.core.model.Failure

/**
 * The narrow surface we use from libXray. Keeping it an interface is what lets the adapter be
 * unit-tested on the JVM without loading the Go runtime.
 */
interface XrayCore {
    val apiVersion: Long

    /** libXray `Invoke`: one JSON envelope in, one JSON envelope out. */
    fun invoke(requestJson: String): String

    /** Android-specific: make Go's resolver use the VPN's DNS server. */
    fun setDns(server: String)

    fun resetDns()

    /** Register the VpnService.protect() bridge so Go sockets do not re-enter the tunnel. */
    fun registerProtector(protect: (Long) -> Boolean)
}

/** Envelope helpers — the wire format of libXray API v3. */
object XrayProtocol {

    fun runRequest(xrayJson: String, apiVersion: Long = 3): String =
        """{"apiVersion":$apiVersion,"method":"runXray","payload":{"xrayJson":${quote(xrayJson)}}}"""

    /** Batch probe: libXray runs each outbound and fetches [url] through it. */
    fun pingBatchRequest(
        configs: List<Pair<String, String>>,   // xrayJson to outboundTag
        url: String,
        timeoutMs: Int,
        apiVersion: Long = 3,
    ): String = buildString {
        append("""{"apiVersion":""").append(apiVersion)
        append(""","method":"pingBatch","payload":{"timeout":""").append(timeoutMs)
        append(""","url":""").append(quote(url)).append(""","configs":[""")
        configs.forEachIndexed { index, (json, tag) ->
            if (index > 0) append(',')
            append("""{"xrayJson":""").append(quote(json))
            append(""","outboundTag":""").append(quote(tag)).append("}")
        }
        append("]}}")
    }

    data class ProbeRow(val ok: Boolean, val delayMs: Long, val error: String)

    fun parsePingBatch(response: String): List<ProbeRow> =
        MiniJson.objects(response, "results").map { row ->
            ProbeRow(
                ok = MiniJson.bool(row, "success") ?: false,
                delayMs = MiniJson.long(row, "delay") ?: -1,
                error = MiniJson.string(row, "error") ?: "",
            )
        }

    fun stopRequest(apiVersion: Long = 3): String =
        """{"apiVersion":$apiVersion,"method":"stopXray","payload":{}}"""

    data class Envelope(val success: Boolean, val error: String)

    fun parse(response: String): Envelope = Envelope(
        success = MiniJson.bool(response, "success") ?: false,
        error = MiniJson.string(response, "error") ?: "",
    )

    /** Map an engine error string onto the app's typed failures. */
    fun failureFor(error: String): Failure {
        val e = error.lowercase()
        return when {
            // Config rejected before any socket is opened: our own generator is at fault, not the server.
            e.contains("failed to parse json config") || e.contains("infra/conf") ||
                e.contains("failed to open ") || e.contains("illegal ip rule") ||
                e.contains("unknown field") ->
                Failure.InvalidConfig(error)
            e.contains("auth") || e.contains("rejected") || e.contains("invalid user") ->
                Failure.AuthenticationFailure(error)
            e.contains("dns") || e.contains("no such host") || e.contains("resolve") ->
                Failure.DnsFailure(error)
            e.contains("tls") || e.contains("certificate") || e.contains("handshake") ->
                Failure.TlsFailure(error)
            // A CDN that accepts TCP and then drops the upgrade: wrong path/Host, or SNI filtering.
            e.contains("failed to dial") && (e.contains("eof") || e.contains("reset")) ->
                Failure.EndpointUnreachable("server closed during the WebSocket upgrade: $error")
            e.contains("timeout") || e.contains("timed out") -> Failure.Timeout(error)
            e.contains("without tls") || e.contains("is prohibited") ->
                Failure.UnsupportedFeature("plaintext VLESS to a public host (needs TLS/REALITY)")
            e.contains("refused") || e.contains("unreachable") || e.contains("network is down") ->
                Failure.EndpointUnreachable(error)
            e.isBlank() -> Failure.Unknown("engine returned failure without a message")
            else -> Failure.Unknown(error)
        }
    }

    internal fun quote(s: String): String = buildString {
        append('"')
        for (c in s) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
        append('"')
    }
}

/** Minimal field reader for the small, controlled envelopes libXray returns. */
internal object MiniJson {

    fun bool(json: String, name: String): Boolean? {
        val i = indexOfField(json, name) ?: return null
        val rest = json.substring(i)
        return when {
            rest.startsWith("true") -> true
            rest.startsWith("false") -> false
            else -> null
        }
    }

    fun long(json: String, name: String): Long? {
        val i = indexOfField(json, name) ?: return null
        val start = i
        var end = i
        while (end < json.length && (json[end].isDigit() || json[end] == '-')) end++
        return json.substring(start, end).toLongOrNull()
    }

    /** Extracts the raw objects of an array field. Values are objects, so brace counting is enough. */
    fun objects(json: String, name: String): List<String> {
        val key = "\"$name\""
        val at = json.indexOf(key)
        if (at < 0) return emptyList()
        val arrStart = json.indexOf('[', at)
        if (arrStart < 0) return emptyList()
        val out = mutableListOf<String>()
        var i = arrStart + 1
        while (i < json.length) {
            if (json[i] == '{') {
                val start = i
                var depth = 0
                var inString = false
                var escaped = false
                while (i < json.length) {
                    val c = json[i]
                    if (inString) {
                        if (escaped) escaped = false
                        else if (c == '\\') escaped = true
                        else if (c == '"') inString = false
                    } else {
                        when (c) {
                            '"' -> inString = true
                            '{' -> depth++
                            '}' -> { depth--; if (depth == 0) { out += json.substring(start, i + 1); break } }
                        }
                    }
                    i++
                }
            } else if (json[i] == ']') {
                break
            }
            i++
        }
        return out
    }

    fun string(json: String, name: String): String? {
        val i = indexOfField(json, name) ?: return null
        var p = json.indexOf('"', i)
        if (p < 0) return null
        p++
        val sb = StringBuilder()
        while (p < json.length) {
            val c = json[p]
            when (c) {
                '\\' -> {
                    if (p + 1 >= json.length) return sb.toString()
                    when (val n = json[p + 1]) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        'u' -> {
                            val hex = json.substring(p + 2, minOf(p + 6, json.length))
                            sb.append(hex.toIntOrNull(16)?.toChar() ?: '?')
                            p += 4
                        }
                        else -> sb.append(n)
                    }
                    p += 2
                }
                '"' -> return sb.toString()
                else -> { sb.append(c); p++ }
            }
        }
        return sb.toString()
    }

    private fun indexOfField(json: String, name: String): Int? {
        val key = "\"$name\""
        val at = json.indexOf(key)
        if (at < 0) return null
        var i = at + key.length
        while (i < json.length && json[i] != ':') i++
        i++
        while (i < json.length && json[i].isWhitespace()) i++
        return i
    }
}
