package dev.tunnelkit.core.config

import java.util.Base64

/**
 * Bounded subscription parsing. No network here — [SubscriptionParser] only decodes bytes that a
 * fetcher already bounded and that the user already consented to import.
 */
object SubscriptionParser {

    /** Hard cap on a decoded subscription body. 2 MB is ~20k links; anything larger is hostile. */
    const val MAX_BYTES = 2 * 1024 * 1024
    const val MAX_LINKS = 5_000

    data class Result(
        val links: List<String>,
        val skipped: Int,
        val truncated: Boolean,
        val encodedAs: Encoding,
    )

    enum class Encoding { PLAIN, BASE64, JSON }

    fun parse(raw: String): Result {
        require(raw.length <= MAX_BYTES) { "subscription exceeds $MAX_BYTES bytes" }

        val text = raw.trim()
        val encoding = when {
            text.startsWith("{") || text.startsWith("[") -> Encoding.JSON
            looksBase64(text) -> Encoding.BASE64
            else -> Encoding.PLAIN
        }

        val body = if (encoding == Encoding.BASE64) decodeBase64(text) else text
        val candidates = when (encoding) {
            Encoding.JSON -> extractUrls(body)
            else -> body.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }

        val valid = ArrayList<String>(minOf(candidates.size, MAX_LINKS))
        var skipped = 0
        var truncated = false
        for (line in candidates) {
            if (valid.size >= MAX_LINKS) { truncated = true; break }
            if (isSupportedLink(line)) valid += line else skipped++
        }
        return Result(valid, skipped, truncated, encoding)
    }

    fun decodeIfBase64(text: String): String =
        if (looksBase64(text)) decodeBase64(text) else text

    private fun isSupportedLink(line: String): Boolean {
        if (line.contains(' ') || line.length > 8192) return false
        val scheme = line.substringBefore("://", "").lowercase()
        return scheme in setOf(
            "vless", "vmess", "trojan", "ss", "ssr", "hysteria2", "hy2", "tuic", "stormdns", "soar",
        ) && line.contains('@')
    }

    private fun looksBase64(text: String): Boolean {
        val compact = text.filterNot { it.isWhitespace() }
        if (compact.length < 16) return false
        if (compact.contains("://")) return false
        return compact.all { it.isLetterOrDigit() || it in "+/=_-" }
    }

    private fun decodeBase64(text: String): String {
        val compact = text.filterNot { it.isWhitespace() }
        val normalized = compact.replace('-', '+').replace('_', '/')
        val padded = when (normalized.length % 4) {
            2 -> "$normalized=="
            3 -> "$normalized="
            else -> normalized
        }
        return runCatching { String(Base64.getDecoder().decode(padded), Charsets.UTF_8) }
            .getOrDefault(text)
    }

    private val URL_REGEX = Regex("""[a-zA-Z][a-zA-Z0-9+.\-]{2,15}://[^\s"',\\]+""")

    private fun extractUrls(json: String): List<String> =
        URL_REGEX.findAll(json).map { it.value }.toList()
}
