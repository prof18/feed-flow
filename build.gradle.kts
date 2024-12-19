plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.triplet.play) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.about.libraries) apply false
    alias(libs.plugins.crashlytics) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.org.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.skie) apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory.get())
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            if (project.findProperty("composeCompilerReports") == "true") {
                freeCompilerArgs = listOf(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${layout.buildDirectory.get().asFile.absolutePath}/compose_compiler"
                )
            }
            if (project.findProperty("composeCompilerMetrics") == "true") {
                freeCompilerArgs = listOf(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${layout.buildDirectory.get().asFile.absolutePath}/compose_compiler"
                )
            }
        }
    }
}


