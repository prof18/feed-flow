import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("dev.detekt")
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            extensions.configure<DetektExtension> {
                config.setFrom("$rootDir/config/detekt/detekt.yml")
                buildUponDefaultConfig.set(true)
                parallel.set(true)
                // -Pfeedflow.detektDebug=true prints the compiler errors behind "compiler errors found during analysis".
                debug.set(providers.gradleProperty("feedflow.detektDebug").map(String::toBoolean).orElse(false))

                source.setFrom(
                    files(
                        "src",
                        "src/main/java",
                        "src/test/java",
                        "src/main/kotlin",
                        "src/test/kotlin",
                    ),
                )
            }

            // Type-resolved tasks analyze compilation sources, which include generated code.
            val buildDirectory = layout.buildDirectory.get().asFile
            tasks.withType<Detekt>().configureEach {
                exclude { it.file.startsWith(buildDirectory) }
            }

            // `detekt` alone skips rules that need type resolution (e.g. most new compose rules).
            tasks.named("detekt") {
                dependsOn(tasks.matching { it.name in TYPE_RESOLVED_TASKS })
            }

            dependencies {
                add("detektPlugins", libs.findLibrary("detekt-ktlint-wrapper").get())
                add("detektPlugins", libs.findLibrary("detekt-compose-rules").get())
            }
        }
    }

    private companion object {
        // JVM compilation of KMP modules (commonMain + jvmMain) and both Android app flavors.
        val TYPE_RESOLVED_TASKS = setOf("detektMainJvm", "detektGooglePlayDebug", "detektFdroidDebug")
    }
}
