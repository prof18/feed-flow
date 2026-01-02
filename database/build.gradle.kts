plugins {
    alias(libs.plugins.feedflow.library)
    alias(libs.plugins.sqldelight)
}

kotlin {

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))

                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutine.extensions)
                implementation(libs.sqldelight.primitive.adapter)
                implementation(libs.touchlab.kermit)
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
