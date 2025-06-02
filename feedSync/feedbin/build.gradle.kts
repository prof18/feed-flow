plugins {
    alias(libs.plugins.feedflow.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()
    androidTarget {
        publishLibraryVariants("release", "debug")
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    js {
        browser()
        nodejs()
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))
                implementation(project(":feedSync:networkcore")) // Assuming this is needed like in greader
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.touchlab.kermit) // Assuming this is a common logging library
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.client.logging)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        
        jsMain {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }

        desktopMain { // Common for macosX64, macosArm64
            dependencies {
                implementation(libs.ktor.client.curl) // Or another suitable engine like okhttp if preferred
            }
        }
    }
}

android {
    namespace = "com.prof18.feedflow.feedsync.feedbin"
}
