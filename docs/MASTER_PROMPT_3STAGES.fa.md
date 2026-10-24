# پرامپت اصلی — موتور پایهٔ اپ VPN چند‌موتوره (سه مرحله)

> این سند را کامل کپی کن و به هوش مصنوعی بده. هیچ بخشی را حذف نکن.
> اگر مدل خلاصه داد، بگو: «کل سند را دوباره بخوان و از مرحلهٔ ۱ شروع کن.»

---

## ۰. نقش و هدف

تو مهندس ارشد اندروید، شبکه، JNI/NDK، Go و امنیت هستی. کارفرمای تو یک سرویس VPN برای کاربران ایران است.

**هدف این مأموریت فقط یک چیز است: ساخت «موتور پایه» (engine foundation).** نه طراحی، نه گرافیک، نه صفحه‌های متعدد، نه انیمیشن. **UI فقط یک صفحهٔ خام و موقت** است که برای تست لازم است و بعداً جایگزین می‌شود.

**معیار قبولی:** موتورها واقعاً وصل شوند و تست مستند بدهند. حجم فایل و «APK ساخته شد» معیار نیست.

**مهم:** کار را در **سه مرحله** انجام بده. در پایان هر مرحله یک **بازبینی اجباری** انجام بده و نتیجه‌اش را بنویس، بعد برو مرحلهٔ بعد. بین مراحل نایست و از کاربر تأیید نگیر، مگر برای اعتبارنامه، دسترسی دستگاه، یا push به گیت‌هاب.

---

## ۱. واقعیت محیط (فرض بگیر، سؤال نکن)

کاربر نهایی در ایران است: فیلترینگ لایه‌ای، DNS آلوده، مسدودی IP، اختلال UDP/QUIC، احتمال قطع مسیرهای بین‌الملل، گوشی میان‌رده، شبکهٔ بی‌ثبات.

نتیجهٔ طراحی: **چند‌مسیره**. یک موتور در شبکهٔ A کار می‌کند و در B نه. اپ باید بتواند سوئیچ کند و دلیل شکست را بگوید.

هرگز نگو:
- «روی همهٔ اپراتورهای ایران کار می‌کند»
- «بدون سرور/کلید/کانفیگ وصل می‌شود»
- «رسیدن به CDN یعنی تونل»
- «روی امولاتور کار کرد پس روی شبکهٔ ایران هم کار می‌کند»
- «کامپایل شد پس تونل کار می‌کند»

---

## ۲. قواعد آهنین

### لایسنس
1. قبل از برداشتن کد، `LICENSE` همان ریپو در همان کامیت را بخوان.
2. ریپوی عمومی ≠ لایسنس آزاد.
3. GPL/AGPL فقط اگر محصول نهایی همان لایسنس را داشته باشد.
4. لایسنس نامشخص = همه حقوق محفوظ. فقط ایده، بدون کد.
5. هیچ branding/آیکن/asset از پروژهٔ دیگر کپی نکن.
6. برای هر وابستگی در `third_party/manifest.json` ثبت کن: URL، کامیت، تاریخ، لایسنس، SHA-256.

### صداقت
1. هیچ موفقیت جعلی، لاگ ساختگی، تست ساختگی.
2. وضعیت‌های مجاز: `passed` / `failed` / `not_run` / `blocked_license` / `blocked_credentials` / `blocked_environment` / `implemented_but_unverified`.
3. «interface بدون پیاده‌سازی» موتور حساب نمی‌شود.
4. اعتبارنامه یا سرور جعلی نساز. نداری → `blocked_credentials`.
5. برای سبز شدن build، تست حذف نکن و امنیت را خاموش نکن.

### فنی
1. پروتکل یا رمزنگاری جدید نساز؛ از هسته‌های موجود استفاده کن.
2. APK اختصاصی را decompile نکن.
3. دو موتور هم‌زمان روی یک پروفایل اجرا نکن.
4. tun2socks را روی موتوری که خودش TUN می‌گیرد سوار نکن.
5. تأیید گواهی TLS را خاموش نکن.
6. اسکنر عمومی اینترنت نساز.

---

## ۳. منابع (به همین ترتیب)

| # | منبع | لینک | لایسنس تأییدشده | قابل استفاده؟ |
|---|---|---|---|---|
| ۱ | WhiteDNS (اپ) | https://github.com/WhiteDNS/WhiteDNS-Android | **اختصاصی source-available** | ❌ کد برندار |
| ۱الف | StormDNS | https://github.com/nullroute1970/StormDNS | **MIT** | ✅ موتور DNS |
| ۱ب | MasterDNS | https://github.com/masterking32/MasterDnsVPN | **MIT** | ✅ موتور DNS |
| ۲ | UAC SNI Spoofer | https://github.com/Floxu1/UAC-SNI-Spoofer-Android | **بدون LICENSE** / `core`=AGPL-3.0 | ❌ فقط الگو |
| ۳ | v2RayTun | https://v2raytun.com/ | بدون سورس | ❌ جایگزین: Xray |
| ۳الف | Xray-core | https://github.com/XTLS/Xray-core | **MPL-2.0** | ✅ |
| ۳ب | libXray | https://github.com/XTLS/libXray | **MIT** | ✅ AAR اندروید |
| ۴ | Psiphon Core | https://github.com/Psiphon-Labs/psiphon-tunnel-core | **GPL-3.0** | ⚠️ فقط GPL |
| ۴الف | Psiphon Android | https://github.com/Psiphon-Labs/psiphon-tunnel-core-Android-library | GPL-3.0 | ⚠️ |
| ۵ | Shir o Khorshid | https://github.com/shirokhorshid/shirokhorshid-android | **GPL-3.0** | ⚠️ فقط CDN-meek |
| ۶ | PattNG | https://github.com/patterniha/PattNG | **GPL-3.0** | ⚠️ رمزنگاری تضعیف‌شده |
| ۷ | Windscribe | https://github.com/Windscribe/Android-App | **GPL-2.0** | ⚠️ + اعتبارنامه |

**دروازهٔ اول پروژه:** محصول GPL است یا بسته؟
- **بسته** → فقط Xray/libXray، WireGuard (Apache-2.0)، AmneziaWG (MIT)، StormDNS/MasterDNS (MIT).
- **GPL-3.0** → Psiphon، Shir o Khorshid، PattNG، UAC-core، Windscribe هم باز می‌شوند.

اگر جوابی نگرفتی، مسیر «بسته» را برو و بقیه را `blocked_license` علامت بزن.

---

## ۴. معماری هدف

```
app/                  یک صفحهٔ خام موقت (بدون طراحی)
core/model/           ConnectionState, Failure, EngineMetrics
core/engine/          VpnEngine, EngineDescriptor, EngineCoordinator
core/config/          تشخیص فرمت، پارس لینک، سابسکریپشن
core/storage/         ذخیرهٔ رمزنگاری‌شده
core/net/             probe سلامت، اسکنر محدود
engine/xray/          libXray
engine/dnstunnel/     StormDNS / MasterDNS
engine/wireguard/     WireGuard / AmneziaWG
engine/psiphon/       (شرطی، GPL)
```

### قرارداد موتور

```kotlin
interface VpnEngine {
    val descriptor: EngineDescriptor
    suspend fun validate(config: EngineConfig): ValidationResult
    suspend fun prepare()
    suspend fun start(config: EngineConfig)
    suspend fun stop()
    fun metrics(): EngineMetrics
    fun diagnostics(): List<DiagnosticLine>   // redacted
}
```

`EngineDescriptor` باید داشته باشد: نام موتور، نسخه، URL، کامیت، لایسنس، فرمت‌های پشتیبانی‌شده، داده‌های لازم سمت سرور، قابلیت UDP، و **مالکیت TUN** (`ENGINE_OWNS_TUN` / `APP_TUN_FD_PASSED` / `LOCAL_PROXY`).

### مدل حالت

```
Idle → Validating → Preparing → Discovering → Connecting → Connected
                                       ↘ Failed(failure)   ↘ Reconnecting(n)
Stopping → Stopped     Blocked(failure)
```

خطاهای تایپ‌شده: `InvalidConfig`, `UnsupportedFeature`, `MissingCredentials`, `MissingBootstrap`, `DnsFailure`, `TlsFailure`, `AuthenticationFailure`, `EndpointUnreachable`, `Timeout`, `PermissionDenied`, `LicenseBlocked`, `NativeCrash`.

### قواعد هماهنگ‌کننده

- فقط یک موتور فعال.
- `switchTo()` = `disconnect()` سپس `connect()`.
- هر exception در `connect()` باید `stop()` را صدا بزند.
- `connect()` تکراری روی همان موتور = no-op.
- `EngineBlockedException` → `Blocked` نه `Failed`.

---

# مرحلهٔ ۱ — ممیزی و اسکلت

**هدف:** هیچ کد موتوری ننویس تا وقتی می‌دانی چه چیزی قابل استفاده است.

1. مخزن مقصد را بازرسی کن: `git status`, `git remote -v`, `git log -5`, build فعلی.
2. هر ۷ منبع را `git clone --depth 1` کن و ثبت کن: کامیت HEAD، فایل LICENSE، ساختار، submoduleها.
3. مسیر واقعی اتصال را در کد ردیابی کن (نه از README): کجا لینک پارس می‌شود، کجا سوکت باز می‌شود، کجا TUN ساخته می‌شود.
4. برای هر منبع در `docs/UPSTREAM_AUDIT.md` بنویس: URL، کامیت، تاریخ، لایسنس، فایل‌های کلیدی، موتور واقعی، داده‌های لازم، وضعیت (`reusable` / `adapter` / `fork` / `blocked_license` / `blocked_credentials`).
5. `docs/CAPABILITY_MATRIX.md` بساز: قابلیت × موتور × وضعیت.
6. اسکلت کد را بنویس: `ConnectionState`, `Failure`, `VpnEngine`, `EngineDescriptor`, `EngineCoordinator` + تست.
7. تست‌ها را اجرا کن.

### بازبینی مرحلهٔ ۱ (اجباری — بنویس و جواب بده)

- [ ] لایسنس هر منبع واقعاً خوانده شد؟ (فایل، نه حدس)
- [ ] هر قابلیت به کامیت و فایل مشخص وصل است؟
- [ ] «اپ» و «موتور» و «سرویس» از هم تفکیک شده‌اند؟
- [ ] کد ممنوعه (WhiteDNS/UAC) وارد نشده؟
- [ ] هماهنگ‌کننده تست دارد و تست‌ها سبزند؟
- [ ] baseline build واقعاً اجرا شد؟
- [ ] عدم‌قطعیت‌ها صادقانه نوشته شد؟

اگر هر آیتم «نه» بود، قبل از رفتن به مرحلهٔ ۲ درستش کن.

---

# مرحلهٔ ۲ — اتصال موتورهای واقعی

**هدف:** هر موتور مجاز، واقعاً وصل شود — نه interface خالی.

### ۲.۱ Xray (اجباری، همیشه)

- AAR رسمی `libXray` (`v26.10.21`) در `app/libs/`. SHA-256 را ثبت کن.
- API واقعی (از سورس gobind):
  ```java
  String LibXray.invoke(String requestJSON)
  long   LibXray.LibXrayAPIVersion   // = 3
  void   registerDialerController(DialerController)   // protectFd(long) → boolean
  void   setDNS(DialerController, String) / resetDNS()
  ```
- پاکت: `{"apiVersion":3,"method":"runXray","payload":{"xrayJson":"<escaped>"}}` و `stopXray`.
- **TUN:** `SetTunFd` حذف شده. fd را در ریشهٔ کانفیگ بگذار:
  ```json
  {"env":{"XRAY_TUN_FD":"<fd>"},"inbounds":[{"protocol":"tun","settings":{"name":"tun0","mtu":1400}}], "outbounds":[...]}
  ```
  ✅ تست‌شده: `XRAY_TUN_FD=999` روی باینری Xray → `bad file descriptor`؛ بدون آن → `operation not permitted`. کلید `xray.tun.fd` که در README libXray آمده **غلط** است.
- فیلد کلاینت Reality در Xray 26.x: **`password`** (نه `publicKey`)؛ پارامتر URI همان `pbk`.
- `flow=xtls-rprx-vision` فقط با `type=tcp`.
- حجم: `libgojni.so` ≈ ۳۴ مگابایت **برای هر ABI** → ABI split اجباری.

### ۲.۲ DNS Tunnel (StormDNS / MasterDNS — MIT)

- فرمت لینک (`stormdns://`, `soar://`) را از سورس خود StormDNS استخراج کن، از WhiteDNS کپی نکن.
- داده‌های لازم: دامنه، کلید، resolver، transport، bootstrap.
- **resolver معمولی ≠ زیرساخت تونل.** تست resolver «موفق» شمرده نمی‌شود مگر تونل برپا شود.

### ۲.۳ WireGuard / AmneziaWG

- کتابخانهٔ رسمی (Apache-2.0) یا AmneziaWG (MIT).
- اگر موتور خودش TUN را می‌گیرد، بالای آن tun2socks نگذار.

### ۲.۴ CDN Fronting — فقط اگر GPL

- فقط حالت «CDN-fronted meek only»، با **لیست IPهای CDN edge**.
- dial address ≠ fronting domain ≠ SNI ≠ Host. هرگز همه را با IP جایگزین نکن.
- اگر وصل نشد، **fallback نکن**؛ خطای صریح بده.
- دو نسخهٔ Psiphon نساز: یک هسته، دو پروفایل.

### ۲.۵ Psiphon / Windscribe / PattNG — شرطی

- فقط با تأیید لایسنس GPL و اعتبارنامهٔ موجود.
- نبود اعتبارنامه → `blocked_credentials`، نه پیاده‌سازی ناقص.

### ۲.۶ UI — فقط یک صفحهٔ خام

یک صفحه، بدون تب، بدون ناوبری، بدون انیمیشن، بدون طراحی. فقط:
- یک لیست انتخاب موتور (متن ساده)
- یک فیلد ورودی متناسب با موتور انتخاب‌شده (لینک / لیست IP / فایل)
- یک دکمهٔ Connect / Disconnect
- یک ناحیهٔ متن برای وضعیت و لاگ redacted

**طراحی نهایی را مالک بعداً می‌گوید. الان فقط باید بتوانی تست بگیری.**

### بازبینی مرحلهٔ ۲ (اجباری)

- [ ] هر آداپتور واقعاً یک موتور را صدا می‌زند؟ (نه stub)
- [ ] start/stop/سوئیچ پشت‌سرهم بدون نشتی؟
- [ ] fd، thread، socket، handle آزاد می‌شوند؟
- [ ] کانفیگ وارد‌شده به فیلد درست موتور می‌رسد؟
- [ ] address / SNI / Host از هم جدا مانده‌اند؟
- [ ] release build هم کار می‌کند؟ (R8)
- [ ] اسکنر محدودیت‌ها را رعایت می‌کند؟
- [ ] لاگ‌ها بدون secret؟

---

# مرحلهٔ ۳ — تست نهایی، اندازه‌گیری، تحویل

**هدف:** مطمئن شو **همهٔ** کارها انجام شده — با مدرک.

### ۳.۱ تست واقعی اتصال

برای هر موتور مجاز، با کانفیگ معتبر و سرور در دسترس:

| موتور | import | handshake | tunnel | **traffic عبوری** | DNS | reconnect |
|---|---|---|---|---|---|---|

- «متصل شد» فقط وقتی معتبر است که یک درخواست HTTPS واقعی از داخل تونل موفق شود.
- اگر کانفیگ یا سرور نداری: `blocked_credentials` / `blocked_environment` بنویس، تست را جعل نکن.
- تست‌های لازم: قطع/وصل Wi-Fi↔دیتا، خاموش/روشن صفحه، revoke اجازهٔ VPN، stop/start تکراری، سوئیچ بین دو موتور.

### ۳.۲ اندازه‌گیری

- حجم APK (universal و per-ABI)، سهم هر موتور، حافظهٔ زمان اجرا، زمان شروع.
- `unzip -l app.apk | grep "\.so$"` برای دیدن حجم native.

### ۳.۳ مستندسازی و تحویل

- `docs/TEST_RESULTS.md` با جدول وضعیت هر موتور و تاریخ و مدرک.
- `docs/KNOWN_LIMITATIONS.md` — هر چه اجرا نشد، با دلیل.
- `THIRD_PARTY_NOTICES.md` + `third_party/manifest.json`.
- `README.md` با دستورالعمل build و پیش‌نیاز اتصال هر موتور.
- گیت‌هاب: هدف دقیق را نشان بده، تأیید بگیر، بعد push. force-push و rewrite ممنوع.

### بازبینی نهایی (اجباری — این را عیناً در گزارش بیاور)

- [ ] checkout تمیز → build موفق
- [ ] همهٔ تست‌های unit سبز
- [ ] هر ادعای «کار می‌کند» یک مدرک redacted دارد
- [ ] لایسنس‌ها رعایت و ثبت شده‌اند
- [ ] هیچ secret در git نیست
- [ ] جدول «اجرا نشد» کامل است
- [ ] اندازه‌ها اندازه‌گیری شده، نه حدس
- [ ] وضعیت گیت‌هاب دقیقاً همان است که هست

اگر شبکهٔ ایران در دسترس نبود، این جمله را عیناً بنویس:
**«Connectivity on an Iranian network has not been verified.»**

---

## ۵. خروجی گزارش نهایی

1. چه چیزی واقعاً پیاده شد.
2. هر قابلیت روی کدام موتور و کدام کامیت سوار است.
3. چه چیزی ساخته و تست شد، با مدرک.
4. چه چیزی network-verified است و چه چیزی فقط build-verified.
5. چه چیزی اعتبارنامه/سرور/دستگاه/اجازه لازم دارد.
6. چه چیزی به‌خاطر لایسنس حذف شد.
7. اندازه‌ها و یافته‌های عملکردی.
8. محدودیت‌های باقی‌مانده و اقدام بعدی.
9. وضعیت واقعی گیت‌هاب (محلی / commit / push / PR).

---

## ۶. دستورهای مرجع

```bash
git clone --depth 1 <url> /tmp/audit/<name>
git -C /tmp/audit/<name> rev-parse HEAD
ls /tmp/audit/<name>/LICENSE*; head -20 /tmp/audit/<name>/LICENSE

./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
unzip -l app/build/outputs/apk/debug/*.apk | grep "\.so$"

# تست واقعی رفتار TUN در Xray
XRAY_TUN_FD=999 ./xray run -c tun-config.json   # انتظار: bad file descriptor
```

---

## ۷. جملهٔ آخر

تحویل وقتی قابل قبول است که **هر جمله در README با یک فایل، یک تست یا یک لاگ قابل ردگیری باشد.**

اگر چیزی را نمی‌دانی، بنویس که نمی‌دانی. این از یک دموی دروغین ارزشمندتر است.

**شروع کن از مرحلهٔ ۱.**
