import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.feedflow.detekt)
}

java {
    toolchain {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }

    androidTarget()
    jvm()

    sourceSets {

        all {
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
            languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3Api")
        }

        androidMain {
            dependencies {
                api(libs.io.coil.network)
                implementation(compose.preview)
            }
        }

        commonMain {
            dependencies {
                api(project(":i18n"))
                implementation(project(":core"))

                api(libs.lyricist)
                api(libs.io.coil.compose)

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.materialIconsExtended)
                implementation(compose.material3AdaptiveNavigationSuite)
                implementation(libs.immutable.collections)
                implementation(libs.saket.swipe)
                implementation(libs.components.ui.tooling.preview)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.jsystem.theme.detector)
                api(libs.io.coil.network)
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// Android preview support
dependencies {
    debugImplementation(compose.uiTooling)
}
