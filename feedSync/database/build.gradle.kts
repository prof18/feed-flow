plugins {
    alias(libs.plugins.feedflow.library)
    alias(libs.plugins.sqldelight)
}

kotlin {
    sourceSets {
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

android {
    namespace = "com.prof18.feedflow.feedsync.database"
}
