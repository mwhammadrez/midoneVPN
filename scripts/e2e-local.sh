#!/usr/bin/env bash
# End-to-end check of the generated client configs against the real Xray core, on one machine.
#
#   ./scripts/e2e-local.sh
#
# Proves: config parses, VLESS over WS carries traffic, VLESS over WS+TLS carries traffic, and a
# dead server makes the request fail (so success is not coming from a direct leak).
# Needs: curl, python3, openssl, and the xray binary (downloaded to /tmp/xray if absent).
set -euo pipefail

XRAY=${XRAY_BIN:-/tmp/xray/xray}
PORT_WS=18080; PORT_TLS=18443; SOCKS_WS=11808; SOCKS_TLS=11812; WEB=19099
TARGET_IP=${TARGET_IP:-45.33.32.156}     # a global unicast address, so freedom does not blackhole it
UUID=5783a3e7-e373-51cd-8642-c83782b807c5
TMP=$(mktemp -d)
PIDS=()
cleanup() { for p in "${PIDS[@]:-}"; do kill "$p" 2>/dev/null || true; done; rm -rf "$TMP"; }
trap cleanup EXIT

if [ ! -x "$XRAY" ]; then
  echo "xray not found at $XRAY — download Xray-linux-64.zip from github.com/XTLS/Xray-core/releases"
  exit 2
fi

say() { printf '\n=== %s ===\n' "$1"; }

say "fixtures from the app's own builders"
(cd "$(dirname "$0")/.." && ./gradlew :app:testDebugUnitTest \
   --tests 'dev.tunnelkit.core.config.EmitFixturesTest' --tests 'dev.tunnelkit.core.config.EmitTlsFixtureTest' -q)

say "local VLESS+WS server on 127.0.0.1:$PORT_WS"
cat > "$TMP/server.json" <<JSON
{"log":{"loglevel":"warning"},
 "inbounds":[{"listen":"127.0.0.1","port":$PORT_WS,"protocol":"vless",
   "settings":{"clients":[{"id":"$UUID"}],"decryption":"none"},
   "streamSettings":{"network":"ws","wsSettings":{"path":"/ws"}}}],
 "outbounds":[{"protocol":"freedom"}]}
JSON
"$XRAY" run -c "$TMP/server.json" > "$TMP/server.log" 2>&1 & PIDS+=($!)

say "local VLESS+WS+TLS server on 127.0.0.1:$PORT_TLS (self-signed, pinned by the client)"
openssl req -x509 -newkey rsa:2048 -nodes -keyout "$TMP/key.pem" -out "$TMP/cert.pem" -days 1 \
  -subj "/CN=localhost" -addext "subjectAltName=DNS:localhost,IP:127.0.0.1" 2>/dev/null
PIN=$(openssl x509 -in "$TMP/cert.pem" -outform der | sha256sum | cut -d' ' -f1)
cat > "$TMP/server-tls.json" <<JSON
{"log":{"loglevel":"warning"},
 "inbounds":[{"listen":"127.0.0.1","port":$PORT_TLS,"protocol":"vless",
   "settings":{"clients":[{"id":"$UUID"}],"decryption":"none"},
   "streamSettings":{"network":"ws","security":"tls",
     "tlsSettings":{"certificates":[{"certificateFile":"$TMP/cert.pem","keyFile":"$TMP/key.pem"}]},
     "wsSettings":{"path":"/ws"}}}],
 "outbounds":[{"protocol":"freedom"}]}
JSON
"$XRAY" run -c "$TMP/server-tls.json" > "$TMP/server-tls.log" 2>&1 & PIDS+=($!)

say "target web server on $TARGET_IP:$WEB"
mkdir -p "$TMP/www" && echo "MIDONE-TUNNEL-OK" > "$TMP/www/probe.txt"
(cd "$TMP/www" && python3 -m http.server "$WEB" --bind "$TARGET_IP" > "$TMP/web.log" 2>&1) & PIDS+=($!)

python3 - "$SOCKS_WS" "$SOCKS_TLS" "$PIN" "$PORT_WS" "$PORT_TLS" <<'PY'
import json, sys
ws, tls, pin, srv_ws, srv_tls = int(sys.argv[1]), int(sys.argv[2]), sys.argv[3], int(sys.argv[4]), int(sys.argv[5])
# the fixtures target 127.0.0.1:8080/8443; point them at this run's ports
c = json.load(open('/tmp/gen/client-raw.json'))
c['inbounds'][0]['port'] = ws
c['outbounds'][0]['settings']['port'] = srv_ws
json.dump(c, open('/tmp/e2e-ws.json', 'w'))
t = json.load(open('/tmp/gen/client-tls.json'))
t['inbounds'][0]['port'] = tls
t['outbounds'][0]['settings']['port'] = srv_tls
t['outbounds'][0]['streamSettings']['tlsSettings']['pinnedPeerCertSha256'] = pin
json.dump(t, open('/tmp/e2e-tls.json', 'w'))
PY
"$XRAY" run -c /tmp/e2e-ws.json  > "$TMP/cli-ws.log"  2>&1 & PIDS+=($!)
"$XRAY" run -c /tmp/e2e-tls.json > "$TMP/cli-tls.log" 2>&1 & PIDS+=($!)
sleep 4

fail=0
check() { # name socks-port
  local out; out=$(curl -s --max-time 15 --socks5-hostname "127.0.0.1:$2" "http://$TARGET_IP:$WEB/probe.txt" || true)
  if [ "$out" = "MIDONE-TUNNEL-OK" ]; then echo "PASS  $1"; else echo "FAIL  $1 (got: ${out:-no response})"; fail=1; fi
}

say "traffic through the tunnel"
check "vless+ws (plaintext, private server)" "$SOCKS_WS"
check "vless+ws+tls (pinned certificate)"     "$SOCKS_TLS"

say "control: kill every server, traffic must stop"
for p in "${PIDS[@]}"; do kill "$p" 2>/dev/null || true; done
sleep 2
leak=$(curl -s --max-time 8 --socks5-hostname "127.0.0.1:$SOCKS_WS" "http://$TARGET_IP:$WEB/probe.txt" || true)
if [ -z "$leak" ]; then echo "PASS  no direct leak (request failed as expected)"; else echo "FAIL  traffic survived a dead tunnel: $leak"; fail=1; fi

say "result"
[ "$fail" -eq 0 ] && echo "ALL PASS" || echo "FAILURES PRESENT"
exit "$fail"
