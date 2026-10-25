package dev.tunnelkit.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.activity.enableEdgeToEdge
import dev.tunnelkit.app.ui.HomeScreen
import dev.tunnelkit.app.ui.Method
import dev.tunnelkit.app.ui.Methods
import dev.tunnelkit.app.ui.ServerRow
import dev.tunnelkit.app.ui.SettingsInfo
import dev.tunnelkit.core.config.ConfigDetector
import dev.tunnelkit.core.config.VlessLink
import dev.tunnelkit.core.config.WireGuardConfig
import dev.tunnelkit.core.model.ConnectionState
import dev.tunnelkit.app.ui.fa
import dev.tunnelkit.app.ui.actionHint
import dev.tunnelkit.core.diag.DiagLog
import dev.tunnelkit.core.profile.Profile
import dev.tunnelkit.core.profile.ProfileStore
import dev.tunnelkit.core.config.XrayConfigBuilder
import dev.tunnelkit.core.net.EndpointScanner
import dev.tunnelkit.core.net.ProbeRunner
import dev.tunnelkit.engine.xray.LibXrayCore
import dev.tunnelkit.engine.xray.XrayProbeRunner
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { MidOneApp() }
    }
}

private fun Profile.toRow(): ServerRow =
    ServerRow(name = name, detail = listOf(transport, security).filter { it.isNotBlank() }.joinToString(" · "))

@Composable
private fun MidOneApp() {
    val context = LocalContext.current
    val store = remember { ProfileStore(context) }
    var profiles by remember { mutableStateOf(store.list()) }
    var selected by remember { mutableStateOf(store.selected()) }
    var importError by remember { mutableStateOf<String?>(null) }
    var importWarning by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf<String?>(null) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val diag by DiagLog.lines.collectAsStateWithLifecycle()
    var state by remember { mutableStateOf<ConnectionState>(ConnectionState.Idle) }
    var method by remember { mutableStateOf(Methods.first()) }

    LaunchedEffect(Unit) { XrayTunnelService.state.collect { state = it } }

    LaunchedEffect(Unit) {
        DiagLog.header(
            "${BuildConfig.VERSION_NAME} · ${Build.MODEL} · Android ${Build.VERSION.RELEASE} · " +
                (Build.SUPPORTED_ABIS.firstOrNull() ?: "?")
        )
    }

    val consent = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> if (result.resultCode == android.app.Activity.RESULT_OK) startTunnel(context) }

    val notifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* denied is allowed: the tunnel still works, only the notification is hidden */ }

    val connected = state is ConnectionState.Connected
    val status = when (val s = state) {
        is ConnectionState.Connected -> "connected"
        is ConnectionState.Failed -> "⚠ " + s.failure.fa() +
            (s.failure.actionHint()?.let { " — $it" } ?: "")
        is ConnectionState.Blocked -> "⛔ ${s.failure.code}"
        is ConnectionState.Connecting, is ConnectionState.Preparing -> "connecting…"
        is ConnectionState.Stopping, is ConnectionState.Stopped -> "disconnected"
        else -> null
    }

    HomeScreen(
        connected = connected,
        method = method,
        server = selected?.toRow(),
        servers = profiles.map { it.toRow() },
        statusText = status,
        importError = importError,
        importWarning = importWarning,
        settingsInfo = SettingsInfo(
            version = BuildConfig.VERSION_NAME,
            lastFailure = (state as? ConnectionState.Failed)?.failure?.code,
            diagLines = diag,
            onCopyDiagnostics = {
                val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                cm.setPrimaryClip(android.content.ClipData.newPlainText("MidONe diagnostics", DiagLog.dump()))
            },
            onClearDiagnostics = { DiagLog.clear() },
        ),
        onToggle = {
            if (connected || state.isActive()) {
                context.startService(
                    Intent(context, XrayTunnelService::class.java).setAction(XrayTunnelService.ACTION_STOP)
                )
            } else if (selected == null) {
                importError = null
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                val intent = VpnService.prepare(context)
                if (intent != null) consent.launch(intent) else startTunnel(context)
            }
        },
        onMethodChange = { method = it },
        onServerSelected = { row ->
            profiles.firstOrNull { it.name == row.name }?.let {
                store.select(it.id)
                selected = it
            }
        },
        scanning = scanning,
        scanStatus = scanStatus,
        onScan = {
            if (!scanning) {
                scanning = true
                scanStatus = "آماده‌سازی تست…"
                scanJob = scope.launch {
                    val targets = profiles.mapNotNull { p ->
                        runCatching {
                            EndpointScanner.Target(
                                p.id,
                                XrayProbeRunner.Payload(
                                    XrayConfigBuilder.socksConfig(
                                        dev.tunnelkit.core.config.VlessLink.parse(p.link).getOrThrow().toOutboundJson()
                                    )
                                ),
                            )
                        }.getOrNull()
                    }
                    if (targets.isEmpty()) {
                        scanStatus = "هیچ پروفایل قابل تستی نیست"
                        scanning = false
                        return@launch
                    }
                    val scanner = EndpointScanner(XrayProbeRunner(LibXrayCore()), concurrency = 3)
                    val report = scanner.scan(
                        targets,
                        EndpointScanner.Budget(maxTargets = 16, timeoutPerTargetMs = 4000, totalBudgetMs = 60_000),
                    ) { done, total -> scanStatus = "تست $done از $total…" }
                    val ok = report.results.count { it.ok }
                    val best = report.results.firstOrNull { it.ok }
                    scanStatus = when {
                        report.timedOut -> "لغو شد · $ok موفق از ${report.testedCount}"
                        best != null -> "$ok از ${report.testedCount} تونل واقعی · بهترین ${best.delayMs}ms"
                        else -> "هیچ‌کدام تونل برقرار نکردند (${report.testedCount} تست)"
                    }
                    scanning = false
                }
            }
        },
        onCancelScan = {
            scanJob?.cancel()
            scanJob = null
            scanning = false
            scanStatus = "لغو شد"
        },
        onImport = import@{ raw ->
            val wg = ConfigDetector.parseWireGuard(raw)
            if (wg != null) {
                if (!wg.ok) {
                    importError = "کانفیگ WireGuard ناقص است: " +
                        wg.problems.filter { it.code != WireGuardConfig.Code.UNKNOWN_FIELD }
                            .joinToString(", ") { it.code.name.lowercase() }
                    return@import false
                } else {
                    val summary = wg.config!!.redactedSummary()
                    val profile = Profile(
                        id = UUID.randomUUID().toString(),
                        name = wg.config!!.peers.firstOrNull()?.endpoint?.substringBefore(':') ?: "WireGuard",
                        link = raw,
                        transport = "wireguard",
                        security = "udp",
                        kind = "wireguard",
                    )
                    store.add(profile)
                    profiles = store.list()
                    selected = profile
                    importError = null
                    importWarning = "ذخیره شد ($summary) — با Connect امتحان کن"
                    return@import true
                }
            }

            val parsed = VlessLink.parse(raw)
            if (parsed.isFailure) {
                importError = "لینک معتبر نیست: ${parsed.exceptionOrNull()?.message ?: ""}"
                return@import false
            } else {
                val link = parsed.getOrThrow()
                val problems = link.validate()
                val profile = Profile(
                    id = UUID.randomUUID().toString(),
                    name = link.label.ifBlank { link.host },
                    link = raw,
                    transport = link.network,
                    security = link.security,
                )
                store.add(profile)

                // A security=none profile cannot work with the upstream engine. Offer a TLS/443
                // twin as a separate, explicit entry instead of silently rewriting the user's link.
                if (link.warnings().contains("security_none_public")) {
                    val tlsLink = link.toShareLink(forceTls = true)
                    store.add(
                        Profile(
                            id = UUID.randomUUID().toString(),
                            name = profile.name + " · TLS",
                            link = tlsLink,
                            transport = link.network,
                            security = "tls",
                        )
                    )
                }

                profiles = store.list()
                selected = profile
                importError = if (problems.isEmpty()) null else problems.first()
                importWarning = when {
                    link.warnings().contains("security_none_public") ->
                        "ذخیره شد، ولی این سرور بدون TLS است؛ Xray آن را رد می‌کند — سرور را روی ۴۴۳ با TLS بگذار"
                    link.warnings().contains("cert_not_trusted") ->
                        "ذخیره شد، ولی گواهی سرور معتبر نیست و Xray 26 دیگر allowInsecure را نمی‌پذیرد"
                    else -> null
                }
                return@import problems.isEmpty()
            }
            false
        },
    )
}

private fun startTunnel(context: Context) {
    ContextCompat.startForegroundService(
        context,
        Intent(context, XrayTunnelService::class.java).setAction(XrayTunnelService.ACTION_START),
    )
}
