import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.triplet.play)
}

val local = Properties()
val localProperties: File = rootProject.file("keystore.properties")
if (localProperties.exists()) {
    localProperties.inputStream().use { local.load(it) }
}


android {
    namespace = "com.prof18.feedflow"
    compileSdk = 33
    defaultConfig {
        applicationId = "com.prof18.feedflow"
        minSdk = 26
        targetSdk = 33
        versionCode = getVersionCode()
        versionName = getVersionName()
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    packagingOptions {
        resources {
//            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
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

dependencies {
    implementation(project(":shared"))
//    implementation(project("RSS-Parser:rssparser"))

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.bundles.compose)
    implementation(libs.bundles.koin)
    implementation(libs.touchlab.kermit)
    implementation(libs.androidx.work.manager)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
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
