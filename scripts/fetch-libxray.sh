#!/usr/bin/env bash
# Downloads the pinned libXray AAR and verifies its SHA-256.
set -euo pipefail
VERSION="v26.10.21"
SHA256="90d1f4889f019fa97a0788d74e6b725038d1cebb2e1b68220253474a18d8a597"
URL="https://github.com/XTLS/libXray/releases/download/${VERSION}/libxray-android.zip"
DEST="$(dirname "$0")/../app/libs"
mkdir -p "$DEST"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
echo "downloading libXray ${VERSION}…"
curl -fsSL -o "$TMP/libxray.zip" "$URL"
python3 - "$TMP/libxray.zip" "$TMP" <<'PY'
import sys, zipfile
zipfile.ZipFile(sys.argv[1]).extractall(sys.argv[2])
PY
cp "$TMP/libxray-android/libXray.aar" "$DEST/libXRay.aar"
ACTUAL="$(sha256sum "$DEST/libXRay.aar" | awk '{print $1}')"
if [ "$ACTUAL" != "$SHA256" ]; then
  echo "SHA-256 mismatch: $ACTUAL" >&2
  rm -f "$DEST/libXRay.aar"
  exit 1
fi
echo "ok: app/libs/libXRay.aar ($ACTUAL)"
