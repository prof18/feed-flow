plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ksp)
    alias(libs.plugins.native.coroutines)
    alias(libs.plugins.crashk.ios.linking)
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
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            export(project(":i18n"))
            export(libs.moko.resources)
            export(libs.touchlab.kermit.simple)
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
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.com.prof18.rss.parser)
                implementation(libs.multiplatform.settings)
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.test.junit)

                api(project(":i18n"))
                api(libs.touchlab.kermit)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.touchlab.kermit.test)
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

        val commonMobileMain by creating {
            dependsOn(commonMain)

            dependencies {
                implementation(libs.crashk.ios)
                implementation(libs.touchlab.kermit.crashlytics)
            }
        }

        val androidMain by getting {
            dependsOn(commonJvmAndroidMain)
            dependsOn(commonMobileMain)

            dependencies {
                implementation(libs.squareup.sqldelight.android.driver)
                implementation(libs.androidx.lifecycle.viewModel.ktx)
                implementation(libs.koin.android)
                implementation(libs.crashk.ios)
                implementation(libs.touchlab.kermit.crashlytics)
            }
        }
        val androidUnitTest by getting {
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
            dependsOn(commonMobileMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)

            dependencies {
                implementation(libs.squareup.sqldelight.native.driver)

                api(libs.touchlab.kermit.simple)
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
                api(libs.sentry)
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
        schemaOutputDirectory = file("src/commonMain/sqldelight/com/prof18/feedflow/schema")

        verifyMigrations = true
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
