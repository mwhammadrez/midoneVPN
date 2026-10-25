package dev.tunnelkit.app.ui

import dev.tunnelkit.core.model.Failure

/**
 * One typed code -> one short Persian sentence + one action.
 * Raw codes stay available for diagnostics; users never see them alone.
 */
fun Failure.fa(): String = when (this) {
    is Failure.InvalidConfig -> "تنظیمات ساخته‌شده معتبر نبود — این ایراد اپ است، نه سرور تو"
    is Failure.InvalidConfig -> "گزارش را بفرست؛ اپ باید درست شود"
    is Failure.UnsupportedFeature ->
        if (detail.contains("plaintext", true)) "این سرور بدون TLS است؛ Xray آن را قبول نمی‌کند"
        else "این روش اتصال پشتیبانی نمی‌شود"
    is Failure.MissingCredentials -> "این پروفایل به نام کاربری یا کلید نیاز دارد"
    is Failure.MissingBootstrap -> "هیچ سروری وارد نشده — با + ADD یک لینک اضافه کن"
    is Failure.DnsFailure -> "تونل بالاست ولی DNS از داخلش رد نمی‌شود"
    is Failure.TlsFailure -> "دست‌دهی امن انجام نشد — ساعت گوشی و SNI را چک کن"
    is Failure.AuthenticationFailure -> "UUID یا کلید با سرور نمی‌خواند"
    is Failure.EndpointUnreachable -> "به سرور نمی‌رسیم — شبکه یا پورت را عوض کن"
    is Failure.Timeout -> "تونل بالا آمد ولی ترافیک عبور نکرد"
    is Failure.PermissionDenied -> "اجازهٔ VPN داده نشد"
    is Failure.LicenseBlocked -> "این موتور به‌خاطر لایسنس قابل استفاده نیست"
    is Failure.NativeCrash -> "هستهٔ اتصال از کار افتاد"
    is Failure.Unknown -> "خطای نامشخص — گزارش را کپی کن"
}

fun Failure.actionHint(): String? = when (this) {
    is Failure.InvalidConfig -> "گزارش را بفرست؛ اپ باید درست شود"
    is Failure.UnsupportedFeature ->
        if (detail.contains("plaintext", true)) "سرور را روی ۴۴۳ با TLS بگذار و لینک را عوض کن" else null
    is Failure.DnsFailure -> "روتر یا اپ دیگری DNS را نمی‌قاپد؟ بعد از تست خبر بده"
    is Failure.MissingBootstrap -> "با + ADD لینک را وارد کن"
    is Failure.PermissionDenied -> "دوباره Connect بزن و اجازه بده"
    is Failure.AuthenticationFailure -> "لینک را دوباره از پنل بگیر"
    else -> null
}
