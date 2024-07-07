import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.feedflow.detekt")
            }
            configure<KotlinMultiplatformExtension> {
                androidTarget {
                    compilations.all {
                        kotlinOptions {
                            jvmTarget = JavaVersion.VERSION_17.toString()
                        }
                    }
                }

                jvmToolchain(17)

                jvm()

                iosArm64()
                iosSimulatorArm64()

                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }

            configure<LibraryExtension> {
                compileSdk = version("android-compile-sdk").toInt()
                defaultConfig {
                    minSdk = version("android-min-sdk").toInt()
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }
        }
    }

    private fun Project.version(key: String): String = extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")
        .findVersion(key)
        .get()
        .requiredVersion
}
