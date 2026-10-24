package dev.tunnelkit.engine.xray

import dev.tunnelkit.core.config.VlessLink
import dev.tunnelkit.core.config.XrayConfigBuilder
import dev.tunnelkit.core.engine.ConfigFormat
import dev.tunnelkit.core.engine.EngineConfig
import dev.tunnelkit.core.engine.EngineDescriptor
import dev.tunnelkit.core.engine.EngineFailureException
import dev.tunnelkit.core.engine.IntegrationStatus
import dev.tunnelkit.core.engine.TunOwnership
import dev.tunnelkit.core.engine.ValidationResult
import dev.tunnelkit.core.engine.VpnEngine
import dev.tunnelkit.core.model.EngineId
import dev.tunnelkit.core.model.Failure

/**
 * Adapter over libXray. It converts an app-level [EngineConfig] into Xray's own JSON and calls the
 * engine; it does not implement VLESS or TLS itself.
 *
 * Two modes:
 *  - `APP_TUN_FD_PASSED` (Android): the service creates the TUN and hands the fd over via
 *    `EngineConfig.runtime["tun.fd"]`; the config then carries a `tun` inbound and `XRAY_TUN_FD`.
 *  - `LOCAL_PROXY` (desktop/test): no fd, so the config carries a loopback SOCKS inbound.
 */
class XrayAdapter(
    private val core: XrayCore,
    private val protect: (Long) -> Boolean = { false },
    private val dnsServer: String? = null,
    private val socksPort: Int = 10808,
    private val mtu: Int = 1400,
) : VpnEngine {

    override val descriptor = EngineDescriptor(
        id = EngineId("xray"),
        engineName = "libXray",
        engineVersion = "26.10.21",
        provenanceUrl = "https://github.com/XTLS/libXray",
        provenanceCommit = "dbe95129fa96689c4643ddca52c278448eec6df1",
        licenseId = "MIT",
        formats = setOf(ConfigFormat.VLESS_LINK, ConfigFormat.XRAY_JSON),
        tunOwnership = TunOwnership.APP_TUN_FD_PASSED,
        supportsUdp = true,
        requires = setOf("server address+port", "uuid", "reality public key (reality only)", "sni (reality only)"),
        status = IntegrationStatus.IMPLEMENTED_BUT_UNVERIFIED,
    )

    private var running = false
    private var lastFailure: Failure? = null

    override suspend fun validate(config: EngineConfig): ValidationResult = when (config.format) {
        ConfigFormat.VLESS_LINK -> VlessLink.parse(config.payload)
            .map { link ->
                val problems = link.validate()
                if (problems.isEmpty()) ValidationResult.Valid
                else ValidationResult.invalid(problems.joinToString("; "))
            }
            .getOrElse { ValidationResult.invalid(it.message ?: "unparseable link") }

        ConfigFormat.XRAY_JSON ->
            if (config.payload.contains("\"outbounds\"")) ValidationResult.Valid
            else ValidationResult.invalid("xray json without outbounds")

        else -> ValidationResult.invalid("xray adapter accepts VLESS links and Xray JSON only")
    }

    override suspend fun prepare() {
        core.registerProtector(protect)
        if (dnsServer != null) core.setDns(dnsServer)
    }

    override suspend fun start(config: EngineConfig) {
        if (running) return

        val fd = config.runtime[EngineConfig.RUNTIME_TUN_FD]?.toIntOrNull()
        val xrayJson = when (config.format) {
            ConfigFormat.VLESS_LINK -> {
                val link = VlessLink.parse(config.payload).getOrThrow()
                if (fd != null) {
                    XrayConfigBuilder.tunConfig(
                        outboundJson = link.toOutboundJson(),
                        fd = fd,
                        mtu = mtu,
                        dns = dnsServer?.substringBefore(':'),
                    )
                } else {
                    link.copy(socksPort = socksPort).toXrayJson()
                }
            }
            ConfigFormat.XRAY_JSON -> config.payload
            else -> throw EngineFailureException(Failure.UnsupportedFeature(config.format.name))
        }

        val response = runCatching { core.invoke(XrayProtocol.runRequest(xrayJson, core.apiVersion)) }
            .getOrElse { t -> throw EngineFailureException(Failure.Unknown(t.message ?: t.javaClass.simpleName)) }

        val envelope = XrayProtocol.parse(response)
        if (!envelope.success) {
            val failure = XrayProtocol.failureFor(envelope.error)
            lastFailure = failure
            throw EngineFailureException(failure)
        }
        running = true
    }

    override suspend fun stop() {
        if (running) {
            runCatching { core.invoke(XrayProtocol.stopRequest(core.apiVersion)) }
            running = false
        }
        runCatching { core.resetDns() }
    }

    fun lastError(): Failure? = lastFailure
}
