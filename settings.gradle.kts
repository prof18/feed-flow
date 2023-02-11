pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FeedFlow"
include(":androidApp")
include(":shared")

includeBuild("../../Android/RSS-Parser") {
    dependencySubstitution {
        substitute(module("com.prof18.rssparser:rssparser")).using(project(":rssparser"))
    }
}


