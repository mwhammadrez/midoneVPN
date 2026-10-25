# UX, error taxonomy, RTL/Persian, accessibility

A VPN app is judged on two screens: "did it connect" and "why didn't it". Everything here is about
making failure legible instead of a spinner that never ends.

## 1. States (the only five you need)

```
Idle → Preparing(consent) → Connecting → Connected → Degraded
                                   ↘ Error(code, cause, action)
```

- **Connected must be earned**, not declared by `establish()`. Probe: resolve one name through the
  tunnel + fetch a tiny known URL, 3 s timeout. Until the probe passes, show `Connecting`.
- **Degraded** = interface up, probe failing (the classic "icon on, nothing loads"). Show it, and
  auto-retry with backoff; never silently sit there.
- Every transition writes one line to a ring-buffer log the user can copy — that single feature kills
  half of one-star reviews.

## 2. Error taxonomy (code → cause → what the user can do)

| Code | Real cause | فارسی (نمونهٔ متن UI) | Action offered |
|---|---|---|---|
| `NO_PERMISSION` | `prepare()` denied / revoked | «اجازهٔ VPN داده نشد» | «تلاش دوباره» → consent dialog |
| `NO_NETWORK` | no default network | «اینترنت گوشی قطع است» | auto-retry on `onAvailable` |
| `SERVER_UNREACHABLE` | TCP/UDP timeout to the node | «به سرور نمی‌رسیم» | switch node / retry |
| `AUTH_FAILED` | wrong uuid / shortId / public key | «اطلاعات اتصال با سرور نمی‌خواند» | edit profile / re-import link |
| `TLS_HANDSHAKE` | SNI not in `serverNames`, clock skew | «دست‌دهی امن انجام نشد» | check device time / re-import |
| `DNS_FAIL` | no resolver, or resolver blocked | «ترجمهٔ نام دامنه شکست خورد» | switch DNS preset |
| `TUN_FAILED` | `establish()` returned null | «ساخت تونل ممکن نشد» | retry / report |
| `CORE_CRASH` | native crash / core exit | «هستهٔ اتصال متوقف شد» | restart core once, then error |
| `CONFIG_INVALID` | bad/partial profile | «فایل تنظیمات ناقص است» | re-import |
| `REVOKED` | user/system disabled VPN | «VPN توسط سیستم قطع شد» | manual reconnect only |

Rules: never show `Exception.toString()`; never say "unknown error" without the log-copy button;
Persian copy stays short (one line + one action).

## 3. Onboarding (3 screens, no more)

1. **What this does** — one sentence + the key icon. No marketing.
2. **Permission** — explain the system dialog *before* it appears (users refuse what surprises them).
3. **Import** — paste link / scan QR / pick a subscription. Skip if a profile already exists.

Second-run behaviour: open straight to the connect button. Never re-onboard.

## 4. RTL / Persian (this is the default locale here)

- `android:supportsRtl="true"` + `LayoutDirection.Rtl`; mirror arrows, back chevrons, progress direction.
- **Persian digits vs technical strings.** Show durations/percent in Persian digits if you localise,
  but **never** localise UUIDs, ports, IPs, byte counts in logs.
- **Bidi trap:** a `vless://…` link or a UUID rendered inside an RTL paragraph gets visually
  reordered (the `://` jumps, the fragment moves to the front). Wrap such text in
  `\u202A` (LTR embedding) … `\u202C`, or set `textDirection = Ltr` on that composable only.
- Font: use a Persian-capable family (Vazirmatn / Noto Naskh) with `lineHeight` ≈ 1.8× for body text;
  Persian needs more leading than Latin.
- Numbers in tables: right-align Persian, left-align Latin — mixing them in one column looks broken.
- Truncation: Persian strings are often ~20–30% longer than their English equivalent — test the
  connect button label with the longest word («قطع اتصال»), not «وصل».

## 5. Accessibility (cheap, and Play rewards it)

- Connect button: `contentDescription` describing state, not the glyph ("اتصال به سرور" / "قطع اتصال").
- Minimum 48 dp touch targets; the big circular button is fine, the small "copy log" is not.
- Contrast ≥ 4.5:1 for body text — the common dark theme (#0A0E14 background) with #7C8899 muted text
  is borderline; use #9AA7B8 for anything the user must read.
- Don't encode state in colour alone: Connected = green **and** the word.
- TalkBack order: status → primary action → details. `Modifier.semantics { }` on the status card.

## 6. Notification

- One channel, `IMPORTANCE_LOW`, ongoing, no sound.
- Title = state, text = node name + duration. A "Disconnect" action (VPN stays controllable from the
  shade — users expect it).
- On API 33+ ask `POST_NOTIFICATIONS` before the first connect; if denied, still run, but say so once.
