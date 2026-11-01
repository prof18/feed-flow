plugins {
    alias(libs.plugins.feedflow.library)
}

kotlin {
    android {
        namespace = "com.prof18.feedflow.feedsync.dropbox"
        withHostTest {}
    }

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

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val commonJvmAndroidMain = create("commonJvmAndroidMain") {
            dependsOn(commonMain.get())

            dependencies {
                implementation(libs.dropbox.core)
            }
        }

        val commonJvmAndroidTest = create("commonJvmAndroidTest") {
            dependsOn(commonTest.get())
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

        jvmTest {
            dependsOn(commonJvmAndroidTest)
        }

        getByName("androidHostTest") {
            dependsOn(commonJvmAndroidTest)
        }
    }
}
