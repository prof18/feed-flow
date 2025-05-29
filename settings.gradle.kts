pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenLocal()
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
            content {
                includeModule("com.github.Dansoftowner", "jSystemThemeDetector")
                // TODO: Delete when/if https://github.com/adrielcafe/lyricist/pull/45
                //  and https://github.com/adrielcafe/lyricist/pull/46 gets merged
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
include(":shared")
include(":sharedUI")
include(":desktopApp")
include(":i18n")
include(":core")
include("database")
include(":feedSync:database")
include("feedSync:dropbox")
include("feedSync:icloud")
include("feedSync:ikloud-macos")
include("feedSync:greader")
include("feedSync:networkcore")

// includeBuild("../RSS-Parser") {
//    dependencySubstitution {
//        substitute(module("com.prof18.rssparser:rssparser")).using(project(":rssparser"))
//    }
// }
