package com.prof18.feedflow.shared.domain.articlereport

import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.core.utils.rejectUnsafeHosts
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.Uuid

class ArticleReporter(
    private val client: HttpClient,
    private val appConfig: AppConfig,
    dispatcherProvider: DispatcherProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.main)

    fun submit(url: String, source: String, onComplete: (Boolean) -> Unit) {
        scope.launch {
            val sent = try {
                client.post(ENDPOINT) {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(payload(url, source)))
                }.status.isSuccess()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                false
            }
            onComplete(sent)
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun payload(url: String, source: String) = ArticleReport(
        reportId = Uuid.random().toString(),
        url = url,
        appVersion = appConfig.appVersion,
        platform = when {
            appConfig.platformName.equals("Android", ignoreCase = true) -> "android"
            appConfig.platformName.contains("iOS", ignoreCase = true) ||
                appConfig.platformName.contains("iPadOS", ignoreCase = true) -> "ios"
            appConfig.platformName.contains("Mac", ignoreCase = true) -> "macos"
            appConfig.platformName.contains("Windows", ignoreCase = true) -> "windows"
            else -> "linux"
        },
        osVersion = appConfig.platformVersion,
        parserVersion = ArticleReportBuildInfo.PARSER_VERSION,
        contentSource = source,
        reportedAt = Clock.System.now().toString(),
    )

    companion object {
        private const val REQUEST_TIMEOUT_MILLIS = 15_000L
        private const val ENDPOINT = "https://article-reports.mgp-dev-studio.workers.dev/v1/article-reports"
        private val json = Json { encodeDefaults = true }

        fun httpClient(engine: HttpClientEngine? = null): HttpClient {
            val configure: HttpClientConfig<*>.() -> Unit = {
                followRedirects = false
                install(HttpTimeout) { requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS }
            }
            return (if (engine == null) HttpClient(configure) else HttpClient(engine, configure)).rejectUnsafeHosts()
        }
    }
}

@Serializable
private data class ArticleReport(
    val schemaVersion: Int = 1,
    val reportId: String,
    val url: String,
    val app: String = "feedflow",
    val appVersion: String,
    val platform: String,
    val osVersion: String,
    val parserVersion: String,
    val contentSource: String,
    val reportedAt: String,
)
