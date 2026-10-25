package dev.tunnelkit.core

/**
 * Parses a vless:// share link into a validated model and renders the Xray client config.
 *
 * Pure Kotlin, zero Android dependencies -> unit-testable on the JVM.
 * Field names follow Xray 26.x: the Reality client key is `password` (formerly `publicKey`),
 * the URI parameter is still `pbk`.
 */
data class VlessLink(
    val uuid: String,
    val host: String,
    val port: Int,
    val label: String = "",
    val security: String = "none",      // reality | tls | none
    val network: String = "tcp",        // tcp | ws | grpc | xhttp
    val flow: String? = null,
    val sni: String? = null,
    val fingerprint: String = "chrome",
    val publicKey: String? = null,
    val shortId: String? = null,
    val spiderX: String = "/",
    val path: String? = null,
    val hostHeader: String? = null,
    val serviceName: String? = null,
    val xhttpMode: String? = null,
) {

    /** Returns a list of human-readable problems; empty means the link is usable. */
    fun validate(): List<String> = buildList {
        if (!UUID_REGEX.matches(uuid)) add("uuid is not a valid UUID")
        if (host.isBlank()) add("host is empty")
        if (port !in 1..65535) add("port out of range: $port")
        if (security == "reality") {
            if (publicKey.isNullOrBlank()) add("reality needs pbk (public key)")
            if (sni.isNullOrBlank()) add("reality needs sni")
        }
        if (network == "ws" && path.isNullOrBlank()) add("ws needs a path")
        if (network == "grpc" && serviceName.isNullOrBlank()) add("grpc needs serviceName")
        if (network != "tcp" && flow == "xtls-rprx-vision") {
            add("flow xtls-rprx-vision only works on tcp; drop it or switch network to tcp")
        }
    }

    fun isUsable(): Boolean = validate().isEmpty()

    /** Xray client config for `runXray`. Plain concatenation - no org.json (JVM-testable). */
    fun toXrayJson(): String = buildString {
        append("{\"log\":{\"loglevel\":\"warning\"},")
        append("\"inbounds\":[{\"tag\":\"socks-in\",\"listen\":\"127.0.0.1\",\"port\":10808,")
        append("\"protocol\":\"socks\",\"settings\":{\"udp\":true,\"auth\":\"noauth\"}}],")
        append("\"outbounds\":[{\"tag\":\"direct\",\"protocol\":\"freedom\"},")
        append("{\"tag\":\"proxy\",\"protocol\":\"vless\",\"settings\":{")
        append("\"address\":").append(quote(host))
        append(",\"port\":").append(port)
        append(",\"id\":").append(quote(uuid))
        append(",\"encryption\":\"none\"")
        if (!flow.isNullOrBlank()) append(",\"flow\":").append(quote(flow))
        append("},\"streamSettings\":{")
        append("\"network\":").append(quote(network))
        append(",\"security\":").append(quote(security))

        if (security == "reality") {
            append(",\"realitySettings\":{")
            append("\"serverName\":").append(quote(sni ?: ""))
            append(",\"fingerprint\":").append(quote(fingerprint))
            append(",\"password\":").append(quote(publicKey ?: ""))
            append(",\"publicKey\":").append(quote(publicKey ?: ""))   // pre-26.x cores
            append(",\"shortId\":").append(quote(shortId ?: ""))
            append(",\"spiderX\":").append(quote(spiderX))
            append("}")
        }
        if (security == "tls") {
            append(",\"tlsSettings\":{")
            append("\"serverName\":").append(quote(sni ?: host))
            append(",\"fingerprint\":").append(quote(fingerprint))
            append("}")
        }
        when (network) {
            "ws" -> {
                append(",\"wsSettings\":{\"path\":").append(quote(path ?: "/"))
                if (!hostHeader.isNullOrBlank()) {
                    append(",\"headers\":{\"Host\":").append(quote(hostHeader)).append("}")
                }
                append("}")
            }
            "grpc" -> append(",\"grpcSettings\":{\"serviceName\":")
                .append(quote(serviceName ?: "")).append("}")
            "xhttp" -> {
                append(",\"xhttpSettings\":{\"path\":").append(quote(path ?: "/"))
                append(",\"mode\":").append(quote(xhttpMode ?: "auto"))
                if (!hostHeader.isNullOrBlank()) append(",\"host\":").append(quote(hostHeader))
                append("}")
            }
        }
        append("}}],\"routing\":{\"domainStrategy\":\"AsIs\",\"rules\":[")
        append("{\"type\":\"field\",\"ip\":[\"geoip:private\"],\"outboundTag\":\"direct\"}]}}")
    }

    companion object {
        private val UUID_REGEX =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

        fun parse(link: String): Result<VlessLink> = runCatching {
            val raw = link.trim()
            require(raw.startsWith("vless://", ignoreCase = true)) { "not a vless:// link" }

            val withoutScheme = raw.substring("vless://".length)
            val hashIndex = withoutScheme.indexOf('#')
            val label = if (hashIndex >= 0) decode(withoutScheme.substring(hashIndex + 1)) else ""
            val body = if (hashIndex >= 0) withoutScheme.substring(0, hashIndex) else withoutScheme

            val queryIndex = body.indexOf('?')
            val authority = if (queryIndex >= 0) body.substring(0, queryIndex) else body
            val query = if (queryIndex >= 0) body.substring(queryIndex + 1) else ""

            val at = authority.lastIndexOf('@')
            require(at > 0) { "missing uuid@host" }
            val uuid = authority.substring(0, at)
            val hostPort = authority.substring(at + 1)

            val (host, port) = splitHostPort(hostPort)
            val params = parseQuery(query)
            val security = params["security"] ?: "none"
            val network = params["type"] ?: "tcp"

            VlessLink(
                uuid = uuid,
                host = host,
                port = port,
                label = label,
                security = security,
                network = network,
                flow = params["flow"]?.takeIf { it.isNotBlank() },
                sni = params["sni"] ?: params["host"],
                fingerprint = params["fp"] ?: "chrome",
                publicKey = params["pbk"],
                shortId = params["sid"],
                spiderX = params["spx"] ?: "/",
                path = params["path"],
                hostHeader = params["host"],
                serviceName = params["serviceName"],
                xhttpMode = params["mode"],
            )
        }

        private fun splitHostPort(value: String): Pair<String, Int> {
            if (value.startsWith("[")) {                       // [ipv6]:port
                val end = value.indexOf(']')
                require(end > 0) { "malformed IPv6 host" }
                val host = value.substring(1, end)
                val port = if (end + 1 < value.length) value.substring(end + 2).toIntOrNull() else null
                return host to (port ?: 443)
            }
            val colon = value.lastIndexOf(':')
            require(colon > 0) { "missing port" }
            return value.substring(0, colon) to value.substring(colon + 1).toInt()
        }

        private fun parseQuery(query: String): Map<String, String> =
            query.split('&').filter { it.isNotBlank() }.mapNotNull { pair ->
                val eq = pair.indexOf('=')
                if (eq < 0) pair.lowercase() to "" else
                    pair.substring(0, eq).lowercase() to decode(pair.substring(eq + 1))
            }.toMap()

        private fun decode(s: String): String = try {
            java.net.URLDecoder.decode(s, "UTF-8")
        } catch (_: Exception) {
            s
        }

        private fun quote(s: String): String {
            val sb = StringBuilder("\"")
            for (c in s) {
                when (c) {
                    '"' -> sb.append("\\\"")
                    '\\' -> sb.append("\\\\")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    else -> if (c < ' ') sb.append(String.format("\\u%04x", c.code)) else sb.append(c)
                }
            }
            return sb.append('"').toString()
        }
    }
}
