// Root build script. Real convention-plugin setup (build-logic/) is
// deliberately omitted here to keep this example small - see
// skills/gradle-build/SKILL.md for when a real project should introduce
// one.
plugins {
    id("com.android.application") version "8.6.0" apply false
    id("com.android.library") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
