import java.net.URI

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven {
            url = URI.create("https://jitpack.io")
            content {
                includeModule("com.github.Dansoftowner", "jSystemThemeDetector")
            }
        }
    }
}

rootProject.name = "FeedFlow"
include(":androidApp")
include(":shared")
include(":sharedUI")
include(":desktopApp")
include(":i18n")
include(":core")

//includeBuild("../../Android/RSS-Parser") {
//    dependencySubstitution {
//        substitute(module("com.prof18.rssparser:rssparser")).using(project(":rssparser"))
//    }
//}
include("database")
