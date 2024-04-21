import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.triplet.play)
    alias(libs.plugins.about.libraries)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.google.services)
}

val local = Properties()
val localProperties: File = rootProject.file("keystore.properties")
if (localProperties.exists()) {
    localProperties.inputStream().use { local.load(it) }
}

android {
    namespace = "com.prof18.feedflow.android"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()
    defaultConfig {
        applicationId = "com.prof18.feedflow"
        minSdk = libs.versions.android.min.sdk.get().toInt()
        targetSdk = libs.versions.android.target.sdk.get().toInt()
        versionCode = getVersionCode()
        versionName = getVersionName()
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.majorVersion
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    signingConfigs {
        create("release") {
            keyAlias = local.getProperty("keyAlias")
            keyPassword = local.getProperty("keyPassword")
            storeFile = file(local.getProperty("storeFile") ?: "NOT_FOUND")
            storePassword = local.getProperty("storePassword")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
        )
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":sharedUI"))

    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.material)
    implementation(compose.materialIconsExtended)
    implementation(compose.ui)
    implementation(compose.uiTooling)

    implementation(libs.bundles.compose)
    implementation(libs.bundles.about.libraries)
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.material.window.size)
    implementation(libs.androidx.browser)
    implementation(libs.compose.webview)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    debugImplementation(compose.uiTooling)

    testImplementation(libs.koin.test)
}

play {
    // The play_config.json file will be provided on CI
    serviceAccountCredentials.set(file("../play_config.json"))
    track.set("alpha")
}

@Suppress("UnstableApiUsage")
fun getVersionCode(): Int =
    providers.exec {
        commandLine("git", "rev-list", "HEAD", "--first-parent", "--count")
    }.standardOutput.asText.get().trim().toInt()

@Suppress("UnstableApiUsage")
fun getVersionName(): String =
    providers.exec {
        commandLine("git", "describe", "--tags", "--abbrev=0", "--match", "*-android")
    }.standardOutput
        .asText.get()
        .trim()
        .replace("-android", "")
