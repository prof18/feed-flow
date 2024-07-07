import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
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