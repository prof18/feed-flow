import com.google.devtools.ksp.gradle.KspAATask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.feedflow.library)
    alias(libs.plugins.ksp)
}

kotlin {
    android {
        namespace = "com.prof18.feedflow.i18n"
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonJvmAndroidMain = create("commonJvmAndroidMain") {
            dependsOn(commonMain.get())
        }

        androidMain {
            dependsOn(commonJvmAndroidMain)
        }

        jvmMain {
            dependsOn(commonJvmAndroidMain)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.lyricist.processorXml)
}

ksp {
    arg("lyricist.xml.resourcesPath", "$projectDir/src/commonMain/resources/locale")
    arg("lyricist.packageName", "com.prof18.feedflow.i18n")
    arg("lyricist.xml.moduleName", "FeedFlow")
    arg("lyricist.xml.defaultLanguageTag", "en")
    arg("lyricist.xml.generateComposeAccessors", "false")
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}

// The platform KSP tasks read commonMain, which includes the generated Lyricist sources below.
tasks.withType<KspAATask>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    } else {
        // Lyricist reads the XML through a processor argument, which Gradle does not track: without
        // this input, an added or removed string can restore stale generated code from the build cache.
        inputs.dir("src/commonMain/resources/locale")
            .withPropertyName("lyricistStrings")
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}
