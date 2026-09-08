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
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
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

    @get:Input
    abstract val destination: Property<String>

    init {
        destination.convention("platform=iOS Simulator,name=iPhone 17 Pro")
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
                    "-destination", destination.get(),
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
            throw GradleException("Swift cloud tests failed. See ${output.resolve("xcodebuild.log")}")
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
}
