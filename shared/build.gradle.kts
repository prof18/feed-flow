import co.touchlab.skie.configuration.SuspendInterop
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.feedflow.detekt)
    alias(libs.plugins.skie)
}

kotlin {
    jvmToolchain(21)

    androidLibrary {
        namespace = "com.prof18.feedflow.shared"
        compileSdk = libs.versions.android.compile.sdk.get().toInt()
        minSdk = libs.versions.android.min.sdk.get().toInt()
        compilerOptions.jvmTarget = JvmTarget.JVM_21

        withHostTest {}

        androidResources {
            enable = true
        }
    }

    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "FeedFlowKit"
            isStatic = true
            binaryOption("bundleId", "com.prof18.feedflow.FeedFlowKit")

            export(project(":i18n"))
            export(project(":core"))
            export(project(":feedSync:dropbox"))
            export(project(":feedSync:googledrive"))
            export(libs.touchlab.kermit.simple)
            export(libs.androidx.lifecycle.viewModel)
        }
        it.binaries.all {
            linkerOpts("-lsqlite3")
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
            languageSettings.optIn("kotlin.time.ExperimentalTime")
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            languageSettings.optIn("kotlinx.coroutines.FlowPreview")
            languageSettings.optIn("co.touchlab.kermit.ExperimentalKermitApi")
        }

        matching { it.name.startsWith("ios") }.all {
            languageSettings.optIn("com.russhwolf.settings.ExperimentalSettingsApi")
            languageSettings.optIn("com.russhwolf.settings.ExperimentalSettingsImplementation")
            languageSettings.optIn("kotlin.experimental.ExperimentalNativeApi")
            languageSettings.optIn("kotlinx.cinterop.BetaInteropApi")
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }

        commonMain {
            dependencies {
                implementation(project(":database"))
                implementation(project(":feedSync:database"))
                implementation(project(":feedSync:icloud"))
                implementation(project(":feedSync:greader"))
                implementation(project(":feedSync:networkcore"))
                implementation(project(":feedSync:feedbin"))
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.com.prof18.rss.parser)
                implementation(libs.csv)
                implementation(libs.multiplatform.settings)
                implementation(libs.kotlinx.date.time)
                implementation(libs.ktor.client.core)
                implementation(libs.skie.annotation)

                api(project(":core"))
                api(project(":i18n"))
                api(project(":feedSync:dropbox"))
                api(project(":feedSync:googledrive"))
                api(libs.touchlab.kermit)
                api(libs.immutable.collections)
                api(libs.androidx.lifecycle.viewModel)
            }
        }

        commonTest {
            dependencies {
                implementation(project(":database"))
                implementation(project(":feedSync:feedbin"))
                implementation(project(":feedSync:test-utils"))
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.touchlab.kermit.test)
                implementation(libs.kotest.assertions)
                implementation(libs.kotest.property)
                implementation(libs.koin.test)
                implementation(libs.turbine)
                implementation(libs.multiplatform.settings.test)
                implementation(libs.ktor.client.mock)
            }
        }

        val commonJvmAndroidMain by creating {
            dependsOn(commonMain.get())

            dependencies {
                implementation(libs.jsoup)
                implementation(libs.readability4j)
                implementation(libs.ktor.client.okhttp)
            }
        }

        val commonJvmAndroidTest by creating {
            dependsOn(commonTest.get())

            dependencies {
                implementation(libs.kotlin.test.junit)
            }
        }

        val commonMobileMain by creating {
            dependsOn(commonMain.get())
        }

        androidMain {
            dependsOn(commonJvmAndroidMain)
            dependsOn(commonMobileMain)

            dependencies {
                implementation(libs.koin.android)
                implementation(libs.workmanager)
                implementation(libs.koin.workmanager)
            }
        }

        getByName("androidHostTest") {
            dependsOn(commonJvmAndroidTest)

            dependencies {
                implementation(libs.junit)
                implementation(libs.org.robolectric)
                implementation(libs.androidx.test.core.ktx)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

        iosMain {
            dependsOn(commonMobileMain)

            dependencies {
                api(libs.touchlab.kermit.simple)
                implementation(libs.touchlab.kermit.crash)
                implementation(libs.crashk.ios)
                implementation(libs.ktor.client.darwin)
            }
        }

        jvmMain {
            dependsOn(commonJvmAndroidMain)

            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.flexmark.html2md.converter)
                api(libs.sentry)
            }
        }

        jvmTest {
            dependsOn(commonJvmAndroidTest)
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}

// Configure TEST_RESOURCES_ROOT for JVM and iOS tests
val testResourcesDir = project(":feedSync:test-utils")
    .file("src/commonMain/resources")
    .absolutePath

tasks.withType<Test> {
    environment("TEST_RESOURCES_ROOT", testResourcesDir)
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest> {
    environment("TEST_RESOURCES_ROOT", testResourcesDir)
    // iOS simulator requires SIMCTL_CHILD_ prefix for environment variables
    environment("SIMCTL_CHILD_TEST_RESOURCES_ROOT", testResourcesDir)
}

skie {
    features {
        group {
            SuspendInterop.Enabled(false)
        }
    }
}
