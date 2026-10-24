package dev.tunnelkit.core.config

/**
 * Parser + validator for standard `wg-quick` configuration text.
 *
 * Safety rules applied here:
 *  - private/preshared keys are `Secret` and never rendered by `toString`;
 *  - unknown sections/fields are reported, never silently dropped;
 *  - malformed input produces typed [Problem]s instead of exceptions.
 */
object WireGuardConfig {

    const val MAX_BYTES = 64 * 1024

    data class Secret(private val value: String) {
        fun reveal(): String = value
        override fun toString(): String = "•••"
    }

    data class Interface(
        val privateKey: Secret?,
        val addresses: List<String>,
        val dns: List<String>,
        val mtu: Int?,
        val listenPort: Int?,
    )

    data class Peer(
        val publicKey: String,
        val presharedKey: Secret?,
        val allowedIps: List<String>,
        val endpoint: String?,
        val persistentKeepalive: Int?,
    )

    data class Config(
        val interfaceSection: Interface,
        val peers: List<Peer>,
        val unknown: List<String>,
    ) {
        fun redactedSummary(): String =
            "wg · ${interfaceSection.addresses.firstOrNull() ?: "-"} · " +
                "${peers.size} peer(s) · ${peers.firstOrNull()?.endpoint ?: "-"}"
    }

    enum class Code {
        OVERSIZED, MALFORMED, NO_INTERFACE, MISSING_PRIVATE_KEY, INVALID_KEY,
        MISSING_ADDRESS, INVALID_ADDRESS, NO_PEER, MISSING_PUBLIC_KEY,
        MISSING_ENDPOINT, INVALID_ENDPOINT, INVALID_ALLOWED_IPS, INVALID_MTU,
        INVALID_KEEPALIVE, UNKNOWN_FIELD,
    }

    data class Problem(val code: Code, val detail: String = "")

    data class ParseResult(val config: Config?, val problems: List<Problem>) {
        val ok: Boolean get() = config != null && problems.none { it.code != Code.UNKNOWN_FIELD }
    }

    fun parse(text: String): ParseResult {
        if (text.length > MAX_BYTES) return ParseResult(null, listOf(Problem(Code.OVERSIZED, "${text.length} bytes")))
        if (text.isBlank()) return ParseResult(null, listOf(Problem(Code.MALFORMED, "empty input")))

        val problems = mutableListOf<Problem>()
        var section: String? = null
        var ifaceKey: Secret? = null
        val addresses = mutableListOf<String>()
        val dns = mutableListOf<String>()
        var mtu: Int? = null
        var listenPort: Int? = null
        val peers = mutableListOf<MutablePeer>()
        val unknown = mutableListOf<String>()
        var sawInterface = false

        text.lineSequence().forEachIndexed { index, rawLine ->
            val line = rawLine.substringBefore('#').substringBefore(';').trim()
            if (line.isEmpty()) return@forEachIndexed

            if (line.startsWith("[") && line.endsWith("]")) {
                val name = line.substring(1, line.length - 1).trim()
                when (name.lowercase()) {
                    "interface" -> { section = "interface"; sawInterface = true }
                    "peer" -> { section = "peer"; peers += MutablePeer() }
                    else -> { unknown += "[$name]"; problems += Problem(Code.UNKNOWN_FIELD, "[$name]"); section = null }
                }
                return@forEachIndexed
            }

            val eq = line.indexOf('=')
            if (eq <= 0) {
                problems += Problem(Code.MALFORMED, "line ${index + 1}: missing '='")
                return@forEachIndexed
            }
            val originalKey = line.substring(0, eq).trim()
            val key = originalKey.lowercase()
            val value = line.substring(eq + 1).trim()

            when (section) {
                "interface" -> when (key) {
                    "privatekey" -> ifaceKey = Secret(value)
                    "address" -> addresses += splitList(value)
                    "dns" -> dns += splitList(value)
                    "mtu" -> mtu = value.toIntOrNull() ?: run {
                        problems += Problem(Code.INVALID_MTU, value); null
                    }
                    "listenport" -> listenPort = value.toIntOrNull()
                    else -> { unknown += "Interface.$originalKey"; problems += Problem(Code.UNKNOWN_FIELD, "Interface.$originalKey") }
                }
                "peer" -> {
                    val peer = peers.lastOrNull()
                    if (peer == null) {
                        problems += Problem(Code.MALFORMED, "key outside any section: $key")
                    } else when (key) {
                        "publickey" -> peer.publicKey = value
                        "presharedkey" -> peer.presharedKey = Secret(value)
                        "allowedips" -> peer.allowedIps += splitList(value)
                        "endpoint" -> peer.endpoint = value
                        "persistentkeepalive" -> peer.persistentKeepalive = value.toIntOrNull()
                            ?: run { problems += Problem(Code.INVALID_KEEPALIVE, value); null }
                        else -> { unknown += "Peer.$originalKey"; problems += Problem(Code.UNKNOWN_FIELD, "Peer.$originalKey") }
                    }
                }
                else -> problems += Problem(Code.MALFORMED, "line ${index + 1}: key before any section")
            }
        }

        if (!sawInterface) return ParseResult(null, problems + Problem(Code.NO_INTERFACE))

        if (ifaceKey == null) problems += Problem(Code.MISSING_PRIVATE_KEY)
        else if (!isValidKey(ifaceKey!!.reveal())) problems += Problem(Code.INVALID_KEY, "PrivateKey")

        if (addresses.isEmpty()) problems += Problem(Code.MISSING_ADDRESS)
        addresses.filterNot { isCidr(it) }.forEach { problems += Problem(Code.INVALID_ADDRESS, it) }

        mtu?.let { if (it !in 576..1500) problems += Problem(Code.INVALID_MTU, it.toString()) }

        if (peers.isEmpty()) problems += Problem(Code.NO_PEER)
        peers.forEachIndexed { i, p ->
            if (p.publicKey.isBlank()) problems += Problem(Code.MISSING_PUBLIC_KEY, "peer ${i + 1}")
            else if (!isValidKey(p.publicKey)) problems += Problem(Code.INVALID_KEY, "peer ${i + 1} PublicKey")
            p.presharedKey?.let { if (!isValidKey(it.reveal())) problems += Problem(Code.INVALID_KEY, "peer ${i + 1} PresharedKey") }
            if (p.endpoint.isNullOrBlank()) problems += Problem(Code.MISSING_ENDPOINT, "peer ${i + 1}")
            else if (!isEndpoint(p.endpoint!!)) problems += Problem(Code.INVALID_ENDPOINT, p.endpoint!!)
            if (p.allowedIps.isEmpty()) problems += Problem(Code.INVALID_ALLOWED_IPS, "peer ${i + 1}: empty")
            p.allowedIps.filterNot { isCidr(it) }.forEach {
                problems += Problem(Code.INVALID_ALLOWED_IPS, "peer ${i + 1}: $it")
            }
        }

        val config = Config(
            interfaceSection = Interface(ifaceKey, addresses, dns, mtu, listenPort),
            peers = peers.map { it.toPeer() },
            unknown = unknown,
        )
        return ParseResult(config, problems)
    }

    private class MutablePeer {
        var publicKey: String = ""
        var presharedKey: Secret? = null
        val allowedIps = mutableListOf<String>()
        var endpoint: String? = null
        var persistentKeepalive: Int? = null
        fun toPeer() = Peer(publicKey, presharedKey, allowedIps.toList(), endpoint, persistentKeepalive)
    }

    private fun splitList(value: String): List<String> =
        value.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    /** WireGuard keys are 32 bytes of base64: 44 chars ending with '='. */
    fun isValidKey(key: String): Boolean {
        if (key.length != 44 || !key.endsWith('=')) return false
        val body = key.dropLast(1)
        if (!body.all { it.isLetterOrDigit() || it == '+' || it == '/' }) return false
        return runCatching { java.util.Base64.getDecoder().decode(key).size == 32 }.getOrDefault(false)
    }

    fun isCidr(value: String): Boolean {
        val slash = value.lastIndexOf('/')
        if (slash <= 0) return false
        val ip = value.substring(0, slash)
        val prefix = value.substring(slash + 1).toIntOrNull() ?: return false
        val isV4 = ip.matches(Regex("""\d{1,3}(\.\d{1,3}){3}""")) && ip.split('.').all { it.toInt() in 0..255 }
        val isV6 = ip.contains(':')
        return when {
            isV4 -> prefix in 0..32
            isV6 -> prefix in 0..128
            else -> false
        }
    }

    fun isEndpoint(value: String): Boolean {
        val colon = value.lastIndexOf(':')
        if (colon <= 0 || colon == value.length - 1) return false
        val port = value.substring(colon + 1).toIntOrNull() ?: return false
        val host = value.substring(0, colon).removePrefix("[").removeSuffix("]")
        return port in 1..65535 && host.isNotBlank()
    }
}
