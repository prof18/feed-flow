plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    macosArm64("ikloud") {
        compilations.getByName("main") {
            cinterops {
                val jni by creating {
                    val javaHome = File(System.getProperty("java.home"))
                    packageName = "com.prof18.jni"
                    includeDirs(
                        Callable { File(javaHome, "include") },
                        Callable { File(javaHome, "include/darwin") },
                    )
                }
            }
        }

        binaries {
            sharedLib {
                baseName = "ikloud"
            }
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.BetaInteropApi")
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
            languageSettings.optIn("kotlin.experimental.ExperimentalNativeApi")
        }
    }
}

tasks.register<BuildAndCopyIkloudMacos>("buildAndCopyIkloudMacOS") {
    dependsOn(":feedSync:ikloud-macos:linkReleaseSharedIkloud")

    val projectDir = rootProject.layout.projectDirectory
    source = layout.buildDirectory.file("bin/ikloud/releaseShared/libikloud.dylib")
    destination = projectDir.dir("desktopApp/resources/macos-arm64")
    destinationSandbox = projectDir.dir("desktopApp/resources-sandbox/macos-arm64")
}

abstract class BuildAndCopyIkloudMacos : DefaultTask() {

    @get:InputFile abstract val source: RegularFileProperty

    @get:OutputDirectory abstract val destinationSandbox: DirectoryProperty

    @get:OutputDirectory abstract val destination: DirectoryProperty

    @get:Inject abstract val fs: FileSystemOperations

    @TaskAction
    fun action() {
        fs.copy {
            from(source)
            into(destination)
        }
        fs.copy {
            from(source)
            into(destinationSandbox)
        }
    }
}
