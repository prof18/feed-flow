plugins {
    alias(libs.plugins.feedflow.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.prof18.feedflow.feedsync.googledrive"
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        matching { it.name.startsWith("ios") }.all {
            languageSettings.optIn("kotlinx.cinterop.BetaInteropApi")
        }

        commonMain {
            dependencies {
                implementation(project(":core"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.touchlab.kermit)
                implementation(libs.multiplatform.settings)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val commonJvmAndroidMain by creating {
            dependsOn(commonMain.get())
        }

        androidMain {
            dependsOn(commonJvmAndroidMain)
        }

        jvmMain {
            dependsOn(commonJvmAndroidMain)

            dependencies {
                implementation(libs.google.api.client)
                // Handles "Open Browser" auth
                implementation(libs.google.oauth.client.jetty)
                implementation(libs.google.api.services.drive)
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
