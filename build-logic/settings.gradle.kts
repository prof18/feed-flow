dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("./offline-repository") }
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")