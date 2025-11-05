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
                implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
            }
        }

        androidMain {
            dependsOn(commonJvmAndroidMain)

            dependencies {
                api("com.google.android.gms:play-services-auth:20.7.0")
            }
        }

        jvmMain {
            dependsOn(commonJvmAndroidMain)

            dependencies {
                api("com.google.api-client:google-api-client:2.2.0")
            }
        }
    }
}

android {
    namespace = "com.prof18.feedflow.feedsync.googledrive"
}
