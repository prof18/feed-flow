import com.mikepenz.aboutlibraries.plugin.AboutLibrariesExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.about.libraries)
}

group = "com.prof18"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    jvmToolchain {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(17)
    }

    jvm {
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
                implementation(compose.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.bundles.about.libraries)
                implementation(libs.jsoup)
                implementation(libs.slf4j.nop)

                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)

                implementation(libs.flexmark.html2md.converter)
                implementation(libs.multiplatform.markdown.renderer.m3)
                implementation(libs.multiplatform.markdown.renderer.coil)
                implementation(libs.voyager.navigator)
                implementation(libs.voyager.transition)
            }
        }
    }
}

compose {
    resources {
        packageOfResClass = "com.prof18.feedflow.desktop.resources"
        generateResClass = always
    }

    desktop {
        application {
            mainClass = "com.prof18.feedflow.desktop.MainKt"

            // Enable only to test translations
//            jvmArgs.add("-Duser.language=zh")
//            jvmArgs.add("-Duser.country=CN")

            buildTypes.release.proguard {
                configurationFiles.from(project.file("compose-desktop.pro"))
            }

            val isAppStoreRelease = project.property("macOsAppStoreRelease").toString().toBoolean()

            nativeDistributions {
                outputBaseDir.set(layout.buildDirectory.asFile.get().resolve("release"))

                if (isAppStoreRelease) {
                    appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
                }

                modules("java.instrument", "java.sql", "jdk.unsupported")

                targetFormats(TargetFormat.Dmg, TargetFormat.Pkg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
                packageName = "FeedFlow"
                packageVersion = getVersionName()

                description = "FeedFlow - RSS Reader"
                copyright = "© 2024 Marco Gomiero. All rights reserved."
                vendor = "Marco Gomiero"

                val iconsRoot = project.file("src/jvmMain/resources/icons/")

                linux {
                    packageName = "FeedFlow"
                    iconFile.set(iconsRoot.resolve("icon.png"))

                    rpmLicenseType = "Apache-2.0"
                    menuGroup = "Marco Gomiero"
                    appCategory = "News"
                }

                windows {
                    iconFile.set(iconsRoot.resolve("icon.ico"))

                    perUserInstall = true
                    menuGroup = "Marco Gomiero"

                    // upgradeUuid = "UUID"
                    // https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
                }

                macOS {
                    iconFile.set(iconsRoot.resolve("icon.icns"))

                    packageName = "FeedFlow"
                    bundleID = "com.prof18.feedflow"

                    appStore = isAppStoreRelease

                    signing {
                        sign.set(true)
                        identity.set("Marco Gomiero")
                    }

                    minimumSystemVersion = "12.0"

                    if (isAppStoreRelease) {
                        entitlementsFile.set(project.file("entitlements.plist"))
                        runtimeEntitlementsFile.set(project.file("runtime-entitlements.plist"))
                        provisioningProfile.set(project.file("embedded.provisionprofile"))
                        runtimeProvisioningProfile.set(project.file("runtime.provisionprofile"))
                    } else {
                        entitlementsFile.set(project.file("default.entitlements"))
                    }

                    appCategory = "public.app-category.news"

                    notarization {
                        appleID.set("mgp.dev.studio@gmail.com")
                        password.set("@keychain:NOTARIZATION_PASSWORD")
                    }

                    infoPlist {
                        extraKeysRawXml = macExtraPlistKeys
                    }
                }
            }
        }
    }
}

val macExtraPlistKeys: String
    get() = """
        <key>ITSAppUsesNonExemptEncryption</key>
        <false/>
        <key>CFBundleLocalizations</key>
        <array>
          <string>en</string>
          <string>it</string>
          <string>fr</string>
          <string>hu</string>
          <string>pl</string>
          <string>nb-rNO</string>
          <string>de</string>
          <string>sk</string>
          <string>pt-BR</string>
          <string>es</string>
          <string>zh-CN</string>
          <string>ru</string>
          <string>et</string>
          <string>gl</string>
          <string>vi</string>
        </array>
    """.trimIndent()

@Suppress("UnstableApiUsage")
fun getVersionCode(): Int =
    providers.exec {
        commandLine("git", "rev-list", "HEAD", "--first-parent", "--count")
    }.standardOutput.asText.get().trim().toInt()

@Suppress("UnstableApiUsage")
fun getVersionName(): String =
    providers.exec {
        commandLine("git", "describe", "--tags", "--abbrev=0", "--match", "*-desktop")
    }.standardOutput
        .asText.get()
        .trim()
        .replace("-desktop", "")

configure<AboutLibrariesExtension> {
    registerAndroidTasks = false
}

tasks.withType(KotlinCompile::class.java) {
    dependsOn("exportLibraryDefinitions")

    compilerOptions {
        freeCompilerArgs = listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        )
    }
}
