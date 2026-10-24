package dev.tunnelkit.core.config

import dev.tunnelkit.core.engine.ConfigFormat

/**
 * Explicit format detection with an honest "I don't know".
 * The brief forbids guessing: an ambiguous payload must come back as Ambiguous so the UI can ask.
 */
object ConfigDetector {

    sealed interface Detection {
        data class Single(val format: ConfigFormat) : Detection
        data class Many(val format: ConfigFormat, val count: Int) : Detection
        data class Ambiguous(val candidates: List<ConfigFormat>) : Detection
        data class Unsupported(val reason: String) : Detection
    }

    /** Parses a WireGuard config if the text looks like one, otherwise null. */
    fun parseWireGuard(raw: String): WireGuardConfig.ParseResult? {
        val text = raw.trim()
        if (!text.contains("[Interface]", ignoreCase = true)) return null
        return WireGuardConfig.parse(text)
    }

    fun detect(raw: String): Detection {
        val text = raw.trim()
        if (text.isEmpty()) return Detection.Unsupported("empty input")
        if (text.length > SubscriptionParser.MAX_BYTES) {
            return Detection.Unsupported("payload exceeds ${SubscriptionParser.MAX_BYTES} bytes")
        }

        // 1. A single share link.
        val scheme = text.substringBefore("://", "").lowercase()
        if (scheme.isNotEmpty() && !text.contains(' ') && !text.contains('\n')) {
            return when (scheme) {
                "vless" -> Detection.Single(ConfigFormat.VLESS_LINK)
                "vmess", "trojan", "ss", "hysteria2", "hy2", "tuic" ->
                    Detection.Single(ConfigFormat.SHARE_LINK)
                "stormdns", "soar" -> Detection.Single(ConfigFormat.DNS_TUNNEL_LINK)
                "wireguard" -> Detection.Single(ConfigFormat.WIREGUARD_CONF)
                else -> Detection.Unsupported("unknown scheme: $scheme")
            }
        }

        // 2. Xray JSON config.
        if (text.startsWith("{") && text.contains("\"outbounds\"")) {
            return Detection.Single(ConfigFormat.XRAY_JSON)
        }

        // 3. WireGuard .conf / OpenVPN .ovpn.
        if (text.contains("[Interface]") && text.contains("PrivateKey")) {
            return Detection.Single(ConfigFormat.WIREGUARD_CONF)
        }
        if (text.contains("client") && text.contains("remote ") && text.contains("<ca>")) {
            return Detection.Single(ConfigFormat.OPENVPN_PROFILE)
        }

        // 4. Subscription: several links, possibly base64.
        val decoded = SubscriptionParser.decodeIfBase64(text)
        val lines = decoded.lineSequence().map { it.trim() }.filter { it.contains("://") }.count()
        if (lines > 1) return Detection.Many(ConfigFormat.SUBSCRIPTION_TEXT, lines)
        if (lines == 1) {
            return Detection.Single(ConfigFormat.VLESS_LINK)
        }

        // 5. Something JSON-ish but not an Xray config.
        if (text.startsWith("{")) return Detection.Ambiguous(listOf(ConfigFormat.XRAY_JSON))

        return Detection.Unsupported("no recognised format")
    }
}
