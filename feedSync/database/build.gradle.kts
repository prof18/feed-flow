plugins {
    alias(libs.plugins.feedflow.library)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidLibrary {
        namespace = "com.prof18.feedflow.feedsync.database"
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }

        commonMain {
            dependencies {
                implementation(project(":core"))
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutine.extensions)
                implementation(libs.sqldelight.primitive.adapter)
                implementation(libs.touchlab.kermit)
                implementation(libs.kotlinx.date.time)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.stately.concurrency)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.sqldelight.android.driver)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.sqldelight.native.driver)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

sqldelight {
    databases {
        create("FeedFlowFeedSyncDB") {
            packageName.set("com.prof18.feedflow.feedsync.database.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/com/prof18/feedflow/feedsync/database/schema"))
            verifyMigrations.set(true)
        }
    }
}
