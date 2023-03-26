import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
}

group = "com.prof18"
version = "1.0-SNAPSHOT"


kotlin {
    jvm {
        jvmToolchain(11)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.preview)
                implementation(libs.koin.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.decompose)
                implementation(libs.decompose.compose.jetbrains)
                implementation(libs.jsystem.theme.detector)

                implementation("org.slf4j:slf4j-nop:2.0.6")

            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "com.prof18.feedflow.MainKt"
        nativeDistributions {

            modules("java.instrument", "java.sql", "jdk.unsupported")

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "FeedFlow"
            packageVersion = "1.0.0"

            description = "FeedFlow - Read RSS Feed"
            copyright = "© 2023 Marco Gomiero. All rights reserved."

            val iconsRoot = project.file("src/jvmMain/resources/icons/")

            macOS {
                iconFile.set(iconsRoot.resolve("icon.icns"))
            }
            windows {
                iconFile.set(project.file("icon.ico"))
            }
            linux {
                iconFile.set(project.file("icon.png"))
            }
        }
    }
}
