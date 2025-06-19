import java.util.Properties
import java.text.SimpleDateFormat
import java.util.Date

val versionProps = Properties()
val versionPropertiesFile = rootProject.file("version.properties")
if (versionPropertiesFile.exists()) {
    versionPropertiesFile.inputStream().use { versionProps.load(it) }
} else {
    throw GradleException("Root project version.properties not found! Please ensure it exists with MAJOR, MINOR, PATCH values.")
}

val appMajorVersion = versionProps.getProperty("MAJOR").toInt()
val appMinorVersion = versionProps.getProperty("MINOR").toInt()
val appPatchVersion = versionProps.getProperty("PATCH").toInt()

fun getCurrentTimestamp(): String {
    val sdf = SimpleDateFormat("yyyyMMddHHmm")
    return sdf.format(Date())
}

fun appVersionCode(): Int {
    val ciBuildNumber = System.getenv("GITHUB_RUN_NUMBER")
    return if (ciBuildNumber != null) {
        ciBuildNumber.toInt() + 6000
    } else {
        1 // Local build version code
    }
}

fun appVersionName(): String {
    return "${appMajorVersion}.${appMinorVersion}.${appPatchVersion}"
}

project.extra.set("appVersionCode", ::appVersionCode)
project.extra.set("appVersionName", ::appVersionName)
