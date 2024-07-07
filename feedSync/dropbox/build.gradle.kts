plugins {
    alias(libs.plugins.feedflow.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.touchlab.kermit)
                implementation(libs.multiplatform.settings)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
            }
        }

        val commonJvmAndroidMain by creating {
            dependsOn(commonMain.get())

            dependencies {
                implementation(libs.dropbox.core)
            }
        }

        androidMain {
            dependsOn(commonJvmAndroidMain)

            dependencies {
                api(libs.dropbox.core.android)
            }
        }

        jvmMain {
            dependsOn(commonJvmAndroidMain)

            dependencies {
                api(libs.dropbox.core)
            }
        }
    }
}

android {
    namespace = "com.prof18.feedflow.feedsync.dropbox"
}
