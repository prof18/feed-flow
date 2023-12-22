import com.mikepenz.aboutlibraries.plugin.AboutLibrariesExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.about.libraries)
}

group = "com.prof18"
version = "1.0-SNAPSHOT"

kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
    }
    sourceSets {
        jvmMain {
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            dependencies {
                implementation(project(":shared"))
                implementation(project(":sharedUI"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.preview)
                implementation(compose.materialIconsExtended)
                implementation(libs.koin.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.decompose)
                implementation(libs.decompose.compose.jetbrains)
                implementation(libs.jsystem.theme.detector)
                implementation(libs.compose.image.loader)
                implementation(libs.moko.resourcesCompose)
                implementation(libs.bundles.about.libraries)
                implementation(libs.jsoup)
                implementation(libs.slf4j.nop)
            }
        }
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

                outputBaseDir.set(layout.buildDirectory.asFile.get().resolve("release"))

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

configure<AboutLibrariesExtension> {
    registerAndroidTasks = false
}

tasks.withType(KotlinCompile::class.java) {
    dependsOn("exportLibraryDefinitions")
}
