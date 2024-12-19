import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.detekt)
    compileOnly(libs.detekt.formatting)
}

gradlePlugin {
    plugins {
        register("library") {
            id = "com.feedflow.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("detekt") {
            id = "com.feedflow.detekt"
            implementationClass = "DetektConventionPlugin"
        }
    }
}