import co.touchlab.skie.configuration.SuspendInterop
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.feedflow.detekt)
    alias(libs.plugins.skie)
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
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
            export(libs.touchlab.kermit.simple)
            export(libs.androidx.lifecycle.viewModel)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
        }

        commonMain {
            dependencies {
                implementation(project(":database"))
                implementation(project(":feedSync:database"))
                implementation(project(":feedSync:icloud"))
                implementation(project(":feedSync:greader"))
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.com.prof18.rss.parser)
                implementation(libs.multiplatform.settings)
                implementation(libs.kotlinx.date.time)
                implementation(libs.ktor.client.core)
                implementation(libs.skie.annotation)

                api(project(":core"))
                api(project(":i18n"))
                api(project(":feedSync:dropbox"))
                api(libs.touchlab.kermit)
                api(libs.immutable.collections)
                api(libs.androidx.lifecycle.viewModel)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.touchlab.kermit.test)
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

        val androidUnitTest by getting {
            dependsOn(commonJvmAndroidTest)

            dependencies {
                implementation(libs.junit)
                implementation(libs.org.robolectric)
                implementation(libs.androidx.test.core.ktx)
            }
        }

        iosMain {
            dependsOn(commonMobileMain)

            dependencies {
                api(libs.touchlab.kermit.simple)
                implementation(libs.touchlab.kermit.crash)
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
        }
    }
}

android {
    namespace = "com.prof18.feedflow.shared"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.min.sdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

skie {
    features {
        group {
            SuspendInterop.Enabled(false)
        }
    }
}
