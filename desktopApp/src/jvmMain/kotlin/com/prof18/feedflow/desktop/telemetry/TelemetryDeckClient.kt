package com.prof18.feedflow.desktop.telemetry

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopOS
import com.prof18.feedflow.core.utils.getDesktopOS
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale
import java.util.Properties
import java.util.UUID

class TelemetryDeckClient(
    private val httpClient: HttpClient,
    private val appEnvironment: AppEnvironment,
    private val logger: Logger,
) {
    private var userIdManager: UserIdManager? = null
    private val sessionId = UUID.randomUUID().toString()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val appId: String = "0334762E-7A84-4A80-A1BA-879165ED0333"
    private var appVersion: String?
    private var isFlatpack: Boolean? = false

    init {
        val properties = Properties()
        val propsFile = TelemetryDeckClient::class.java.classLoader?.getResourceAsStream("props.properties")
            ?: InputStream.nullInputStream()
        properties.load(propsFile)

        appVersion = properties["version"]?.toString()
        isFlatpack = properties["flatpak"]?.toString()?.toBooleanStrictOrNull()
        val saltFromProps = properties["tdeck_salt"]?.toString()

        val salt = saltFromProps ?: ""
        userIdManager = UserIdManager(
            salt = salt,
            appEnv = appEnvironment,
        )
    }

    fun signal(type: String, parameters: Map<String, String> = emptyMap()) {
        if (appEnvironment.isRelease()) {
            scope.launch {
                try {
                    sendSignal(type, parameters)
                } catch (e: Exception) {
                    logger.e(e) { "Error while sending signal" }
                }
            }
        }
    }

    private suspend fun sendSignal(type: String, parameters: Map<String, String>) {
        val id = appId
        val userId = userIdManager?.getHashedUserId()
        if (userId.isNullOrEmpty()) {
            logger.w { "TelemetryDeck: appId or userId is not set, skipping signal." }
            return
        }
        val signal = TelemetrySignal(
            appID = id,
            clientUser = userId,
            type = type,
            sessionID = sessionId,
            isTestMode = false,
            payload = getSystemInfo() + parameters,
        )

        val json = Json.encodeToString(listOf(signal))

        httpClient.post("https://nom.telemetrydeck.com/v2/") {
            contentType(ContentType.Application.Json)
            setBody(json)
        }
    }

    private fun getSystemInfo(): Map<String, String> {
        val systemVersion = "${getOperatingSystem()} ${System.getProperty("os.version")}"
        return mutableMapOf(
            "TelemetryDeck.AppInfo.version" to (appVersion ?: "unknown"),
            "TelemetryDeck.Device.operatingSystem" to getOperatingSystem(),
            "TelemetryDeck.Device.architecture" to System.getProperty("os.arch"),
            "TelemetryDeck.Device.systemMajorMinorVersion" to systemVersion,
            "TelemetryDeck.RunContext.locale" to Locale.getDefault().toString(),
            "TelemetryDeck.RunContext.language" to System.getProperty("user.language"),
            "TelemetryDeck.Device.timeZone" to TimeZone.currentSystemDefault().toString(),
            "TelemetryDeck.SDK.name" to "FeedFlow-Desktop-API",
        ).apply {
            getModelName()?.let { modelName ->
                set("TelemetryDeck.Device.modelName", modelName)
            }
            isAppStore()?.let { isAppStore ->
                set("TelemetryDeck.RunContext.isAppStore", isAppStore.toString())
            }
            isFlatpack()?.let { isFlatpack ->
                set("TelemetryDeck.RunContext.isFlatpack", isFlatpack.toString())
            }
        }
    }

    private fun getOperatingSystem(): String = when (getDesktopOS()) {
        DesktopOS.MAC -> "macOS"
        DesktopOS.WINDOWS -> "windows"
        DesktopOS.LINUX -> "linux"
    }

    private fun isAppStore(): Boolean? {
        return when (getDesktopOS()) {
            DesktopOS.MAC -> System.getenv("APP_SANDBOX_CONTAINER_ID")?.isNotEmpty()
            DesktopOS.WINDOWS -> null
            DesktopOS.LINUX -> null
        }
    }

    private fun isFlatpack(): Boolean? {
        return when (getDesktopOS()) {
            DesktopOS.MAC -> null
            DesktopOS.WINDOWS -> null
            DesktopOS.LINUX -> isFlatpack
        }
    }

    private fun getModelName(): String? =
        when (getDesktopOS()) {
            DesktopOS.WINDOWS -> try {
                val process = ProcessBuilder(
                    "wmic",
                    "computersystem",
                    "get",
                    "model",
                    "/format:value",
                ).redirectErrorStream(true).start()

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.use { it.readText() }

                // Parse the output for Model=value
                output.lines()
                    .firstOrNull { it.startsWith("Model=") }
                    ?.substringAfter("Model=")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
            } catch (_: Exception) {
                null
            }

            DesktopOS.MAC -> {
                try {
                    val process = ProcessBuilder("sysctl", "-n", "hw.model")
                        .redirectErrorStream(true)
                        .start()

                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    reader.use { it.readLine()?.trim() }
                } catch (_: Exception) {
                    null
                }
            }

            DesktopOS.LINUX -> try {
                val process = ProcessBuilder("cat", "/sys/class/dmi/id/product_name")
                    .redirectErrorStream(true)
                    .start()

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                reader.use { it.readLine()?.trim() }
            } catch (_: Exception) {
                null
            }
        }
}
