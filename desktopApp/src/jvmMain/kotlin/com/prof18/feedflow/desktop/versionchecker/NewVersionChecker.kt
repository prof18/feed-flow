package com.prof18.feedflow.desktop.versionchecker

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.DesktopOS
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.desktop.di.DI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.InputStream
import java.util.Properties

internal class NewVersionChecker(
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
) {

    private val newVersionMutableState: MutableStateFlow<NewVersionState> =
        MutableStateFlow(NewVersionState.NoNewVersion)

    val newVersionState = newVersionMutableState.asStateFlow()

    suspend fun notifyIfNewVersionIsAvailable() = withContext(dispatcherProvider.io) {
        val isSandboxed = System.getenv("APP_SANDBOX_CONTAINER_ID") != null
        val isNotMacOs = getDesktopOS()
        // TODO: Add support for Windows and Linux
        if (isSandboxed || isNotMacOs != DesktopOS.MAC) {
            return@withContext
        }
        try {
            val doc = Jsoup.connect("https://github.com/prof18/feed-flow/releases/latest").get()

            val content = doc.text()
            val regex = "Release (\\d+\\.\\d+\\.\\d+-\\w+)".toRegex()

            val newVersionString = regex.find(content)?.value
                ?.replace("-desktop", "")
                ?.replace("Release ", "")

            val newVersion = newVersionString
                ?.replace(".", "")
                ?.toIntOrNull()
                ?: return@withContext

            val properties = Properties()
            val propsFile = DI::class.java.classLoader?.getResourceAsStream("props.properties")
                ?: InputStream.nullInputStream()
            properties.load(propsFile)

            val version = properties["version"]
                ?.toString()
                ?.replace(".", "")
                ?.toIntOrNull()
                ?: return@withContext

            val versionLink = """
                https://github.com/prof18/feed-flow/releases/download/$newVersionString-desktop/FeedFlow-$newVersionString.dmg
            """.trimIndent()

            newVersionMutableState.update {
                if (newVersion > version) {
                    NewVersionState.NewVersion(
                        downloadLink = versionLink,
                    )
                } else {
                    NewVersionState.NoNewVersion
                }
            }
        } catch (e: Exception) {
            // Do nothing
            logger.e(e) { "Error while trying to notify if a new version is available" }
        }
    }

    fun clearNewVersionState() {
        newVersionMutableState.update {
            NewVersionState.NoNewVersion
        }
    }
}
