import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_SRC_DIR_JAVA
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_SRC_DIR_KOTLIN
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_TEST_SRC_DIR_JAVA
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_TEST_SRC_DIR_KOTLIN

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.triplet.play) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.native.coroutines) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.about.libraries) apply false
    alias(libs.plugins.crashlytics) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.versionsBenManes)
    alias(libs.plugins.org.jetbrains.kotlin.jvm) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

allprojects {
    apply {
        plugin(rootProject.libs.plugins.detekt.get().pluginId)
    }

    dependencies {
        detektPlugins(rootProject.libs.io.gitlab.arturbosch.detekt.formatting) {
            exclude(group = "org.slf4j", module = "slf4j-nop")
        }
    }

    detekt {
        source.setFrom(
            files(
                "src",
                DEFAULT_SRC_DIR_JAVA,
                DEFAULT_TEST_SRC_DIR_JAVA,
                DEFAULT_SRC_DIR_KOTLIN,
                DEFAULT_TEST_SRC_DIR_KOTLIN,
            ),
        )
        toolVersion = rootProject.libs.versions.detekt.get()
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        parallel = true
        autoCorrect = true
    }
}
