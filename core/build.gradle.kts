plugins {
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.feedflow.library)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(libs.immutable.collections)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

android {
    namespace = "com.prof18.feedflow.core"
}
