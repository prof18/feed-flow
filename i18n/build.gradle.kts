import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.moko.resources)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm {
        jvmToolchain(17)
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "i18n"
            isStatic = true
        }
    }

    // TODO: remove when Moko resource fix the issue
    applyDefaultHierarchyTemplate()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
        }

        commonMain {
            dependencies {
                api(libs.moko.resources)
                api(libs.moko.resourcesCompose)
            }
        }

        // TODO: remove when Moko resource fix the issue
        androidMain {
            dependsOn(commonMain.get())
        }

        // TODO: remove when Moko resource fix the issue
        jvmMain {
            dependsOn(commonMain.get())
        }
    }
}

android {
    namespace = "com.prof18.feedflow.i18n"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.min.sdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        disable.add("MissingTranslation")
    }
}

multiplatformResources {
    multiplatformResourcesPackage = "com.prof18.feedflow"
//    multiplatformResourcesClassName = "StringRes" // optional, default MR
    disableStaticFrameworkWarning = true
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
