plugins {
    alias(libs.plugins.feedflow.library)
}

kotlin {
    sourceSets {
        matching { it.name.startsWith("ios") }.all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
            languageSettings.optIn("kotlinx.cinterop.BetaInteropApi")
        }

        commonMain {
            dependencies {
                implementation(libs.multiplatform.settings)
            }
        }

        iosMain {
            dependencies {
                implementation(project(":core"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.touchlab.kermit)
            }
        }
    }
}

android {
    namespace = "com.prof18.feedflow.feedsync.icloud"
}
