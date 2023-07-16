import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
}

group = "com.prof18"
version = "1.0-SNAPSHOT"


kotlin {
    jvm {
        jvmToolchain(17)
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
                implementation(libs.compose.image.loader)
                implementation(libs.moko.resourcesCompose)

                implementation("org.slf4j:slf4j-nop:2.0.6")

            }
        }
        val jvmTest by getting
    }
}

compose {
    desktop {
        application {
            mainClass = "com.prof18.feedflow.MainKt"

//        buildTypes.release.proguard {
//            obfuscate.set(true)
//        }
//
        buildTypes.release.proguard {
            configurationFiles.from(project.file("compose-desktop.pro"))
        }

            nativeDistributions {

                outputBaseDir.set(project.buildDir.resolve("release"))

                modules("java.instrument", "java.sql", "jdk.unsupported")

                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "FeedFlow"
                packageVersion = getVersionName()

                description = "FeedFlow - Read RSS Feed"
                copyright = "© 2023 Marco Gomiero. All rights reserved."

                val iconsRoot = project.file("src/jvmMain/resources/icons/")

                macOS {
                    iconFile.set(iconsRoot.resolve("icon.icns"))

                    packageName = "FeedFlow"
                    bundleID = "com.prof18.feedflow"

                    entitlementsFile.set(project.file("default.entitlements"))

                    signing {
                        sign.set(true)
                        identity.set("Marco Gomiero")
                        // keychain.set("/path/to/keychain")
                    }

                    notarization {
                        appleID.set("mgp.dev.studio@gmail.com")
                        password.set("@keychain:NOTARIZATION_PASSWORD")
                    }
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
}

fun getVersionCode(): Int {
    val outputStream = org.apache.commons.io.output.ByteArrayOutputStream()
    project.exec {
        commandLine = "git rev-list HEAD --first-parent --count".split(" ")
        standardOutput = outputStream
    }
    return outputStream.toString().trim().toInt()
}

fun getVersionName(): String {
    val outputStream = org.apache.commons.io.output.ByteArrayOutputStream()
    project.exec {
        commandLine = listOf("git", "describe", "--tags", "--abbrev=0", "--match", "*-desktop")
        standardOutput = outputStream
    }
    return outputStream.toString()
        .trim()
        .replace("-desktop", "")
}
