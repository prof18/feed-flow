pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven { url = uri("./offline-repository") }
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("./offline-repository") }
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
            content {
                includeModule("com.github.Dansoftowner", "jSystemThemeDetector")
                includeModule("com.github.prof18", "lyricist")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "FeedFlow"
include(":androidApp")
include(":benchmarks")
include(":shared")
include(":sharedUI")
include(":desktopApp")
include(":i18n")
include(":core")
include("database")
include(":feedSync:database")
include("feedSync:dropbox")
include("feedSync:googledrive")
include("feedSync:icloud")
include("feedSync:ikloud-macos")
include("feedSync:greader")
include("feedSync:feedbin")
include("feedSync:networkcore")
include("feedSync:test-utils")

// Pass -Pfeedflow.kleadPath=/path/to/klead to use a local checkout instead of Maven Central.
providers.gradleProperty("feedflow.kleadPath").orNull?.let { kleadPath ->
    includeBuild(kleadPath) {
        dependencySubstitution {
            substitute(module("com.prof18:klead")).using(project(":"))
            substitute(module("com.prof18:klead-android")).using(project(":"))
        }
    }
}

// includeBuild("../RSS-Parser") {
//    dependencySubstitution {
//        substitute(module("com.prof18.rssparser:rssparser")).using(project(":rssparser"))
//    }
// }
