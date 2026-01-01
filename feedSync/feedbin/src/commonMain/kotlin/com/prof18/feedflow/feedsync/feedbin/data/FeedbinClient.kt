package com.prof18.feedflow.feedsync.feedbin.data

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.DataNotFound
import com.prof18.feedflow.core.model.DataResult
import com.prof18.feedflow.core.model.NetworkFailure
import com.prof18.feedflow.core.model.Unhandled
import com.prof18.feedflow.core.model.success
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.feedsync.feedbin.data.dto.CreateSubscriptionRequest
import com.prof18.feedflow.feedsync.feedbin.data.dto.CreateTaggingRequest
import com.prof18.feedflow.feedsync.feedbin.data.dto.DeleteTagRequest
import com.prof18.feedflow.feedsync.feedbin.data.dto.EntryDTO
import com.prof18.feedflow.feedsync.feedbin.data.dto.IconDTO
import com.prof18.feedflow.feedsync.feedbin.data.dto.RenameTagRequest
import com.prof18.feedflow.feedsync.feedbin.data.dto.StarredEntriesRequest
import com.prof18.feedflow.feedsync.feedbin.data.dto.SubscriptionDTO
import com.prof18.feedflow.feedsync.feedbin.data.dto.TaggingDTO
import com.prof18.feedflow.feedsync.feedbin.data.dto.UnreadEntriesRequest
import com.prof18.feedflow.feedsync.feedbin.data.dto.UpdateSubscriptionRequest
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.networkcore.executeNetwork
import com.prof18.feedflow.feedsync.networkcore.isMissingConnectionError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.patch
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class FeedbinClient internal constructor(
    private val logger: Logger,
    private val networkSettings: NetworkSettings,
    private val appEnvironment: AppEnvironment,
    private val dispatcherProvider: DispatcherProvider,
) {

    private var httpClient: HttpClient? = null

    suspend fun login(
        username: String,
        password: String,
    ): DataResult<Unit> = withContext(dispatcherProvider.io) {
        val baseURL = "https://api.feedbin.com/"
        val client = createHttpClient(baseURL, username, password)
        httpClient = client

        return@withContext executeNetwork {
            client.get(FeedbinV2Resource.Subscriptions())
        }
    }

    suspend fun getSubscriptions(): DataResult<List<SubscriptionDTO>> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.get(FeedbinV2Resource.Subscriptions())
        }
    }

    suspend fun getIcons(): DataResult<List<IconDTO>> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.get(FeedbinV2Resource.Icons())
        }
    }

    suspend fun createSubscription(feedUrl: String): DataResult<SubscriptionDTO> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.post(FeedbinV2Resource.Subscriptions()) {
                contentType(ContentType.Application.Json)
                setBody(CreateSubscriptionRequest(feedUrl))
            }
        }
    }

    suspend fun deleteSubscription(subscriptionId: Long): DataResult<Unit> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.delete(FeedbinV2Resource.Subscriptions.ById(id = subscriptionId))
        }
    }

    suspend fun updateSubscription(
        subscriptionId: Long,
        title: String,
    ): DataResult<SubscriptionDTO> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.patch(FeedbinV2Resource.Subscriptions.ById(id = subscriptionId)) {
                contentType(ContentType.Application.Json)
                setBody(UpdateSubscriptionRequest(title))
            }
        }
    }

    suspend fun getEntries(
        page: Int? = null,
        since: String? = null,
        ids: List<Long>? = null,
        perPage: Int? = null,
        mode: String? = null,
    ): DataResult<List<EntryDTO>> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        val idsParam = ids?.joinToString(",")
        return@withContext executeNetwork {
            client.get(
                FeedbinV2Resource.Entries(
                    page = page,
                    since = since,
                    ids = idsParam,
                    mode = mode,
                    per_page = perPage,
                ),
            )
        }
    }

    suspend fun getEntriesPage(
        page: Int? = null,
        since: String? = null,
        ids: List<Long>? = null,
        perPage: Int? = null,
        mode: String? = null,
    ): DataResult<FeedbinEntriesPage> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        val idsParam = ids?.joinToString(",")

        try {
            val response = client.get(
                FeedbinV2Resource.Entries(
                    page = page,
                    since = since,
                    ids = idsParam,
                    mode = mode,
                    per_page = perPage,
                ),
            )

            val isBadToken = response
                .headers["X-Reader-Google-Bad-Token"]
                .orEmpty()
                .toBoolean()

            if (isBadToken) {
                return@withContext DataResult.Error(NetworkFailure.BadToken)
            }

            if (!response.status.isSuccess()) {
                return@withContext when (response.status) {
                    io.ktor.http.HttpStatusCode.Unauthorized -> DataResult.Error(NetworkFailure.Unauthorised)
                    io.ktor.http.HttpStatusCode.NotFound -> DataResult.Error(DataNotFound)
                    io.ktor.http.HttpStatusCode.ServiceUnavailable,
                    io.ktor.http.HttpStatusCode.InternalServerError,
                    -> DataResult.Error(NetworkFailure.ServerFailure)
                    else -> DataResult.Error(NetworkFailure.UnhandledNetworkFailure)
                }
            }

            val entries: List<EntryDTO> = response.body()

            val linkHeader = response.headers[HttpHeaders.Link]
                ?: response.headers["Links"]
                ?: response.headers["links"]
            val nextPage = parseNextPage(linkHeader)
            FeedbinEntriesPage(entries, nextPage).success()
        } catch (e: Throwable) {
            return@withContext if (e.isMissingConnectionError()) {
                DataResult.Error(NetworkFailure.NoConnection)
            } else {
                DataResult.Error(Unhandled(e))
            }
        }
    }

    suspend fun getUnreadEntries(): DataResult<List<Long>> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.get(FeedbinV2Resource.UnreadEntries())
        }
    }

    suspend fun markAsUnread(entryIds: List<Long>): DataResult<List<Long>> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.post(FeedbinV2Resource.UnreadEntries()) {
                contentType(ContentType.Application.Json)
                setBody(UnreadEntriesRequest(entryIds))
            }
        }
    }

    suspend fun markAsRead(entryIds: List<Long>): DataResult<Unit> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.delete(FeedbinV2Resource.UnreadEntries()) {
                contentType(ContentType.Application.Json)
                setBody(UnreadEntriesRequest(entryIds))
            }
        }
    }

    suspend fun getStarredEntries(): DataResult<List<Long>> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.get(FeedbinV2Resource.StarredEntries())
        }
    }

    suspend fun starEntries(entryIds: List<Long>): DataResult<List<Long>> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.post(FeedbinV2Resource.StarredEntries()) {
                contentType(ContentType.Application.Json)
                setBody(StarredEntriesRequest(entryIds))
            }
        }
    }

    suspend fun unstarEntries(entryIds: List<Long>): DataResult<Unit> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.delete(FeedbinV2Resource.StarredEntries()) {
                contentType(ContentType.Application.Json)
                setBody(StarredEntriesRequest(entryIds))
            }
        }
    }

    suspend fun getTaggings(): DataResult<List<TaggingDTO>> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.get(FeedbinV2Resource.Taggings())
        }
    }

    suspend fun createTagging(feedId: Long, name: String): DataResult<TaggingDTO> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.post(FeedbinV2Resource.Taggings()) {
                contentType(ContentType.Application.Json)
                setBody(CreateTaggingRequest(feedId, name))
            }
        }
    }

    suspend fun deleteTagging(taggingId: Long): DataResult<Unit> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.delete(FeedbinV2Resource.Taggings.ById(id = taggingId))
        }
    }

    suspend fun renameTag(oldName: String, newName: String): DataResult<List<TaggingDTO>> = withContext(
        dispatcherProvider.io,
    ) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.post(FeedbinV2Resource.Tags()) {
                contentType(ContentType.Application.Json)
                setBody(RenameTagRequest(oldName, newName))
            }
        }
    }

    suspend fun deleteTag(name: String): DataResult<List<TaggingDTO>> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork {
            client.delete(FeedbinV2Resource.Tags()) {
                contentType(ContentType.Application.Json)
                setBody(DeleteTagRequest(name))
            }
        }
    }

    private fun getOrCreateHttpClient(): HttpClient {
        val baseURL = "https://api.feedbin.com/"
        val username = networkSettings.getSyncUsername()
        val password = networkSettings.getSyncPwd()
        return httpClient ?: createHttpClient(baseURL, username, password).also {
            httpClient = it
        }
    }

    private fun createHttpClient(
        baseURL: String,
        username: String,
        password: String,
    ): HttpClient =
        HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(username = username, password = password)
                    }
                    sendWithoutRequest { true }
                }
            }
            install(Resources)
            defaultRequest {
                val url = with(baseURL) {
                    if (endsWith("/")) {
                        this
                    } else {
                        "$this/"
                    }
                }
                url(url)
            }
            if (appEnvironment.isDebug()) {
                install(Logging) {
                    level = LogLevel.ALL
                    logger = object : io.ktor.client.plugins.logging.Logger {
                        override fun log(message: String) {
                            this@FeedbinClient.logger.d { message }
                        }
                    }
                }
            }
        }

    private fun parseNextPage(linkHeader: String?): Int? {
        if (linkHeader.isNullOrBlank()) {
            return null
        }

        val nextLink = linkHeader.split(",")
            .firstOrNull { it.contains("rel=\"next\"") }
            ?: return null

        val url = nextLink.substringAfter("<").substringBefore(">")
        return try {
            io.ktor.http.Url(url).parameters["page"]?.toIntOrNull()
        } catch (_: Exception) {
            null
        }
    }
}

internal data class FeedbinEntriesPage(
    val entries: List<EntryDTO>,
    val nextPage: Int?,
)
