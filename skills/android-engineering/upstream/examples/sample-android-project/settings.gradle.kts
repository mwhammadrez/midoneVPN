pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "sample-android-project"

include(":app")
include(":feature:greeting:presentation")
include(":feature:greeting:domain")
include(":feature:greeting:data")
