import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.triplet.play)
    alias(libs.plugins.about.libraries)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.google.services)
    alias(libs.plugins.feedflow.detekt)
    alias(libs.plugins.kotlin.serialization)
}

val local = Properties()
val localProperties: File = rootProject.file("keystore.properties")
if (localProperties.exists()) {
    localProperties.inputStream().use { local.load(it) }
}

val dropboxAppKey: String = local.getProperty("dropbox_key").orEmpty()
if (dropboxAppKey.isEmpty()) {
    println("Dropbox key not set in keystore.properties. Please add it to the file with the key 'dropbox_key'")
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

        addManifestPlaceholders(
            mapOf(
                "dropboxKey" to dropboxAppKey,
            ),
        )
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

    signingConfigs {
        create("release") {
            keyAlias = local.getProperty("keyAlias")
            keyPassword = local.getProperty("keyPassword")
            storeFile = file(local.getProperty("storeFile") ?: "NOT_FOUND")
            storePassword = local.getProperty("storePassword")
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("String", "DROPBOX_APP_KEY", "\"$dropboxAppKey\"")
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "DROPBOX_APP_KEY", "\"$dropboxAppKey\"")
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("fdroid") {
            dimension = "version"
        }

        create("googlePlay") {
            dimension = "version"
            isDefault = true
        }
    }

    aboutLibraries {
        excludeFields = arrayOf("generated")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs = listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        )
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":sharedUI"))

    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.ui)
    implementation(compose.uiTooling)
    implementation(libs.androidx.material3)

    implementation(libs.bundles.compose)
    implementation(libs.bundles.about.libraries)

    implementation(libs.material.window.size)
    implementation(libs.androidx.browser)
    implementation(libs.compose.webview)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.workmanager)
    implementation(libs.koin.composeVM)

    implementation(libs.dropbox.core.android)
    implementation(libs.workmanager)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material)

    "googlePlayImplementation"(platform(libs.firebase.bom))
    "googlePlayImplementation"(libs.firebase.crashlytics)
    "googlePlayImplementation"(libs.touchlab.kermit.crash)
    "googlePlayImplementation"(libs.play.review)

    debugImplementation(compose.uiTooling)

    testImplementation(libs.koin.test)
}

play {
    // The play_config.json file will be provided on CI
    serviceAccountCredentials.set(file("../play_config.json"))
    track.set("internal")
}

fun getVersionCode(): Int =
    providers.exec {
        commandLine("git", "rev-list", "HEAD", "--first-parent", "--count")
    }.standardOutput.asText.get().trim().toInt()

fun getVersionName(): String =
    providers.exec {
        commandLine("git", "describe", "--tags", "--abbrev=0", "--match", "*-android")
    }.standardOutput
        .asText.get()
        .trim()
        .replace("-android", "")

android.applicationVariants.configureEach {
    val name = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    val googleTask = tasks.findByName("process${name}GoogleServices")
    val uploadTask = tasks.findByName("uploadCrashlyticsMappingFile${name}")
    googleTask?.enabled = !name.contains("Fdroid")
    uploadTask?.enabled = !name.contains("Fdroid")
}
