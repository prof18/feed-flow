plugins {
    alias(libs.plugins.feedflow.library)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.multiplatform.settings)
            }
        }
    }
}

android {
    namespace = "com.prof18.feedflow.feedsync.icloud"
}
