plugins {
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.feedflow.library)
}

kotlin {

    compilerOptions {
        optIn.add("kotlin.contracts.ExperimentalContracts")
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(libs.immutable.collections)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.touchlab.kermit)
                implementation(libs.kotlinx.date.time)
                implementation(libs.com.prof18.rss.parser)
            }
        }

        matching { it.name.startsWith("ios") }.all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }
}

android {
    namespace = "com.prof18.feedflow.core"
}
