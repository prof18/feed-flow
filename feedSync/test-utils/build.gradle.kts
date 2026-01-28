plugins {
    alias(libs.plugins.feedflow.library)
}

kotlin {
    androidLibrary {
        namespace = "com.prof18.feedflow.feedsync.test"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))
                implementation(project(":database"))
                implementation(project(":feedSync:greader"))
                implementation(project(":feedSync:feedbin"))
                implementation(project(":feedSync:networkcore"))
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.mock)
                implementation(libs.ktor.resources)
                implementation(libs.ktor.content.negotiation)
                implementation(libs.ktor.serialization)
                implementation(libs.touchlab.kermit)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
            // Include commonMain resources in Android main so they're available in the JAR
            resources.srcDirs("src/commonMain/resources")
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}
