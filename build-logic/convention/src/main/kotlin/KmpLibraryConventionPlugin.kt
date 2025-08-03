import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_21)
                    }
                }

                jvmToolchain(21)

                jvm()

                iosArm64()
                iosSimulatorArm64()

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
                    sourceCompatibility = JavaVersion.VERSION_21
                    targetCompatibility = JavaVersion.VERSION_21
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
