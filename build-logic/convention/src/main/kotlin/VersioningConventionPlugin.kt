import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Properties

class VersioningConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val versionProps = Properties()
        val versionPropertiesFile = target.rootProject.file("version.properties")
        if (versionPropertiesFile.exists()) {
            versionPropertiesFile.inputStream().use { versionProps.load(it) }
        } else {
            error(
                "Root project version.properties not found! " +
                    "Please ensure it exists with MAJOR, MINOR, PATCH values.",
            )
        }

        val appMajorVersion = versionProps.getProperty("MAJOR").toInt()
        val appMinorVersion = versionProps.getProperty("MINOR").toInt()
        val appPatchVersion = versionProps.getProperty("PATCH").toInt()

        fun appVersionCode(): Int {
            val ciBuildNumber = System.getenv("GITHUB_RUN_NUMBER")
            return if (ciBuildNumber != null) {
                ciBuildNumber.toInt() + 6000
            } else {
                1
            }
        }

        fun appVersionName(): String = "$appMajorVersion.$appMinorVersion.$appPatchVersion"

        target.extensions.extraProperties.set("appVersionCode", ::appVersionCode)
        target.extensions.extraProperties.set("appVersionName", ::appVersionName)
    }
}
