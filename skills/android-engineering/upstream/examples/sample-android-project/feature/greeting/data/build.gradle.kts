plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(project(":feature:greeting:domain"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
