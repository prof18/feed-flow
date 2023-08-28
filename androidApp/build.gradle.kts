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
    namespace = "com.prof18.feedflow"
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
        kotlinCompilerExtensionVersion = libs.versions.compose.multiplatform.get()
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
        }
    }
}

compose {
    kotlinCompilerPlugin.set(libs.versions.compose.multiplatform.get())
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
    implementation(libs.bundles.koin)
    implementation(libs.bundles.about.libraries)
    implementation(libs.moko.resourcesCompose)
    implementation(libs.firebase.crashlytics.ktx)

    debugImplementation(compose.uiTooling)
}

play {
    // The play_config.json file will be provided on CI
    serviceAccountCredentials.set(file("../play_config.json"))
    track.set("alpha")
}

fun getVersionCode(): Int {
    val outputStream = org.apache.commons.io.output.ByteArrayOutputStream()
    project.exec {
        commandLine = "git rev-list HEAD --first-parent --count".split(" ")
        standardOutput = outputStream
    }
    return outputStream.toString().trim().toInt()
}

fun getVersionName(): String {
    val outputStream = org.apache.commons.io.output.ByteArrayOutputStream()
    project.exec {
        commandLine = listOf("git", "describe", "--tags", "--abbrev=0", "--match", "*-android")
        standardOutput = outputStream
    }
    return outputStream.toString()
        .trim()
        .replace("-android", "")
}
