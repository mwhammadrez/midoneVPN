# libXray / gobind / gojni
-keep class libXray.** { *; }
-keep class go.** { *; }
-keepclasseswithmembernames class * { native <methods>; }
-dontwarn libXray.**
-dontwarn go.**

# JNI callbacks from native into Kotlin
-keep class dev.tunnelkit.engine.xray.LibXrayCore { *; }

# Keep engine descriptors/reflection-free models readable in stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Annotation-only references pulled in by Tink (via androidx.security-crypto).
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.crypto.tink.**
