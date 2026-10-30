import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@DisableCachingByDefault(because = "Runs XCTest on a local iOS Simulator")
abstract class SwiftCloudSyncTest @Inject constructor(
    private val execOperations: ExecOperations,
    private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {
    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:LocalState
    abstract val derivedDataDirectory: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val frameworkDirectory: DirectoryProperty

    /** An explicit xcodebuild destination; when absent, an installed iPhone simulator is picked. */
    @get:Input
    @get:Optional
    abstract val destination: Property<String>

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun runCloudSyncTests() {
        val iosDirectory = rootDirectory.dir("iosApp").get().asFile
        val output = outputDirectory.get().asFile.apply { mkdirs() }
        val resultBundle = output.resolve("CloudSyncTests.xcresult")
        fileSystemOperations.delete { delete(resultBundle) }
        execOperations.exec {
            workingDir(iosDirectory)
            commandLine("./.scripts/generate-project.sh")
        }
        val result = output.resolve("xcodebuild.log").outputStream().use { log ->
            execOperations.exec {
                workingDir(iosDirectory)
                commandLine(
                    "xcodebuild", "-quiet", "-project", "FeedFlow.xcodeproj",
                    "-scheme", "CloudSyncTests", "-configuration", "Debug",
                    "-destination", destination.orNull ?: "platform=iOS Simulator,id=${findSimulatorId()}",
                    "-derivedDataPath", derivedDataDirectory.get().asFile.absolutePath,
                    "-resultBundlePath", resultBundle.absolutePath,
                    "OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES",
                    "FRAMEWORK_SEARCH_PATHS=\"${frameworkDirectory.get().asFile.absolutePath}\"",
                    "test",
                )
                standardOutput = log
                errorOutput = log
                isIgnoreExitValue = true
            }
        }
        if (result.exitValue != 0) {
            val logFile = output.resolve("xcodebuild.log")
            // Surface the failing tests in the console; CI only exposes the log as a downloadable artifact.
            val failingTests = logFile.readLines()
                .dropWhile { !it.startsWith("Failing tests:") }
                .takeWhile { !it.startsWith("** TEST") }
                .joinToString("\n")
            throw GradleException("Swift cloud tests failed. See $logFile\n$failingTests".trimEnd())
        }
        val summaryFile = output.resolve("summary.json")
        summaryFile.outputStream().use { log ->
            execOperations.exec {
                commandLine(
                    "xcrun", "xcresulttool", "get", "test-results", "summary",
                    "--path", resultBundle.absolutePath, "--format", "json",
                )
                standardOutput = log
            }
        }
        val summary = JsonSlurper().parse(summaryFile) as Map<*, *>
        val total = (summary["totalTestCount"] as? Number)?.toInt() ?: 0
        if (
            total == 0 || summary["passedTests"] != total || summary["failedTests"] != 0 ||
            summary["skippedTests"] != 0 || summary["expectedFailures"] != 0 || summary["result"] != "Passed"
        ) {
            throw GradleException("Swift cloud test suite is incomplete or failed. See $summaryFile")
        }
        logger.lifecycle("Swift cloud adapter tests: $total passed")
    }

    // Local machines and CI images ship different simulator sets, so pick from what is installed.
    private fun findSimulatorId(): String {
        val json = ByteArrayOutputStream().also { output ->
            execOperations.exec {
                commandLine("xcrun", "simctl", "list", "devices", "available", "--json")
                standardOutput = output
            }
        }.toString(Charsets.UTF_8)
        val devicesByRuntime = (JsonSlurper().parseText(json) as Map<*, *>)["devices"] as Map<*, *>
        val iPhones = devicesByRuntime
            .filterKeys { it.toString().contains(".SimRuntime.iOS-") }
            .entries
            .sortedByDescending { runtimeVersion(it.key.toString()) }
            .map { it.value }
            .flatMap { it as List<*> }
            .map { it as Map<*, *> }
            .filter { it["name"].toString().startsWith("iPhone") }
        val device = PREFERRED_SIMULATORS.firstNotNullOfOrNull { name -> iPhones.firstOrNull { it["name"] == name } }
            ?: iPhones.firstOrNull()
            ?: throw GradleException("No iPhone simulator is installed; install one or set -Pfeedflow.swiftCloudSyncDestination")
        logger.lifecycle("Swift cloud adapter tests run on ${device["name"]}")
        return device["udid"].toString()
    }

    // "com.apple.CoreSimulator.SimRuntime.iOS-26-2" -> 2602
    private fun runtimeVersion(runtime: String): Int =
        runtime.substringAfter(".SimRuntime.iOS-").split("-")
            .take(2)
            .map { it.toIntOrNull() ?: 0 }
            .fold(0) { acc, part -> acc * 100 + part }

    private companion object {
        val PREFERRED_SIMULATORS = listOf("iPhone 18 Pro", "iPhone 17 Pro")
    }
}
