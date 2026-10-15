plugins {
    alias(libs.plugins.feedflow.library)
}

kotlin {
    androidLibrary {
        namespace = "com.prof18.feedflow.core"
    }

    compilerOptions {
        optIn.add("kotlin.contracts.ExperimentalContracts")
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.androidx.compose.runtime.annotation)
                implementation(libs.immutable.collections)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.touchlab.kermit)
                implementation(libs.kotlinx.date.time)
                implementation(libs.com.prof18.rss.parser)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.http)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        matching { it.name.startsWith("ios") }.all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }
}
