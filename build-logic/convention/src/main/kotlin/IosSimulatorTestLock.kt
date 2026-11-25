import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

/**
 * Lets one iOS simulator test task run at a time across all projects: the Kotlin/Native simulator
 * tests and the Swift cloud sync xcodebuild run. On the 3-core CI macOS runner, overlapping runs
 * stalled Kotlin/Native tests past their runTest and Turbine timeouts.
 */
abstract class IosSimulatorTestLock : BuildService<BuildServiceParameters.None>

internal fun Project.iosSimulatorTestLock(): Provider<IosSimulatorTestLock> =
    gradle.sharedServices.registerIfAbsent("iosSimulatorTestLock", IosSimulatorTestLock::class) {
        maxParallelUsages.set(1)
    }

internal fun Project.serializeKotlinNativeSimulatorTests() {
    val lock = iosSimulatorTestLock()
    tasks.withType<KotlinNativeSimulatorTest>().configureEach { usesService(lock) }
}
