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


    compilerOptions {
        optIn.add("kotlin.experimental.ExperimentalObjCName")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
        optIn.add("androidx.compose.foundation.ExperimentalFoundationApi")
        optIn.add("androidx.compose.foundation.layout.ExperimentalLayoutApi")
        optIn.add("kotlinx.coroutines.FlowPreview")
    }

    androidTarget()
    jvm()

    sourceSets {
        androidMain {
            dependencies {
                api(libs.io.coil.network)
                implementation(compose.preview)
                implementation(libs.compose.material3)
            }
        }

        commonMain {
            dependencies {
                api(project(":i18n"))
                implementation(project(":core"))

                api(libs.lyricist)
                api(libs.io.coil.compose)
 
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.material3)
                implementation(libs.compose.multiplatform.ui.tooling.preview)
                implementation(libs.compose.multiplatform.material.icons.extended)
                implementation(libs.compose.multiplatform.material3.adaptive.navigationsuite)
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
