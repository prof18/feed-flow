package com.prof18.feedflow.versionchecker

import com.prof18.feedflow.di.DI
import com.prof18.feedflow.utils.DispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.InputStream
import java.util.Properties

internal class NewVersionChecker(
    private val dispatcherProvider: DispatcherProvider,
) {

    private val newVersionMutableState: MutableStateFlow<NewVersionState> =
        MutableStateFlow(NewVersionState.NoNewVersion)

    val newVersionState = newVersionMutableState.asStateFlow()

    suspend fun notifyIfNewVersionIsAvailable() = withContext(dispatcherProvider.io) {
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
    }

    fun clearNewVersionState() {
        newVersionMutableState.update {
            NewVersionState.NoNewVersion
        }
    }
}
