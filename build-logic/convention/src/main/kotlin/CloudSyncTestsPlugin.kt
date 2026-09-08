import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests
import org.jetbrains.kotlin.gradle.targets.native.DefaultSimulatorTestRun

class CloudSyncTestsPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        // Ordinary tests remain independently runnable without artifact setup.
        tasks.withType<AbstractTestTask>().configureEach {
            if (!name.contains("cloudArtifact", ignoreCase = true)) {
                filter.excludeTestsMatching("*.cloudsync.*CloudArtifactExchangeTest")
            }
        }
        afterEvaluate {
            val appleHost = System.getProperty("os.name").contains("Mac") &&
                System.getProperty("os.arch") in listOf("aarch64", "arm64")
            val platforms = if (appleHost) listOf("desktop", "android", "ios") else listOf("desktop", "android")
            val artifacts = layout.buildDirectory.dir("cloud-sync-artifacts")
            val producers = platforms.associateWith { platform ->
                artifactTask(platform, "producer", platforms, artifacts)
            }
            val consumers = platforms.map { platform ->
                artifactTask(platform, "consumer", platforms, artifacts).apply {
                    configure { dependsOn(producers.values) }
                }
            }
            tasks.named("allTests") {
                dependsOn(producers.values, consumers)
            }
            if (appleHost) {
                val swiftTests = tasks.register<SwiftCloudSyncTest>("swiftCloudSyncTest") {
                    group = "verification"
                    description = "Runs the isolated Swift cloud adapter tests on the iOS simulator."
                    dependsOn("linkDebugFrameworkIosSimulatorArm64")
                    rootDirectory.set(rootProject.layout.projectDirectory)
                    frameworkDirectory.set(layout.buildDirectory.dir("bin/iosSimulatorArm64/debugFramework"))
                    derivedDataDirectory.set(layout.buildDirectory.dir("swift-cloud-sync-derived-data"))
                    outputDirectory.set(layout.buildDirectory.dir("reports/tests/swiftCloudSyncTest"))
                }
                tasks.named("allTests") { dependsOn(swiftTests) }
            }
        }
    }

    private fun Project.artifactTask(
        platform: String,
        role: String,
        platforms: List<String>,
        artifacts: Provider<Directory>,
    ): TaskProvider<out AbstractTestTask> {
        val capitalPlatform = platform.replaceFirstChar(Char::uppercaseChar)
        val capitalRole = role.replaceFirstChar(Char::uppercaseChar)
        val artifactDirectory = if (role == "producer") artifacts.map { it.dir(platform) } else artifacts
        val environment = mapOf(
            "FEEDFLOW_CLOUD_ARTIFACT_DIR" to artifactDirectory.get().asFile.absolutePath,
            "FEEDFLOW_CLOUD_ARTIFACT_ROLE" to role,
            "FEEDFLOW_CLOUD_ARTIFACT_RUN_ID" to platform,
            "FEEDFLOW_CLOUD_ARTIFACT_PLATFORM" to platform,
            "FEEDFLOW_CLOUD_ARTIFACT_PRODUCERS" to platforms.joinToString(","),
        )
        val testClass = "com.prof18.feedflow.shared.test.cloudsync.${capitalPlatform}CloudArtifactExchangeTest"
        val execution: TaskProvider<out AbstractTestTask> = if (platform == "ios") {
            val target = extensions.getByType<KotlinMultiplatformExtension>().targets
                .getByName("iosSimulatorArm64") as KotlinNativeTargetWithSimulatorTests
            val run = target.testRuns.create("cloudArtifact$capitalRole") as DefaultSimulatorTestRun
            run.executionTask.apply {
                configure {
                    environment.forEach { (key, value) ->
                        environment(key, value)
                        environment("SIMCTL_CHILD_$key", value)
                    }
                }
            }
        } else {
            val base = tasks.named<Test>(if (platform == "desktop") "jvmTest" else "testAndroidHostTest").get()
            tasks.register<Test>("cloudArtifact$capitalPlatform$capitalRole") {
                testClassesDirs = base.testClassesDirs
                classpath = base.classpath
                javaLauncher.set(base.javaLauncher)
                jvmArgs = base.jvmArgs
                jvmArgumentProviders.addAll(base.jvmArgumentProviders)
                systemProperties = base.systemProperties
                environment(base.environment)
                environment(environment)
                workingDir = base.workingDir
            }
        }
        execution.configure {
            group = "verification"
            description = "Runs the $platform cloud snapshot $role tests."
            filter.includeTestsMatching(testClass)
            filter.isFailOnNoMatchingTests = true
            // A cached pass must never replace an actual cross-runtime file handoff.
            outputs.upToDateWhen { false }
            outputs.doNotCacheIf("Exchanges live fixture files between platform test tasks") { true }
            if (role == "producer") {
                outputs.dir(artifactDirectory)
                doFirst {
                    val directory = artifactDirectory.get().asFile
                    check(!directory.exists() || directory.deleteRecursively()) {
                        "Cannot clear cloud artifact directory: $directory"
                    }
                    check(directory.mkdirs()) { "Cannot create cloud artifact directory: $directory" }
                }
            } else {
                platforms.filter { it != platform }.forEach { producer ->
                    inputs.dir(artifacts.map { it.dir(producer) })
                }
            }
        }
        return execution
    }
}
