plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    androidTarget()

    jvm {
        jvmToolchain(17)
    }

    sourceSets {

        all {
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
            languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3Api")
            languageSettings.optIn("androidx.compose.material.ExperimentalMaterialApi")
        }

        commonMain {
            dependencies {
                implementation(project(":i18n"))
                implementation(project(":core"))

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.preview)
                implementation(compose.materialIconsExtended)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.jsystem.theme.detector)
            }
        }
    }
}

android {
    namespace = "com.prof18.feedflow.shared.ui"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.min.sdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

