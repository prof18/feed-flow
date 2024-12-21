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

                // Because of https://github.com/cashapp/sqldelight/issues/4357
                // Delete this line when the issue is fixed on Koin side
                implementation("co.touchlab:stately-common:2.1.0")
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
        create("FeedFlowDB") {
            packageName.set("com.prof18.feedflow.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/com/prof18/feedflow/schema"))
            verifyMigrations.set(true)
        }
    }
}

android {
    namespace = "com.prof18.feedflow.database"
}
