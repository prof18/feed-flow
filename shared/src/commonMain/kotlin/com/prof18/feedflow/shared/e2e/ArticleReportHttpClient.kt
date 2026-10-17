package com.prof18.feedflow.shared.e2e

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.shared.domain.articlereport.ArticleReporter
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlin.time.Duration.Companion.seconds

private const val RESPONSE_KEY = "article-report-debug-transport-v1"

internal fun selectArticleReportResponse(settings: Settings, mode: String?) {
    require(mode == null || mode in setOf("accepted", "offline", "terminal", "rate-limit", "slow"))
    if (mode == null) settings.remove(RESPONSE_KEY) else settings.putString(RESPONSE_KEY, mode)
}

internal fun articleReportHttpClient(
    environment: AppEnvironment,
    settings: Settings,
    live: HttpClient = ArticleReporter.httpClient(),
): HttpClient {
    if (!environment.isDebug()) return live
    val client = ArticleReporter.httpClient(
        MockEngine { request ->
            when (settings.getStringOrNull(RESPONSE_KEY)) {
                "accepted" -> respond("")
                "offline" -> error("Simulated offline report")
                "terminal" -> respond("", HttpStatusCode.BadRequest)
                "rate-limit" -> respond("", HttpStatusCode.TooManyRequests)
                "slow" -> {
                    delay(2.seconds)
                    respond("")
                }
                else -> {
                    val response = live.request(request.url) {
                        method = request.method
                        headers.appendAll(request.headers)
                        setBody(request.body)
                    }
                    respond(response.bodyAsText(), response.status, response.headers)
                }
            }
        },
    )
    client.coroutineContext.job.invokeOnCompletion { live.close() }
    return client
}
