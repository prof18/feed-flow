plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.touchlab.kermit)
    alias(libs.plugins.native.coroutines)
    alias(libs.plugins.ksp)
    alias(libs.plugins.moko.resources)
}

kotlin {
    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm("desktop") {
        jvmToolchain(17)
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            export(libs.moko.resources)
        }
    }

    sourceSets {

       all {
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
        }

        val commonMain by getting {
            dependencies {
                implementation(libs.squareup.sqldelight.runtime)
                implementation(libs.squareup.sqldelight.coroutine.extensions)
                implementation(libs.koin.core)
                implementation(libs.touchlab.kermit)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.com.prof18.rss.parser)
                implementation(libs.multiplatform.settings)

                api(libs.moko.resources)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val commonJvmAndroidMain by creating {
            dependsOn(commonMain)

            dependencies {
                implementation(libs.jsoup)
            }
        }

        val commonJvmAndroidTest by creating {
            dependsOn(commonTest)
        }


        val androidMain by getting {
            dependsOn(commonJvmAndroidMain)

            dependencies {
                implementation(libs.squareup.sqldelight.android.driver)
                implementation(libs.androidx.lifecycle.viewModel.ktx)
                implementation(libs.koin.android)
            }
        }
        val androidTest by getting {
            dependsOn(commonJvmAndroidTest)

            dependencies {
                implementation(libs.junit)
                implementation(libs.org.robolectric)
                implementation(libs.squareup.sqldelight.sqlite.driver)
                implementation(libs.androidx.test.core.ktx)
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)

            dependencies {
                implementation(libs.squareup.sqldelight.native.driver)
            }
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)

            dependencies {
                implementation(libs.squareup.sqldelight.native.driver)
            }
        }


        val desktopMain by getting {
            dependsOn(commonJvmAndroidMain)

            dependencies {
                implementation(libs.squareup.sqldelight.sqlite.driver)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        val desktopTest by getting {
            dependsOn(commonJvmAndroidTest)
            dependsOn(commonTest)

        }
    }
}

sqldelight {
    database("FeedFlowDB") {
        packageName = "com.prof18.feedflow.db"
    }
}

android {
    namespace = "com.prof18.feedflow.shared"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.min.sdk.get().toInt()
        targetSdk = libs.versions.android.target.sdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val releaseBuild: String by project

kermit {
    if(releaseBuild.toBoolean()) {
        stripBelow = co.touchlab.kermit.gradle.StripSeverity.Info
    }
}

multiplatformResources {
    multiplatformResourcesPackage = "com.prof18.feedflow"
//    multiplatformResourcesClassName = "StringRes" // optional, default MR
    iosBaseLocalizationRegion = "en" // optional, default "en"
}

// Various fixes for moko-resources tasks
// iOS
if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
    afterEvaluate {
        tasks.findByPath("kspKotlinIosArm64")?.apply {
            dependsOn(tasks.getByPath("generateMRiosArm64Main"))
        }
        tasks.findByPath("kspKotlinIosSimulatorArm64")?.apply {
            dependsOn(tasks.getByPath("generateMRiosSimulatorArm64Main"))
        }
        tasks.findByPath("kspKotlinIosX64")?.apply {
            dependsOn(tasks.getByPath("generateMRiosX64Main"))
        }
    }
}
// Android
tasks.withType(com.android.build.gradle.tasks.MergeResources::class).configureEach {
    dependsOn(tasks.getByPath("generateMRandroidMain"))
}
tasks.withType(com.android.build.gradle.tasks.MapSourceSetPathsTask::class).configureEach {
    dependsOn(tasks.getByPath("generateMRandroidMain"))
}