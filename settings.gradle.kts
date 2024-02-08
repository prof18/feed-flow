import java.net.URI

pluginManagement {
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
