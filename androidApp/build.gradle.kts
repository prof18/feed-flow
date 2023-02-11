plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.prof18.feedflow"
    compileSdk = 33
    defaultConfig {
        applicationId = "com.prof18.feedflow"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
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
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
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
}