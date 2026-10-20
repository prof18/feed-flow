import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hot.reload)
    alias(libs.plugins.about.libraries)
    alias(libs.plugins.feedflow.detekt)
    alias(libs.plugins.feedflow.versioning)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

@Suppress("UNCHECKED_CAST")
val appVersionName = extra["appVersionName"] as () -> String
@Suppress("UNCHECKED_CAST")
val appVersionCode = extra["appVersionCode"] as () -> Int

val generateDesktopBuildInfo by tasks.registering {
    val versionName = appVersionName()
    inputs.property("versionName", versionName)
    val outputDirectory = layout.buildDirectory.dir("generated/desktop-build-info/kotlin")
    outputs.dir(outputDirectory)
    doLast {
        val output = outputDirectory.get().file(
            "com/prof18/feedflow/desktop/DesktopBuildInfo.kt",
        ).asFile
        output.parentFile.mkdirs()
        output.writeText(
            """
            package com.prof18.feedflow.desktop

            internal object DesktopBuildInfo {
                const val VERSION = "$versionName"
            }
            """.trimIndent() + "\n",
        )
    }
}

group = "com.prof18"

val isMacOS = System.getProperty("os.name").lowercase().contains("mac")
val isLinux = System.getProperty("os.name").lowercase().contains("linux")
val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

aboutLibraries {
    collect {
        configPath.set(file("config/aboutlibraries"))
    }
    export {
        outputFile.set(file("src/jvmMain/resources/aboutlibraries.json"))
    }
}

dependencies {
    components {
        withModule("de.jangassen:jfa") {
            allVariants {
                withDependencies {
                    // jfa requests an obsolete jpms classifier; Jewel supplies the regular JNA artifact.
                    removeAll { it.group == "net.java.dev.jna" && it.name == "jna" }
                }
            }
        }
    }
    modules {
        module("org.jetbrains.intellij.deps.kotlinx:kotlinx-coroutines-core-jvm") {
            replacedBy("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm", "Use the upstream coroutine runtime")
        }
    }
}

java {
    toolchain {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    jvmToolchain {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(25)
    }

    jvm()

    sourceSets {
        jvmMain {
            kotlin.srcDir(generateDesktopBuildInfo)
            dependencies {
                implementation(libs.jewel.int.ui.standalone)
                implementation(libs.jewel.platform.icons)
                implementation(libs.jewel.markdown.core)
                implementation(libs.jewel.markdown.standalone.styling)
                implementation(libs.jewel.markdown.images)
                implementation(libs.jewel.markdown.tables)
                implementation(libs.jewel.markdown.strikethrough)
                implementation(libs.jewel.markdown.autolink)
                implementation(libs.saket.swipe)
                implementation(libs.about.libraries.core)
                implementation(project(":i18n"))
                implementation(libs.lyricist)
                implementation(libs.io.coil.compose)
                implementation(libs.io.coil.network)
                implementation(libs.jsystem.theme.detector)
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs) {
                    exclude(group = "org.jetbrains.compose.material", module = "material")
                }
                implementation(libs.compose.multiplatform.ui.tooling.preview)
                implementation(libs.compose.multiplatform.material.icons.extended)
                implementation(libs.compose.multiplatform.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.slf4j.nop)

                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.koin.composeVM)

                implementation(libs.jetbrains.navigation3.ui)
                implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.date.time)
                implementation(libs.flatlaf)
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(compose.desktop.uiTestJUnit4)
                implementation(compose.desktop.currentOs) {
                    exclude(group = "org.jetbrains.compose.material", module = "material")
                }
                implementation(libs.spectre.core)
                implementation(libs.spectre.recording)
                implementation(libs.spectre.testing)
                when {
                    isMacOS -> runtimeOnly(libs.spectre.recording.macos)
                    isLinux -> runtimeOnly(libs.spectre.recording.linux)
                    isWindows -> runtimeOnly(libs.spectre.recording.windows)
                }
                // Spectre's shared rule lifecycle references the Jupiter API even under JUnit 4.
                implementation(libs.junit.jupiter.api)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
        vendor = JvmVendorSpec.JETBRAINS
    })
    if (isLinux) {
        // End each native worker before surfaces from multiple classes accumulate.
        forkEvery = 1
        maxParallelForks = 1
        systemProperty("skiko.renderApi", "SOFTWARE")
    }
}

val isAppStoreRelease = project.property("macOsAppStoreRelease").toString().toBoolean()
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

            javaHome = javaToolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(25)
                vendor = JvmVendorSpec.JETBRAINS
            }.get().metadata.installationPath.asFile.absolutePath

            // Enable only to test translations
//            jvmArgs.add("-Duser.language=zh")
//            jvmArgs.add("-Duser.country=CN")

            buildTypes.release.proguard {
                isEnabled = false
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

                val distributionFormats = mutableListOf(
                    TargetFormat.Dmg,
                    TargetFormat.Pkg,
                    TargetFormat.Msi,
                    TargetFormat.Exe,
                    TargetFormat.Deb,
                    TargetFormat.Rpm,
                )
                if (isLinux) {
                    distributionFormats += TargetFormat.AppImage
                }
                targetFormats(*distributionFormats.toTypedArray())
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
                    menuGroup = "Network;News;"
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

if (isLinux) {
    tasks.withType<AbstractJPackageTask>().matching {
        it.name == "packageRpm" || it.name == "packageReleaseRpm"
    }.configureEach {
        freeArgs.addAll(
            "--linux-package-deps",
            listOf(
                "libX11.so.6()(64bit)",
                "libXext.so.6()(64bit)",
                "libXi.so.6()(64bit)",
                "libXrender.so.1()(64bit)",
                "libXtst.so.6()(64bit)",
                "libEGL.so.1()(64bit)",
                "libGL.so.1()(64bit)",
                "libfontconfig.so.1()(64bit)",
                "libfreetype.so.6()(64bit)",
                "libstdc++.so.6()(64bit)",
            ).joinToString(","),
        )
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
val copyAssetsCarDebug = tasks.register("copyAssetsCarDebug") {
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

val copyAssetsCarRelease = tasks.register("copyAssetsCarRelease") {
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
}

tasks.named("jvmProcessResources") {
    dependsOn("exportLibraryDefinitions")
}
