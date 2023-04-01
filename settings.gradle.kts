import java.net.URI

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
//        mavenLocal()
        google()
        mavenCentral()
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
include(":desktopApp")

includeBuild("../../Android/RSS-Parser") {
    dependencySubstitution {
        substitute(module("com.prof18.rssparser:rssparser")).using(project(":rssparser"))
    }
}


