import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_SRC_DIR_JAVA
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_SRC_DIR_KOTLIN
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_TEST_SRC_DIR_JAVA
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_TEST_SRC_DIR_KOTLIN
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("io.gitlab.arturbosch.detekt")
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            extensions.configure<DetektExtension> {
                config.setFrom("$rootDir/build-config/detekt-config.yml")
                parallel = true

                source.setFrom(
                    files(
                        "src",
                        DEFAULT_SRC_DIR_JAVA,
                        DEFAULT_TEST_SRC_DIR_JAVA,
                        DEFAULT_SRC_DIR_KOTLIN,
                        DEFAULT_TEST_SRC_DIR_KOTLIN,
                    ),
                )

                dependencies {
                    add("detektPlugins", libs.findLibrary("detekt-formatting").get())
                    add("detektPlugins", libs.findLibrary("detekt-compose-rules").get())
                }
            }
        }
    }
}
