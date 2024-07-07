plugins {
    alias(libs.plugins.feedflow.library)
    alias(libs.plugins.ksp)
}

kotlin {
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

android {
    namespace = "com.prof18.feedflow.i18n"
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

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().all {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}
