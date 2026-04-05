import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.about.libraries)
    alias(libs.plugins.feedflow.detekt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

apply(from = "../versioning.gradle.kts")

val appVersionName: () -> String by extra
val appVersionCode: () -> Int by extra

group = "com.prof18"

java {
    toolchain {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }

    compilerOptions {
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add("androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi")
        optIn.add("androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi")
    }

    jvm()

    sourceSets {
        jvmMain {
            dependencies {
                implementation(project(":shared"))
                implementation(project(":sharedUI"))
                implementation(compose.desktop.currentOs)
                implementation(libs.compose.multiplatform.material3)
                implementation(libs.compose.multiplatform.ui.tooling.preview)
                implementation(libs.compose.multiplatform.material.icons.extended)
                implementation(libs.compose.multiplatform.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.bundles.about.libraries)
                implementation(libs.jsoup)
                implementation(libs.slf4j.nop)

                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.koin.composeVM)

                implementation(libs.multiplatform.markdown.renderer.m3)
                implementation(libs.multiplatform.markdown.renderer.coil)
                implementation(libs.jetbrains.navigation3.ui)
                implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
                implementation(libs.material.window.size)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.date.time)
                implementation(libs.flatlaf)
                implementation(libs.compose.multiplatform.material3.adaptive.layout)
                implementation(libs.compose.multiplatform.material3.adaptive.navigation)
                implementation(libs.haze)
                implementation(libs.haze.materials)
                implementation(libs.compose.unstyled.primitives)
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

val isAppStoreRelease = project.property("macOsAppStoreRelease").toString().toBoolean()
val isMacOS = System.getProperty("os.name").lowercase().contains("mac")
val macSigningIdentity = "Marco Gomiero"
// Full identity name for codesign. Compose's MacSigner derives this same form
// internally (see ValidatedMacOSSigningSettings.fullDeveloperID).
val macCodesignIdentity = if (isAppStoreRelease) {
    "3rd Party Mac Developer Application: $macSigningIdentity"
} else {
    "Developer ID Application: $macSigningIdentity"
}
val macEntitlementsFile = if (isAppStoreRelease) {
    project.file("entitlements.plist")
} else {
    project.file("default.entitlements")
}

val propsFile = project.file("src/jvmMain/resources/props.properties")
val props = Properties()
if (propsFile.exists()) {
    propsFile.inputStream().use(props::load)
}
val isReleaseBuild = props.getProperty("is_release", "false").toBoolean()

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

            nativeDistributions {
                outputBaseDir.set(layout.buildDirectory.asFile.get().resolve("release"))

                if (isMacOS) {
                    if (isAppStoreRelease) {
                        appResourcesRootDir.set(project.layout.projectDirectory.dir("resources-sandbox"))
                    } else {
                        appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
                    }
                }

                modules("java.instrument", "java.sql", "jdk.unsupported", "jdk.httpserver")

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

                val iconsRoot = if (isReleaseBuild) {
                    project.file("src/jvmMain/resources/icons/")
                } else {
                    project.file("src/jvmMain/resources/icons-debug/")
                }

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
                        identity.set(macSigningIdentity)
                    }

                    minimumSystemVersion = "12.0"

                    entitlementsFile.set(macEntitlementsFile)
                    if (isAppStoreRelease) {
                        runtimeEntitlementsFile.set(project.file("runtime-entitlements.plist"))
                        provisioningProfile.set(project.file("embedded.provisionprofile"))
                        runtimeProvisioningProfile.set(project.file("runtime.provisionprofile"))
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
        <key>CFBundleIconName</key>
        <string>AppIcon</string>
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
          <string>tr</string>
          <string>ar</string>
          <string>sq</string>
          <string>ca</string>
          <string>zh-Hant</string>
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

// Copy the Liquid Glass asset catalog (Assets.car) into Contents/Resources/ of the
// packaged .app. macOS 26's CFBundleIconName lookup requires Assets.car at that
// bundle-level path. jpackage / Compose don't know about this file, so we copy it
// ourselves after the distributable is assembled.
val assetsCarSource = if (isAppStoreRelease || isReleaseBuild) {
    project.file("macos-icon/release/Assets.car")
} else {
    project.file("macos-icon/debug/Assets.car")
}

// When Compose signs the .app via jpackage during createDistributable, the bundle's
// CodeResources seal captures every file under Contents/. Copying Assets.car in
// afterward invalidates the signature and notarization rejects the app. After the
// copy we re-sign the bundle with the same identity and entitlements so the seal
// matches the final contents.
val copyAssetsCarDebug by tasks.registering {
    val appDirProvider = layout.buildDirectory.dir("release/main/app/FeedFlow.app")
    val assetsCarFile = assetsCarSource
    val identity = macCodesignIdentity
    val entitlements = macEntitlementsFile
    doLast {
        val appDir = appDirProvider.get().asFile
        val target = appDir.resolve("Contents/Resources/Assets.car")
        target.parentFile.mkdirs()
        assetsCarFile.copyTo(target, overwrite = true)
        // Re-sign only if the bundle was signed in the first place (local dev
        // builds may skip signing when no identity is available).
        if (appDir.resolve("Contents/_CodeSignature").exists()) {
            val process = ProcessBuilder(
                "codesign", "--force", "--timestamp", "--options", "runtime",
                "--sign", identity,
                "--entitlements", entitlements.absolutePath,
                appDir.absolutePath,
            ).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            val result = process.waitFor()
            println("codesign: $output")
            if (result != 0) {
                throw GradleException("codesign re-sign of ${appDir.name} failed with exit code $result:\n$output")
            }
        }
    }
}

val copyAssetsCarRelease by tasks.registering {
    val appDirProvider = layout.buildDirectory.dir("release/main-release/app/FeedFlow.app")
    val assetsCarFile = assetsCarSource
    val identity = macCodesignIdentity
    val entitlements = macEntitlementsFile
    doLast {
        val appDir = appDirProvider.get().asFile
        val target = appDir.resolve("Contents/Resources/Assets.car")
        target.parentFile.mkdirs()
        assetsCarFile.copyTo(target, overwrite = true)
        // Re-sign only if the bundle was signed in the first place (local dev
        // builds may skip signing when no identity is available).
        if (appDir.resolve("Contents/_CodeSignature").exists()) {
            val process = ProcessBuilder(
                "codesign", "--force", "--timestamp", "--options", "runtime",
                "--sign", identity,
                "--entitlements", entitlements.absolutePath,
                appDir.absolutePath,
            ).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            val result = process.waitFor()
            println("codesign: $output")
            if (result != 0) {
                throw GradleException("codesign re-sign of ${appDir.name} failed with exit code $result:\n$output")
            }
        }
    }
}

tasks.matching { it.name == "createDistributable" }
    .configureEach { finalizedBy(copyAssetsCarDebug) }
tasks.matching { it.name == "createReleaseDistributable" }
    .configureEach { finalizedBy(copyAssetsCarRelease) }

tasks.withType(KotlinCompile::class.java) {
    dependsOn("exportLibraryDefinitions")

    compilerOptions {
        freeCompilerArgs = listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        )
    }
}
