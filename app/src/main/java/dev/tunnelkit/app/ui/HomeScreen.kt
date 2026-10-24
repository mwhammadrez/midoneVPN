package dev.tunnelkit.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.fontResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tunnelkit.app.R

object Tokens {
    val Bg = Color(0xFF050C12)
    val Upper = Color(0xFF100F16)
    val Accent = Color(0xFF4A1820)
    val Text = Color(0xFFF4F4F5)
    val Secondary = Color(0xFFB5B2B8)
    val Muted = Color(0xFF777780)
    val Divider = Color(0xFF19202A)
    val PowerFill = Color(0xFFFFFFFF)
    val PowerSymbol = Color(0xFF17151B)
    val Live = Color(0xFF20CA72)
}

val BrandFont = FontFamily(Font(R.font.oswald, weight = FontWeight.SemiBold))

enum class MethodStatus { AVAILABLE, NOT_INTEGRATED, BLOCKED_LICENSE, NEEDS_CREDENTIALS }

/**
 * The seven families requested by the owner. Status is the truth, not the wish:
 * only what is actually wired is AVAILABLE.
 */
data class Method(
    val id: String,
    val title: String,
    val subtitle: String,
    val status: MethodStatus,
    val note: String,
)

val Methods = listOf(
    Method(
        "xray", "VLESS / Reality", "Xray · TCP · WS · XHTTP",
        MethodStatus.AVAILABLE, "integrated — the only working engine today",
    ),
    Method(
        "wireguard", "WireGuard", "UDP · port from config",
        MethodStatus.NOT_INTEGRATED, "Apache-2.0 and integration is the next step",
    ),
    Method(
        "dns", "DNS Tunnel", "StormDNS / MasterDNS",
        MethodStatus.NOT_INTEGRATED, "MIT engines, adapter not written yet",
    ),
    Method(
        "cdn", "CDN Fronting", "meek only · edge IPs",
        MethodStatus.BLOCKED_LICENSE, "Shir o Khorshid is GPL-3.0",
    ),
    Method(
        "psiphon", "Psiphon", "discovery · server entries",
        MethodStatus.BLOCKED_LICENSE, "GPL-3.0 + needs credentials",
    ),
    Method(
        "sni", "SNI Spoofing", "UAC style",
        MethodStatus.BLOCKED_LICENSE, "UAC app has no licence; core is AGPL",
    ),
    Method(
        "ovpn", "OpenVPN / IKEv2 / Stealth / WStunnel", "Windscribe family",
        MethodStatus.BLOCKED_LICENSE, "GPL-2.0 + needs account",
    ),
)

fun methodById(id: String): Method = Methods.firstOrNull { it.id == id } ?: Methods.first()

data class ServerRow(val name: String, val detail: String, val best: Boolean = false)

val SampleServers = listOf(
    ServerRow("Best Server", "Auto · lowest latency", best = true),
    ServerRow("MrServer P3", "arvancloud.ir · ws"),
    ServerRow("MrServer P1", "reality · 443"),
    ServerRow("MrServer P2", "xhttp · 443"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    connected: Boolean = false,
    method: Method = Methods.first(),
    server: ServerRow? = null,
    servers: List<ServerRow> = SampleServers,
    statusText: String? = null,
    onToggle: () -> Unit = {},
    onMethodChange: (Method) -> Unit = {},
    onServerSelected: (ServerRow) -> Unit = {},
    onImport: (String) -> Boolean = { false },
    importError: String? = null,
    importWarning: String? = null,
    scanning: Boolean = false,
    scanStatus: String? = null,
    onScan: () -> Unit = {},
    onCancelScan: () -> Unit = {},
    settingsInfo: SettingsInfo = SettingsInfo(),
) {
    var settingsOpen by remember { mutableStateOf(false) }
    var importOpen by remember { mutableStateOf(false) }
    var methodOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = modifier.fillMaxSize().background(Tokens.Bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            BrandHeader(onMenu = { settingsOpen = true })

            Hero(
                connected = connected,
                method = method,
                server = server,
                statusText = statusText,
                onToggle = onToggle,
                onMethodClick = { methodOpen = true },
            )

            UtilityStrip()
            Divider(color = Tokens.Divider)
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("SERVERS", color = Tokens.Muted, fontSize = 10.sp, letterSpacing = 1.6.sp)
                Spacer(Modifier.weight(1f))
                if (scanning) {
                    Text(
                        "CANCEL",
                        color = Color(0xFFE8A33D),
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.clickable { onCancelScan() }.padding(6.dp),
                    )
                } else {
                    Text(
                        "TEST",
                        color = Tokens.Text,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.clickable(enabled = servers.isNotEmpty()) { onScan() }.padding(6.dp),
                    )
                }
                Text(
                    "+ ADD",
                    color = Tokens.Text,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.clickable { importOpen = true }.padding(6.dp),
                )
            }
            if (!scanStatus.isNullOrBlank()) {
                Text(
                    scanStatus,
                    color = if (scanning) Tokens.Secondary else Tokens.Muted,
                    fontSize = 10.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            LazyColumn(Modifier.fillMaxWidth()) {
                items(servers) { row ->
                    ServerLine(
                        row = row,
                        selected = row.name == server?.name,
                        onClick = { onServerSelected(row) },
                    )
                }
                if (servers.isEmpty()) {
                    item {
                        Text(
                            "هنوز سروری اضافه نشده — با «+ ADD» یک لینک vless وارد کن",
                            color = Tokens.Muted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                        )
                    }
                }
            }
        }
    }

    if (methodOpen) {
        ModalBottomSheet(
            onDismissRequest = { methodOpen = false },
            containerColor = Tokens.Upper,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            MethodSheet(current = method, onPick = { onMethodChange(it); methodOpen = false })
        }
    }

    if (importOpen) {
        ImportSheet(
            error = importError,
            warning = importWarning,
            onDismiss = { importOpen = false },
            onSubmit = { text ->
                val ok = onImport(text)
                if (ok) importOpen = false
                ok
            },
        )
    }

    if (settingsOpen) {
        ModalBottomSheet(
            onDismissRequest = { settingsOpen = false },
            sheetState = sheetState,
            containerColor = Tokens.Upper,
        ) {
            SettingsSheet(settingsInfo)
        }
    }
}

@Composable
private fun BrandHeader(onMenu: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "MidONe",
            fontFamily = BrandFont,
            fontSize = 22.sp,
            letterSpacing = 0.5.sp,
            color = Tokens.Text,
        )
        Spacer(Modifier.weight(1f))
        MenuGlyph(onClick = onMenu)
    }
}

@Composable
private fun Hero(
    connected: Boolean,
    method: Method,
    server: ServerRow?,
    statusText: String?,
    onToggle: () -> Unit,
    onMethodClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        if (connected) Color(0xFF0B3A24) else Tokens.Accent,
                        Tokens.Upper,
                    ),
                    center = Offset(140f, 210f),
                    radius = 520f,
                )
            )
    ) {
        Column(Modifier.padding(start = 16.dp, top = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (connected) "ON" else "OFF",
                        color = Tokens.Text,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                    )
                }
                Spacer(Modifier.size(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x22FFFFFF))
                        .clickable { onMethodClick() }
                        .padding(start = 10.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
                ) {
                    Box(
                        Modifier.size(5.dp).clip(CircleShape).background(
                            when (method.status) {
                                MethodStatus.AVAILABLE -> Tokens.Live
                                MethodStatus.NOT_INTEGRATED -> Tokens.Muted
                                MethodStatus.BLOCKED_LICENSE, MethodStatus.NEEDS_CREDENTIALS -> Color(0xFFE8A33D)
                            }
                        )
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(method.title, color = Tokens.Secondary, fontSize = 12.sp, maxLines = 1)
                    Text(" «", color = Tokens.Muted, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = server?.name ?: "No server",
                color = Tokens.Text,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(server?.detail ?: "add a vless:// link", color = Tokens.Muted, fontSize = 11.sp)
            if (!statusText.isNullOrBlank()) {
                Text(
                    text = statusText,
                    color = if (connected) Tokens.Live else Color(0xFFE8A33D),
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(6.dp).clip(CircleShape)
                        .background(if (connected) Tokens.Live else Tokens.Muted)
                )
                Spacer(Modifier.size(6.dp))
                Text("IR-MCI", color = Tokens.Secondary, fontSize = 11.sp)
                Text("  ›  ", color = Tokens.Muted, fontSize = 11.sp)
                Text("2001:db8::1", color = Tokens.Muted, fontSize = 11.sp)
            }
        }

        PowerButton(
            connected = connected,
            onClick = onToggle,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 20.dp),
        )
    }
}

@Composable
private fun PowerButton(connected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(if (connected) Tokens.Live else Tokens.PowerFill)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(30.dp)) {
            val stroke = size.minDimension * 0.11f
            drawArc(
                color = Tokens.PowerSymbol,
                startAngle = -60f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(stroke, stroke),
                size = Size(size.width - stroke * 2, size.height - stroke * 2),
                style = Stroke(width = stroke),
            )
            drawLine(
                color = Tokens.PowerSymbol,
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height * 0.42f),
                strokeWidth = stroke,
            )
        }
    }
}

@Composable
private fun UtilityStrip() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StripIcon(active = true) { c, s -> drawCircle(c, s.minDimension * 0.35f, Offset(s.width / 2, s.height / 2), style = Stroke(1.6f)); drawLine(c, Offset(s.width/2, s.height*0.22f), Offset(s.width/2, s.height*0.78f), 1.6f) }
        StripIcon(active = false) { c, s -> drawHeart(c, s) }
        StripIcon(active = false) { c, s -> drawPin(c, s) }
        StripIcon(active = false) { c, s -> drawTerminal(c, s) }
        Spacer(Modifier.weight(1f))
        StripIcon(active = false) { c, s -> drawCircle(c, s.minDimension * 0.3f, Offset(s.width * 0.42f, s.height * 0.42f), style = Stroke(1.6f)); drawLine(c, Offset(s.width*0.64f, s.height*0.64f), Offset(s.width*0.86f, s.height*0.86f), 1.6f) }
    }
}

@Composable
private fun StripIcon(active: Boolean, draw: androidx.compose.ui.graphics.drawscope.DrawScope.(Color, Size) -> Unit) {
    val color = if (active) Tokens.Text else Tokens.Muted
    Canvas(Modifier.size(19.dp)) { draw(color, size) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeart(c: Color, s: Size) {
    val p = Path().apply {
        moveTo(s.width * 0.5f, s.height * 0.85f)
        cubicTo(s.width * 0.05f, s.height * 0.55f, s.width * 0.15f, s.height * 0.15f, s.width * 0.5f, s.height * 0.32f)
        cubicTo(s.width * 0.85f, s.height * 0.15f, s.width * 0.95f, s.height * 0.55f, s.width * 0.5f, s.height * 0.85f)
    }
    drawPath(p, c, style = Stroke(1.6f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPin(c: Color, s: Size) {
    val p = Path().apply {
        moveTo(s.width * 0.5f, s.height * 0.92f)
        cubicTo(s.width * 0.15f, s.height * 0.55f, s.width * 0.2f, s.height * 0.1f, s.width * 0.5f, s.height * 0.1f)
        cubicTo(s.width * 0.8f, s.height * 0.1f, s.width * 0.85f, s.height * 0.55f, s.width * 0.5f, s.height * 0.92f)
    }
    drawPath(p, c, style = Stroke(1.6f))
    drawCircle(c, s.minDimension * 0.11f, Offset(s.width / 2, s.height * 0.38f), style = Stroke(1.4f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTerminal(c: Color, s: Size) {
    drawRoundRect(
        color = c,
        topLeft = Offset(0f, s.height * 0.15f),
        size = Size(s.width, s.height * 0.7f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f),
        style = Stroke(1.6f),
    )
    drawLine(c, Offset(s.width * 0.22f, s.height * 0.38f), Offset(s.width * 0.38f, s.height * 0.5f), 1.4f)
    drawLine(c, Offset(s.width * 0.38f, s.height * 0.5f), Offset(s.width * 0.22f, s.height * 0.62f), 1.4f)
    drawLine(c, Offset(s.width * 0.5f, s.height * 0.64f), Offset(s.width * 0.72f, s.height * 0.64f), 1.4f)
}

@Composable
private fun MethodSheet(current: Method, onPick: (Method) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Connection method", color = Tokens.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Only AVAILABLE methods can connect today.", color = Tokens.Muted, fontSize = 10.sp)
        Spacer(Modifier.height(10.dp))
        Methods.forEach { m ->
            val enabled = m.status == MethodStatus.AVAILABLE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onPick(m) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(7.dp).clip(CircleShape).background(
                        when (m.status) {
                            MethodStatus.AVAILABLE -> Tokens.Live
                            MethodStatus.NOT_INTEGRATED -> Tokens.Muted
                            else -> Color(0xFFE8A33D)
                        }
                    )
                )
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        m.title,
                        color = if (enabled) Tokens.Text else Tokens.Secondary,
                        fontSize = 13.sp,
                    )
                    Text("${m.subtitle} · ${m.note}", color = Tokens.Muted, fontSize = 9.sp, maxLines = 2)
                }
                if (m.id == current.id) Text("✓", color = Tokens.Live, fontSize = 12.sp)
                else if (!enabled) Text("🔒", color = Tokens.Muted, fontSize = 10.sp)
            }
            Divider(color = Tokens.Divider)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ServerLine(row: ServerRow, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape)
                .background(if (row.best) Color(0xFF16302A) else Tokens.Upper),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (row.best) "★" else row.name.take(2),
                color = if (row.best) Tokens.Live else Tokens.Secondary,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.name, color = if (selected) Tokens.Text else Tokens.Secondary, fontSize = 14.sp)
            Text(row.detail, color = Tokens.Muted, fontSize = 10.sp)
        }
        if (selected) Text("✓", color = Tokens.Live, fontSize = 13.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportSheet(
    error: String?,
    warning: String?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Boolean,
) {
    var text by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Tokens.Upper,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Add server", color = Tokens.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it; localError = null },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Tokens.Text),
                placeholder = { Text("vless://…", fontSize = 11.sp, color = Tokens.Muted) },
                singleLine = false,
                maxLines = 4,
            )
            val shown = localError ?: error
            if (!shown.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(shown, color = Color(0xFFF87171), fontSize = 10.sp)
            } else if (!warning.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(warning, color = Color(0xFFE8A33D), fontSize = 10.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "IMPORT",
                color = Tokens.Bg,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Tokens.Text)
                    .clickable {
                        if (text.isBlank()) localError = "empty link" else onSubmit(text.trim())
                    }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

data class SettingsInfo(
    val version: String = "0.1.0",
    val engine: String = "libXray 26.10.21",
    val licence: String = "MIT (libXray) · MPL-2.0 (Xray-core)",
    val lastFailure: String? = null,
    val probeUrl: String = "https://www.gstatic.com/generate_204",
)

@Composable
private fun SettingsSheet(info: SettingsInfo) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Settings", color = Tokens.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Only real controls are shown; unavailable features say so.",
            color = Tokens.Muted,
            fontSize = 10.sp,
        )
        Spacer(Modifier.height(12.dp))
        listOf(
            "Protocol" to "Chosen from the method pill on the main screen · only AVAILABLE methods connect",
            "Routing" to "Split tunneling — NOT IMPLEMENTED (no toggle is shown on purpose)",
            "DNS & Network" to "Probe URL: ${info.probeUrl} · private DNS detection — NOT IMPLEMENTED",
            "Diagnostics" to (info.lastFailure?.let { "Last failure: $it" } ?: "No failure recorded in this session"),
            "About" to "${info.version} · ${info.engine} · ${info.licence}",
        ).forEach { (title, sub) ->
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Text(title, color = Tokens.Text, fontSize = 13.sp)
                Text(sub, color = Tokens.Muted, fontSize = 10.sp, maxLines = 3)
            }
            Divider(color = Tokens.Divider)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MenuGlyph(onClick: () -> Unit) {
    Canvas(Modifier.size(22.dp).clickable { onClick() }) {
        val w = size.width
        val h = size.height
        for (i in 0..2) {
            val y = h * (0.28f + i * 0.22f)
            drawLine(
                color = Tokens.Text,
                start = Offset(w * 0.12f, y),
                end = Offset(w * 0.88f, y),
                strokeWidth = 2f,
            )
        }
    }
}

@Composable
private fun Divider(color: Color) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(color))
}
