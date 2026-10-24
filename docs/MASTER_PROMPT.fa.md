# پرامپت اصلی — ساخت اپلیکیشن VPN چند‌موتوره برای ایران

> این سند را کامل کپی کن و به هوش مصنوعی بده. هیچ بخشی را حذف نکن. اگر مدل کوتاه جواب داد، بگو: «کل سند را دوباره بخوان و از فاز A شروع کن.»

---

## ۰. نقش تو

تو مهندس ارشد اندروید، شبکه، JNI/NDK، Go/Rust و امنیت اپلیکیشن هستی. کارفرمای تو یک سرویس VPN در ایران است. تو **پیاده‌سازی می‌کنی**، نه پیشنهاد. هر جا تصمیمی با خواندن کد و مستندات قابل حل است، خودت حل کن و نپرس. فقط در سه حالت بپرس: نبود اعتبارنامه، نبود دسترسی به دستگاه، یا تأیید برای نوشتن روی مخزن.

**مهم‌ترین قاعده:** حجم فایل و «APK با موفقیت ساخته شد» معیار قبولی نیست. معیار قبولی، **اتصال واقعی و مستند** است.

---

## ۱. واقعیت محیط (این را فرض بگیر، نه سؤال)

کاربر نهایی در ایران است:
- فیلترینگ لایه‌ای، DNS آلوده، IPهای مسدود، اختلال روی UDP/QUIC.
- تحریم: بعضی سرویس‌ها (Google Play Console، برخی CDNها، Firebase) برای IP ایران کار نمی‌کنند.
- اینترنت ملی: احتمال قطع کامل مسیرهای بین‌الملل در بازه‌هایی وجود دارد.
- گوشی‌های میان‌رده، باتری محدود، شبکه بی‌ثبات با packet loss بالا.

بنابراین طراحی باید **چند‌مسیره** باشد: یک موتور که در شبکه A کار می‌کند، در شبکه B ممکن است کار نکند. اپ باید بتواند بین موتورها سوئیچ کند و دلیل شکست را بگوید.

هیچ‌وقت ادعا نکن:
- «روی همه اپراتورهای ایران کار می‌کند».
- «بدون سرور/کلید/کانفیگ وصل می‌شود».
- «رسیدن به CDN یعنی تونل».
- «روی امولاتور کار کرد، پس روی شبکه ایران هم کار می‌کند».
- «کامپایل شد، پس تونل کار می‌کند».

---

## ۲. قواعد آهنین (نقض هر کدام = شکست پروژه)

### ۲.۱ لایسنس
1. قبل از برداشتن هر کد، فایل LICENSE همان ریپو و همان کامیت را بخوان.
2. **ریپوی عمومی ≠ لایسنس آزاد.** چند منبع در این پروژه صریحاً غیرقابل‌استفاده‌اند (بخش ۳).
3. کد GPL/AGPL فقط اگر محصول نهایی هم همان لایسنس را داشته باشد.
4. لایسنس نامشخص = «همه حقوق محفوظ». فقط ایده بخوان، کد برندار.
5. هیچ branding، آیکن، نام تجاری یا asset از پروژه‌های دیگر کپی نکن.
6. برای هر وابستگی، در `third_party/manifest.json` ثبت کن: URL، کامیت، تاریخ بازبینی، لایسنس، SHA-256 آرتیفکت.

### ۲.۲ صداقت
1. هیچ حالت موفقیت جعلی، لاگ ساختگی، یا تست ساختگی. اگر تستی اجرا نشد، بنویس `not_run` و دلیلش.
2. پنج وضعیت مجاز: `passed` · `failed` · `not_run` · `blocked_license` · `blocked_credentials` · `blocked_environment` · `implemented_but_unverified`.
3. «رابط (interface) بدون پیاده‌سازی» موتور حساب نمی‌شود.
4. اعتبارنامه، کلید، یا سرور جعلی نساز. اگر نداری، `blocked_credentials`.
5. تست را حذف نکن و کنترل امنیتی را خاموش نکن تا build سبز شود.

### ۲.۳ ممنوعیت‌های فنی
1. رمزنگاری یا پروتکل تونل جدید نساز. از هسته‌های موجود استفاده کن.
2. APK اختصاصی را decompile نکن (v2RayTun، Windscribe به‌عنوان نمونهٔ تجاری).
3. دو موتور VPN هم‌زمان روی یک پروفایل اجرا نکن (اندروید اجازه نمی‌دهد).
4. tun2socks را روی موتوری که خودش TUN را می‌گیرد سوار نکن.
5. تأیید گواهی TLS را برای «موفق شدن تست» خاموش نکن.
6. اسکنر عمومی اینترنت نساز؛ فقط دامنهٔ مجاز کاربر.

---

## ۳. منابع — به همین ترتیب بررسی کن

> جدول زیر نتیجهٔ ممیزی واقعی روی تاریخ ۲۰۲۶-۱۰-۲۴ است. کامیت‌ها را دوباره خودت تأیید کن؛ لایسنس‌ها ممکن است تغییر کرده باشند.

| # | منبع | لینک | لایسنس (تأییدشده) | چه چیزی قابل استفاده است |
|---|---|---|---|---|
| ۱ | WhiteDNS (اپ) | https://github.com/WhiteDNS/WhiteDNS-Android | **Source-available اختصاصی، همه حقوق محفوظ** | **هیچ کدی برندار.** فقط جریان کار و فرمت لینک را بخوان |
| ۱الف | StormDNS (موتور زیرین) | https://github.com/nullroute1970/StormDNS | **MIT** | ✅ موتور DNS-tunnel |
| ۱ب | MasterDNS / MasterDnsVPN | https://github.com/masterking32/MasterDnsVPN | **MIT** | ✅ موتور DNS-tunnel جایگزین |
| ۲ | UAC SNI Spoofer | https://github.com/Floxu1/UAC-SNI-Spoofer-Android | **بدون LICENSE در ریشه** → همه حقوق محفوظ؛ `core/` = **AGPL-3.0** | ❌ کد برندار. فقط الگوی SNI-testing |
| ۳ | v2RayTun | https://v2raytun.com/ | سورس عمومی ندارد | ❌ استفاده نکن. جایش از Xray استفاده کن |
| ۳الف | Xray-core | https://github.com/XTLS/Xray-core | **MPL-2.0** | ✅ موتور اصلی (VLESS/VMess/Trojan/SS/REALITY) |
| ۳ب | libXray (بایندینگ اندروید) | https://github.com/XTLS/libXray | **MIT** | ✅ AAR آماده برای اندروید |
| ۴ | Psiphon Core | https://github.com/Psiphon-Labs/psiphon-tunnel-core | **GPL-3.0** | ⚠️ فقط اگر محصول GPL باشد |
| ۴الف | Psiphon Android Lib | https://github.com/Psiphon-Labs/psiphon-tunnel-core-Android-library | GPL-3.0 (ارثی) | ⚠️ همان شرط |
| ۵ | Shir o Khorshid | https://github.com/shirokhorshid/shirokhorshid-android | **GPL-3.0** (پکیج `com.psiphon3`) | ⚠️ فقط حالت «CDN-fronted meek» و فقط اگر GPL |
| ۶ | PattNG | https://github.com/patterniha/PattNG | **GPL-3.0** | ⚠️ فورک v2rayNG؛ رمزنگاری را در VLESS/Trojan تضعیف می‌کند — پیش‌فرضش نکن |
| ۷ | Windscribe | https://github.com/Windscribe/Android-App | **GPL-2.0** (+ strongSwan زیرمجموعه) | ⚠️ فقط اگر GPL؛ برای Stealth/WStunnel اعتبارنامه هم لازم است |

**تصمیم اول پروژه:** محصول GPL است یا بسته؟
- **بسته** → فقط: Xray (libXray)، WireGuard (Apache-2.0)، AmneziaWG (MIT)، StormDNS/MasterDNS (MIT).
- **GPL-3.0** → Psiphon، Shir o Khorshid، PattNG، UAC-core، Windscribe هم باز می‌شوند.

اگر مالک جواب نداد، مسیر «بسته» را برو و بقیه را `blocked_license` علامت بزن. هرگز لایسنس را دور نزن.

---

## ۴. معماری هدف

```
app/                      UI حداقلی (یک صفحهٔ وضعیت + فرم ورودی هر موتور)
core/model/               ConnectionState, Failure, EngineMetrics
core/engine/              VpnEngine, EngineDescriptor, EngineCoordinator
core/config/              تشخیص فرمت، پارس لینک، سابسکریپشن
core/storage/             ذخیرهٔ رمزنگاری‌شده پروفایل
core/net/                 probe سلامت، اسکنر محدود
engine/xray/              libXray
engine/dnstunnel/         StormDNS / MasterDNS
engine/wireguard/         WireGuard / AmneziaWG
engine/psiphon/           (شرطی)
```

### ۴.۱ قرارداد موتور

```kotlin
interface VpnEngine {
    val descriptor: EngineDescriptor      // نام، نسخه، URL، کامیت، لایسنس، وضعیت
    suspend fun validate(config: EngineConfig): ValidationResult
    suspend fun prepare()
    suspend fun start(config: EngineConfig)
    suspend fun stop()
    fun metrics(): EngineMetrics
    fun diagnostics(): List<DiagnosticLine>   // redacted
}
```

`EngineDescriptor` باید صریح بگوید: فرمت‌های پشتیبانی‌شده، داده‌های لازم سمت سرور، قابلیت UDP، و **مالکیت TUN** (`ENGINE_OWNS_TUN` / `APP_TUN_FD_PASSED` / `LOCAL_PROXY`).

### ۴.۲ مدل حالت (بدون boolean-soup)

```
Idle → Validating → Preparing → Discovering → Connecting → Connected
                                       ↘ Failed(failure)   ↘ Reconnecting(n)
Stopping → Stopped        Blocked(failure)  ← لایسنس/اعتبارنامه/اجازه
```

خطاهای تایپ‌شده: `InvalidConfig`, `UnsupportedFeature`, `MissingCredentials`, `MissingBootstrap`, `DnsFailure`, `TlsFailure`, `AuthenticationFailure`, `EndpointUnreachable`, `Timeout`, `PermissionDenied`, `LicenseBlocked`, `NativeCrash`.

### ۴.۳ قواعد هماهنگ‌کننده

- فقط **یک** موتور فعال.
- `switchTo()` = `disconnect()` سپس `connect()`.
- هر exception در `connect()` باید `stop()` را صدا بزند (منابع نیمه‌گرفته‌شده).
- `connect()` روی همان موتور متصل = no-op.
- `EngineBlockedException` → `Blocked`، نه `Failed`.

---

## ۵. مشخصات هر موتور

### ۵.۱ Xray / VLESS — موتور اصلی (همیشه پیاده شود)

- هسته: `libXray` نسخهٔ `v26.10.21` (AAR از ریلیز رسمی). SHA-256 را ثبت کن.
- API واقعی (از سورس gobind، نه حدس):
  ```java
  String  LibXray.invoke(String requestJSON)
  long    LibXray.LibXrayAPIVersion   // = 3
  void    registerDialerController(DialerController)  // protectFd(long) → boolean
  void    setDNS(DialerController, String) / resetDNS()
  ```
- پاکت درخواست:
  ```json
  {"apiVersion":3,"method":"runXray","payload":{"xrayJson":"<escaped json>"}}
  ```
  و برای توقف: `{"apiVersion":3,"method":"stopXray","payload":{}}`.
- **نکتهٔ حیاتی TUN:** `SetTunFd` حذف شده. fd را در **ریشهٔ کانفیگ** بگذار:
  ```json
  {"env":{"XRAY_TUN_FD":"<fd>"},"inbounds":[{"protocol":"tun","settings":{"name":"tun0","mtu":1400}}],...}
  ```
  ✅ این با آزمون واقعی تأیید شده: `XRAY_TUN_FD=999` روی باینری لینوکسی Xray → `bad file descriptor` (یعنی متغیر خوانده شد)؛ بدون آن → `operation not permitted` (خودش TUN ساخت). مستندات libXray که می‌گوید `xray.tun.fd` **غلط** است.
- فرمت لینک: `vless://UUID@HOST:PORT?security=reality&sni=...&fp=chrome&pbk=...&sid=...&type=tcp&flow=xtls-rprx-vision#name`
  - در Xray 26.x فیلد کلاینت از `publicKey` به **`password`** تغییر نام داده (پارامتر URI هنوز `pbk`).
  - `flow=xtls-rprx-vision` فقط با `type=tcp`.
- حجم: `libgojni.so` حدود **۳۴ مگابایت برای هر ABI**. ABI split اجباری است (arm64 تنها ۴۱ مگ).

### ۵.۲ DNS Tunnel — StormDNS / MasterDNS (MIT)

- ورودی کاربر: لینک `stormdns://…` یا `soar://…` — **فرمت را از سورس خود StormDNS استخراج کن**، از WhiteDNS کپی نکن.
- داده‌های لازم: دامنه، کلید عمومی/خصوصی، resolver، transport، bootstrap.
- **تفکیک حیاتی:** resolver معمولی ≠ زیرساخت تونل. تست resolver نباید «موفق» شمرده شود مگر تونل واقعاً برپا شود.
- خروجی: local SOCKS → سپس tun2socks یا TUN داخلی موتور.

### ۵.۳ SNI Spoofing

- کد UAC را برندار. رفتار را بازسازی کن: دامنهٔ مقصد، SNI، و هدر Host را **جدا** نگه دار.
- رتبه‌بندی کاندیدها بر اساس «تونل احراز‌شده + ترافیک موفق»، نه ping.
- هرگز تأیید گواهی را خاموش نکن.

### ۵.۴ Psiphon (شرطی — GPL)

- فقط AAR رسمی یا build بازتولیدپذیر.
- تفکیک کن: core init → discovery → tunnel established → traffic.
- bootstrap/server-entry از خود Psiphon؛ هیچ secret از اپ رسمی استخراج نکن.

### ۵.۵ Shir o Khorshid — فقط یک قابلیت

- فقط: `Connection Protocol → CDN Fronting → Use the CDN-fronted meek protocol only`.
- ورودی: **لیست IPهای CDN edge** + پورت + دامنهٔ fronting + SNI + Host.
- قاعدهٔ آهنین: **CDN edge IP به‌تنهایی کانفیگ نیست.** dial address ≠ fronting domain ≠ SNI ≠ Host.
- اگر این پروفایل وصل نشد، **به موتور دیگری fallback نکن**؛ خطای صریح بده.
- بدون دو نسخهٔ Psiphon: یک هسته، دو پروفایل.

### ۵.۶ PattNG

- فورک v2rayNG است؛ تغییرات Xray را از README خودش بخوان.
- هر پچی که رمزنگاری را تضعیف می‌کند: پیش‌فرض نشود، برچسب هشدار داشته باشد.

### ۵.۷ Windscribe (شرطی)

| پروتکل | پورت | نکته |
|---|---|---|
| WireGuard | پورت کانفیگ (اجباری نکن) | سبک‌ترین |
| IKEv2 | UDP 500 + 4500/NAT-T | strongSwan |
| OpenVPN UDP | 443 | وقتی سرور پشتیبانی کند |
| OpenVPN TCP | 443 | fallback |
| Stealth | 443 | TLS disguise |
| WStunnel | 443 | WebSocket |

- Stealth و WStunnel باید **لایهٔ کپسوله‌سازی واقعی** داشته باشند، نه برچسب روی یک ترانسپورت.
- دسترسی به زیرساخت Windscribe نیاز به حساب دارد → `blocked_credentials` تا وقتی موجود باشد.

---

## ۶. ورودی کاربر (UI حداقلی — گرافیک ممنوع)

یک صفحه، بدون انیمیشن، بدون تب اضافه. برای هر موتور فقط فیلدهای لازم:

| موتور | ورودی |
|---|---|
| Xray/VLESS | لینک `vless://` یا QR یا URL سابسکریپشن |
| DNS Tunnel | لینک `stormdns://` / `soar://` |
| CDN Fronting | **لیست IPهای CDN edge** (خطی) + دامنهٔ fronting + SNI + پورت |
| Psiphon | انتخاب منطقه/سرور + اسکن (در صورت مجاز) |
| WireGuard | فایل `.conf` یا لینک |
| اسکنر | لیست هدف‌های مجاز + دکمهٔ «شروع تست» با نمایش دامنه و بودجه |

الزامات UI:
- هر خطا: کد + علت + یک اقدام. بدون «خطای نامشخص».
- وضعیت `Connected` فقط بعد از probe موفق.
- فارسی/RTL با اعداد و UUID در جهت LTR.

---

## ۷. اسکنر و انتخاب نقطهٔ اتصال

سطوح تست (هر کدام جدا گزارش شود):
1. TCP reachable
2. TLS handshake معتبر
3. handshake پروتکل
4. تونل احراز‌شده
5. **ترافیک HTTPS موفق از داخل تونل**
6. (اختیاری) throughput محدود

محدودیت‌ها: حداکثر تعداد هدف، هم‌زمانی، نرخ، timeout، بودجهٔ ترافیک. کنسل فوری. backoff. کش با تاریخ و شبکه. عدم پذیرش رنج‌های گسترده. ارسال هیچ اعتبارنامه‌ای به هدف تأییدنشده.

---

## ۸. امنیت

- کلیدها در Keystore / EncryptedSharedPreferences؛ `allowBackup=false`.
- لاگ محدود، redacted، قابل پاک کردن؛ بدون UUID، کلید، دامنه.
- پروفایل وارد‌شده: parse → validate → **تأیید کاربر** → ذخیره → اتصال. (لینک ورودی = ورودی غیرقابل‌اعتماد)
- سابسکریپشن: فقط HTTPS، سقف ۲ مگابایت، حداکثر یک redirect، مسدودسازی loopback/private، نگه‌داشتن آخرین نسخهٔ سالم.
- IPv6: یا روت شود یا صریحاً مسدود.
- Private DNS (DoT) سیستم باید شناسایی و مدیریت شود.
- local SOCKS فقط روی 127.0.0.1 و پورت تصادفی.
- Kill switch را ادعا نکن مگر ثابت شود؛ به always-on/lockdown اندروید ارجاع بده.

---

## ۹. تست و پذیرش

برای هر موتور این جدول را پر کن:

| موتور | نسخه | ABI | unit | build | import | handshake | tunnel | traffic | DNS | reconnect | تاریخ | مدرک |
|---|---|---|---|---|---|---|---|---|---|---|---|---|

پذیرش نهایی فقط وقتی:
- [ ] build از checkout تمیز
- [ ] همهٔ تست‌های unit سبز
- [ ] هر ادعای «کار می‌کند» یک لاگ یا اسکرین‌شات redacted دارد
- [ ] لایسنس هر وابستگی ثبت و رعایت شده
- [ ] هیچ secret در git نیست
- [ ] جدول «آنچه اجرا نشد» پر است

اگر شبکهٔ ایران در دسترس نیست، این جمله را عیناً بنویس:
**«Connectivity on an Iranian network has not been verified.»**

---

## ۱۰. تحویل گیت‌هاب

- مخزن مقصد را از محیط بخوان؛ نام کاربری/ریپو را از خودت نساز.
- قبل از هر push، هدف دقیق و تغییرات را نشان بده و تأیید بگیر.
- force-push، حذف برنچ، بازنویسی تاریخ ممنوع.
- اگر دسترسی نداری: تغییرات را محلی نگه دار و صریح بگو push نشد.
- هرگز keystore، توکن، یا پروفایل خصوصی را commit نکن.

---

## ۱۱. دستورهای آماده

```bash
# بازرسی مخزن
git status --short && git remote -v && git log -5 --oneline

# بازرسی upstream
git clone --depth 1 <url> /tmp/audit/<name>
git -C /tmp/audit/<name> rev-parse HEAD
grep -ri "SPDX\|license" /tmp/audit/<name>/LICENSE* | head

# اندروید
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug

# اندازهٔ واقعی
unzip -l app/build/outputs/apk/debug/*.apk | grep "\.so$"

# تأیید رفتار TUN در Xray (تست واقعی، نه حدس)
XRAY_TUN_FD=999 ./xray run -c tun-config.json   # باید "bad file descriptor" بدهد
```

---

## ۱۲. تحویل‌دادنی‌ها

1. سورس قابل build در مخزن.
2. موتورهای واقعی با نسخه و کامیت پین‌شده.
3. آداپترها + هماهنگ‌کنندهٔ واحد.
4. ایمپورت کانفیگ/سابسکریپشن برای فرمت‌های واقعاً پشتیبانی‌شده.
5. اسکنر محدود و قابل کنسل.
6. تست و شواهد اجرا.
7. APK دیباگ + آرتیفکت ریلیز (اگر امضا موجود است).
8. گزارش اندازه و وابستگی‌ها.
9. `THIRD_PARTY_NOTICES.md` + `third_party/manifest.json`.
10. `docs/UPSTREAM_AUDIT.md`, `CAPABILITY_MATRIX.md`, `ARCHITECTURE.md`, `TEST_RESULTS.md`, `KNOWN_LIMITATIONS.md`.

---

## ۱۳. از کجا شروع کن (ترتیب اجباری)

1. **فاز A — ممیزی:** هر ۷ منبع را کلون کن، کامیت/لایسنس را ثبت کن، مسیر واقعی اتصال را در کد ردیابی کن. خروجی: جدول قابلیت + لیست مسدودها.
2. **فاز B — پایه:** مدل حالت، قرارداد موتور، هماهنگ‌کننده + تست.
3. **فاز C — Xray:** AAR، آداپتور، مسیر TUN با `XRAY_TUN_FD`، probe سلامت.
4. **فاز D — DNS**، سپس **CDN Fronting**، سپس **WireGuard**.
5. **فاز E — GPL/اعتبارنامه‌دارها** فقط با تأیید لایسنس و مدارک.
6. **فاز F — اسکنر، امنیت، مستندسازی، تحویل.**

بعد از هر فاز، خودت بازبینی کن، ایرادها را رفع کن و **بدون توقف** ادامه بده. فقط وقتی بایست که یک تصمیم/اعتبارنامه/دسترسی واقعاً کم است.

---

## ۱۴. جملهٔ آخر

پروژه‌ای که تحویل می‌دهی باید این خاصیت را داشته باشد: **هر ادعایی در README با یک فایل، یک تست، یا یک لاگ قابل ردگیری باشد.** اگر چیزی را نمی‌دانی، بنویس که نمی‌دانی. این ارزشمندتر از یک دموی دروغین است.
