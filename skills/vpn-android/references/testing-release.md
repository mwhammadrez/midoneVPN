# Testing, CI and release

## What can be verified without a phone

| Check | How | Value |
|---|---|---|
| Project compiles, APK/AAB builds | `./gradlew assembleDebug bundleRelease` | catches 80% of integration errors |
| Packet logic (checksums, header parse, config parse) | JVM unit tests (`./gradlew testDebugUnitTest`) | the VPN-specific logic is testable off-device |
| Manifest correctness | `aapt dump badging/xmltree app.apk` | catches missing `BIND_VPN_SERVICE`, wrong FGS type |
| Link strings / config conversion | unit tests over `vless://` samples | the #1 source of "connects but no traffic" |
| Binary presence | `unzip -l app.apk \| grep lib/` | catches missing ABIs |

Sandbox reality: no emulator, no device. Verify build+tests here; behaviour on device needs the user.

## Device matrix (minimum before calling it done)

- **Android versions:** 8.0 (26), 10 (29), 12 (31), 14 (34), 16 (36)
- **OEMs:** Xiaomi (MIUI/HyperOS), Samsung, Huawei/Honor, Pixel — the first two break background services
- **Networks:** Wi-Fi, LTE, 5G, IPv6-only, captive portal, high loss (throttle to 5% loss, 300 ms RTT)
- **Transitions:** Wi-Fi→LTE mid-download, airplane mode on/off, VPN toggle in Settings, revoke permission
- **Battery:** 2 h idle with screen off — measure %/h (target < 2%/h idle)
- **Leaks:** ipleak.net + dnsleaktest.com + ipv6-test.com while connected
- **Per-app:** enable split tunnel with a banking app in the bypass list, confirm it still works

## Instrumented tests worth writing

```kotlin
@Test fun serviceEstablishesTunnel()      // start service, await state==CONNECTED, assert tun fd != null
@Test fun packetCountersIncrease()        // pump traffic (ping gateway), assert stats delta
@Test fun revokeStopsService()            // simulate revoke -> state==DISCONNECTED, no crash
@Test fun alwaysOnStartWithoutExtras()    // start with null intent, assert no crash
```

## Debugging tools

- `adb logcat -s VpnService:* <app>:*` — establish/protect/revoke events
- `adb shell dumpsys connectivity | grep -i vpn`
- `adb shell ip addr show tun0` + `adb shell ip route show table <vpn table>`
- **PCAPdroid** for packet capture (it uses its own VpnService, so run it *instead of* your app, or capture
  on the server side with `tcpdump -ni any port <vpnport>`)
- Server side: `journalctl -u xray -f`, `ss -tnp` to see which client IPs connect

## CI (GitHub Actions sketch)

```yaml
- uses: actions/setup-java@v4
  with: { distribution: temurin, java-version: '17' }
- run: ./gradlew --no-daemon testDebugUnitTest assembleRelease bundleRelease
- uses: actions/upload-artifact@v4
  with: { name: apk, path: app/build/outputs/apk/release/*.apk }
```
Cache `~/.gradle/caches` and `~/.gradle/wrapper`. Keep `local.properties` out of git; set
`ANDROID_SDK_ROOT` from the runner image (`ubuntu-latest` ships the SDK).

## Signing / release

- Create a keystore once, store it + passwords as CI secrets; **never** commit it.
- Enrol in Play App Signing (Google holds the app-signing key; you keep the upload key).
- `versionCode` monotonic integer; `versionName` semver.
- Build an **AAB** for Play (per-ABI splits generated automatically); ship a universal APK only for
  sideload/F-Droid.
- Size budget: a libXray-based client lands ~25–35 MB per ABI. If it's > 60 MB, check for duplicate
  ABIs and uncompressed native libs.

## Pre-release checklist

- [ ] `targetSdk ≥ 36`, `compileSdk` = latest installed
- [ ] `foregroundServiceType="specialUse"` + permission + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`
- [ ] VPN declaration submitted in Play Console; privacy policy URL live
- [ ] no secrets in logs; keys in Keystore/EncryptedSharedPreferences
- [ ] IPv6 + DNS leak tests pass
- [ ] survives 24 h with screen off on a Xiaomi
- [ ] connects from a cold boot with always-on enabled
- [ ] uninstall/reinstall does not leave a broken always-on entry
- [ ] app size and startup time measured on a low-end device (2 GB RAM)
