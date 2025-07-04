import com.mikepenz.aboutlibraries.plugin.AboutLibrariesExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.about.libraries)
    alias(libs.plugins.compose.hotreload)
    alias(libs.plugins.feedflow.detekt)
}

apply(from = "../versioning.gradle.kts")

val appVersionName: () -> String by extra
val appVersionCode: () -> Int by extra

group = "com.prof18"

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

    jvm()

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

                implementation(libs.multiplatform.markdown.renderer.m3)
                implementation(libs.multiplatform.markdown.renderer.coil)
                implementation(libs.voyager.navigator)
                implementation(libs.voyager.transition)
                implementation(libs.material.window.size)
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

            val isMacOS = System.getProperty("os.name").lowercase().contains("mac")

            nativeDistributions {
                outputBaseDir.set(layout.buildDirectory.asFile.get().resolve("release"))

                if (isMacOS) {
                    if (isAppStoreRelease) {
                        appResourcesRootDir.set(project.layout.projectDirectory.dir("resources-sandbox"))
                    } else {
                        appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
                    }
                }

                modules("java.instrument", "java.sql", "jdk.unsupported")

                targetFormats(
                    TargetFormat.Dmg,
                    TargetFormat.Pkg,
                    TargetFormat.Msi,
                    TargetFormat.Exe,
                    TargetFormat.Deb,
                    TargetFormat.Rpm,
                )
                packageName = "FeedFlow"
                packageVersion = appVersionName()

                description = "FeedFlow - RSS Reader"
                copyright = "© 2024 Marco Gomiero. All rights reserved."
                vendor = "Marco Gomiero"

                val iconsRoot = project.file("src/jvmMain/resources/icons/")

                linux {
                    iconFile.set(iconsRoot.resolve("icon.png"))

                    rpmLicenseType = "Apache-2.0"
                    menuGroup = "Marco Gomiero"
                    appCategory = "News"
                }

                windows {
                    iconFile.set(iconsRoot.resolve("icon.ico"))

                    perUserInstall = true
                    menuGroup = "Marco Gomiero"

                    upgradeUuid = "2a997274-d04e-40ae-b912-8f86970bd181".uppercase()
                }

                macOS {
                    iconFile.set(iconsRoot.resolve("icon.icns"))

                    packageName = "FeedFlow"
                    bundleID = "com.prof18.feedflow"

                    packageBuildVersion = appVersionCode().toString()

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
          <string>ta</string>
          <string>he</string>
          <string>uk</string>
          <string>ja</string>
          <string>cs</string>
          <string>lv</string>
        </array>
        <key>NSUbiquitousContainers</key>
        <dict>
            <key>iCloud.com.prof18.feedflow</key>
            <dict>
                <key>NSUbiquitousContainerIsDocumentScopePublic</key>
                <true/>
                <key>NSUbiquitousContainerName</key>
                <string>FeedFlow</string>
                <key>NSUbiquitousContainerSupportedFolderLevels</key>
                <string>Any</string>
            </dict>
        </dict>
    """.trimIndent()

configure<AboutLibrariesExtension> {
    android {
        registerAndroidTasks = false
    }
}

tasks.withType(KotlinCompile::class.java) {
    dependsOn("exportLibraryDefinitions")

    compilerOptions {
        freeCompilerArgs = listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        )
    }
}
