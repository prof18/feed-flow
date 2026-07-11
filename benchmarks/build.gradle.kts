import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.feedflow.detekt)
}

android {
    namespace = "com.prof18.feedflow.benchmarks"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = 28
        targetSdk = libs.versions.android.target.sdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    targetProjectPath = ":androidApp"

    flavorDimensions += "version"
    productFlavors {
        create("fdroid") {
            dimension = "version"
        }

        create("googlePlay") {
            dimension = "version"
        }
    }
}

// The TestedApks configurations resolve the app's dependency graph without Android
// attributes, so KMP libraries (e.g. compose-webview-multiplatform) would fall back
// to their desktop variants and fail resolution.
configurations.configureEach {
    if (name.endsWith("TestedApks")) {
        attributes {
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
            attribute(
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                objects.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.ANDROID),
            )
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.junit)
}
