plugins {
    alias(libs.plugins.feedflow.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    applyDefaultHierarchyTemplate()

    sourceSets {
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

            dependencies {
                // Google API Client libraries for JVM/Android
                implementation("com.google.api-client:google-api-client:2.2.0")
                implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
                implementation("com.google.auth:google-auth-library-oauth2-http:1.41.0")
            }
        }

        androidMain {
            dependsOn(commonJvmAndroidMain)

            dependencies {
                // Credential Manager
                api("androidx.credentials:credentials:1.5.0")
                api("androidx.credentials:credentials-play-services-auth:1.5.0")
                api("com.google.android.libraries.identity.googleid:googleid:1.1.1")

                // Authorization API
                api("com.google.android.gms:play-services-auth:21.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

                // Drive API (Standard Java Client)
                api("com.google.api-client:google-api-client-android:2.8.1")
                api("com.google.apis:google-api-services-drive:v3-rev20240123-2.0.0")
            }
        }

        jvmMain {
            dependsOn(commonJvmAndroidMain)

            dependencies {
                api("com.google.api-client:google-api-client:2.2.0")
                api("com.google.oauth-client:google-oauth-client-jetty:1.34.1") // Handles "Open Browser" auth
                api("com.google.apis:google-api-services-drive:v3-rev20240123-2.0.0")
            }
        }
    }
}

android {
    namespace = "com.prof18.feedflow.feedsync.googledrive"
}
