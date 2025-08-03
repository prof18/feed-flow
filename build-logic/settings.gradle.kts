pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven { url = uri("../offline-repository") }
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("../offline-repository") }
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")