import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.feedflow.library)
    alias(libs.plugins.ksp)
}

kotlin {
    androidLibrary {
        namespace = "com.prof18.feedflow.i18n"
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonJvmAndroidMain by creating {
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

tasks.withType<KotlinCompilationTask<*>>().all {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}
