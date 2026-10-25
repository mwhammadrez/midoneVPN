plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("app.cash.paparazzi") version "1.3.5"
}

import java.util.Properties

// Release signing is configured from <root>/keystore.properties (never committed).
// When it is missing, a release build fails with a clear Gradle error instead of
// silently producing an unsigned artifact.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

val releaseTaskRequested = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
if (releaseTaskRequested && !keystorePropsFile.exists()) {
    throw GradleException(
        "Release signing requires keystore.properties at the project root " +
            "(storeFile, storePassword, keyAlias, keyPassword). " +
            "Create your own keystore — see docs/BUILD_ENVIRONMENT.md. " +
            "Nothing is published unsigned on purpose."
    )
}

android {
    namespace = "dev.tunnelkit.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.tunnelkit.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "0.9.2"
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = file(keystoreProps.getProperty("storeFile") ?: "")
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging { jniLibs { useLegacyPackaging = false } }

    splits {
        abi {
            isEnable = true
            isUniversalApk = false
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("app.cash.paparazzi:paparazzi:1.3.5")
}
