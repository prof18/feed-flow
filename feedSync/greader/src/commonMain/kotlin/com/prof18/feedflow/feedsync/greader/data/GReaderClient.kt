package com.prof18.feedflow.feedsync.greader.data

import co.touchlab.kermit.Logger
import co.touchlab.stately.concurrency.AtomicReference
import com.prof18.feedflow.core.model.BazquxLoginFailure
import com.prof18.feedflow.core.model.DataNotFound
import com.prof18.feedflow.core.model.DataResult
import com.prof18.feedflow.core.model.NetworkFailure
import com.prof18.feedflow.core.model.Unhandled
import com.prof18.feedflow.core.model.isError
import com.prof18.feedflow.core.model.onSuccess
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.core.utils.FEEDFLOW_USER_AGENT
import com.prof18.feedflow.feedsync.greader.data.dto.StreamItemIdDTO
import com.prof18.feedflow.feedsync.greader.data.dto.StreamItemsContentsDTO
import com.prof18.feedflow.feedsync.greader.data.dto.SubscriptionListDTO
import com.prof18.feedflow.feedsync.greader.data.dto.SubscriptionQuickAddResult
import com.prof18.feedflow.feedsync.greader.domain.Stream
import com.prof18.feedflow.feedsync.greader.domain.SubscriptionEditAction
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.networkcore.executeNetwork
import com.prof18.feedflow.feedsync.networkcore.isMissingConnectionError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class GReaderClient internal constructor(
    private val logger: Logger,
    private val networkSettings: NetworkSettings,
    private val appEnvironment: AppEnvironment,
    private val dispatcherProvider: DispatcherProvider,
) {

    private var httpClient: HttpClient? = null
    private var postToken = AtomicReference<String?>(null)

    suspend fun login(
        username: String,
        password: String,
        baseURL: String,
    ): DataResult<String> = withContext(dispatcherProvider.io) {
        val client = createHttpClient(baseURL)
        httpClient = client
        val loginRes = AccountsRes.ClientLogin(
            Email = username,
            Passwd = password,
        )
        return@withContext executeLogin {
            client.post(loginRes)
        }
    }

    suspend fun getFeedSourcesAndCategories(): DataResult<SubscriptionListDTO> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork<SubscriptionListDTO> {
            client.get(ReaderResource.Api.Zero.Subscription.List())
        }
    }

    suspend fun getStreamItemsIDs(
        stream: Stream,
        since: Long? = null,
        continuation: String? = null,
        count: Int = 15_000,
        excludedStream: Stream? = null,
    ): DataResult<StreamItemIdDTO> = withContext(dispatcherProvider.io) {
        val client = getOrCreateHttpClient()
        return@withContext executeNetwork<StreamItemIdDTO> {
            client.get(
                ReaderResource.Api.Zero.StreamRes.Items.IDs(
                    s = stream.id,
                    ot = since,
                    c = continuation,
                    n = count,
                    xt = excludedStream?.id,
                ),
            )
        }
    }

    suspend fun editTag(
        itemIds: List<String>,
        addTag: Stream? = null,
        removeTag: Stream? = null,
    ): DataResult<Unit> = withContext(dispatcherProvider.io) {
        withPostToken {
            executeNetwork {
                getOrCreateHttpClient()
                    .post(ReaderResource.Api.Zero.EditTag()) {
                        contentType(ContentType.Application.FormUrlEncoded)
                        val formData = Parameters.build {
                            appendAll("i", itemIds)
                            postToken.get()?.let { token ->
                                append("T", token)
                            }
                            addTag?.let { append("a", it.id) }
                            removeTag?.let { append("r", it.id) }
                        }
                        setBody(FormDataContent(formData))
                    }
            }
        }
    }

    suspend fun renameTag(
        old: String,
        new: String,
    ): DataResult<Unit> = withContext(dispatcherProvider.io) {
        withPostToken {
            executeNetwork {
                getOrCreateHttpClient()
                    .post(ReaderResource.Api.Zero.RenameTag()) {
                        contentType(ContentType.Application.FormUrlEncoded)
                        val formData = Parameters.build {
                            append("s", old)
                            append("dest", new)
                            postToken.get()?.let { token ->
                                append("T", token)
                            }
                        }
                        setBody(FormDataContent(formData))
                    }
            }
        }
    }

    suspend fun disableTag(
        tagId: String,
    ): DataResult<Unit> = withContext(dispatcherProvider.io) {
        withPostToken {
            executeNetwork {
                getOrCreateHttpClient()
                    .post(ReaderResource.Api.Zero.EditTag()) {
                        contentType(ContentType.Application.FormUrlEncoded)
                        val formData = Parameters.build {
                            append("s", tagId)
                            postToken.get()?.let { token ->
                                append("T", token)
                            }
                        }
                        setBody(FormDataContent(formData))
                    }
            }
        }
    }

    suspend fun editSubscription(
        feedSourceId: String,
        editAction: SubscriptionEditAction,
        title: String? = null,
        addCategoryId: String? = null,
        removeCategoryId: String? = null,
    ): DataResult<Unit> = withContext(dispatcherProvider.io) {
        withPostToken {
            executeNetwork {
                getOrCreateHttpClient()
                    .post(ReaderResource.Api.Zero.Subscription.Edit()) {
                        contentType(ContentType.Application.FormUrlEncoded)
                        val formData = Parameters.build {
                            append("s", feedSourceId)
                            append("ac", editAction.id)
                            removeCategoryId?.let { append("r", it) }
                            title?.let { append("t", it) }
                            addCategoryId?.let { append("a", it) }
                            postToken.get()?.let { token ->
                                append("T", token)
                            }
                        }
                        setBody(FormDataContent(formData))
                    }
            }
        }
    }

    suspend fun addSubscription(
        url: String,
    ): DataResult<SubscriptionQuickAddResult> = withContext(dispatcherProvider.io) {
        withPostToken {
            executeNetwork {
                getOrCreateHttpClient()
                    .post(ReaderResource.Api.Zero.Subscription.QuickAdd()) {
                        contentType(ContentType.Application.FormUrlEncoded)
                        val formData = Parameters.build {
                            append("quickadd", url)
                            postToken.get()?.let { token ->
                                append("T", token)
                            }
                        }
                        setBody(FormDataContent(formData))
                    }
            }
        }
    }

    suspend fun getItems(
        excludeTargets: List<String>?,
        max: Int,
        lastModified: Long?,
        continuation: String?,
    ): DataResult<StreamItemsContentsDTO> = withContext(dispatcherProvider.io) {
        executeNetwork {
            getOrCreateHttpClient()
                .get(
                    ReaderResource.Api.Zero.StreamRes.Contents.ReadingList(
                        xt = excludeTargets,
                        n = max,
                        ot = lastModified,
                        c = continuation,
                    ),
                )
        }
    }

    suspend fun getStarredItemsContent(
        continuation: String?,
        maxNumber: Int,
        since: Long?,
    ): DataResult<StreamItemsContentsDTO> = withContext(dispatcherProvider.io) {
        executeNetwork {
            getOrCreateHttpClient()
                .get(
                    ReaderResource.Api.Zero.StreamRes.Contents.Starred(
                        n = maxNumber,
                        ot = since,
                        c = continuation,
                    ),
                )
        }
    }

    suspend fun getItemContents(
        itemIds: List<String>,
    ): DataResult<StreamItemsContentsDTO> = withContext(dispatcherProvider.io) {
        withPostToken {
            executeNetwork {
                getOrCreateHttpClient()
                    .post(ReaderResource.Api.Zero.StreamRes.Items.Contents()) {
                        contentType(ContentType.Application.FormUrlEncoded)
                        val formData = Parameters.build {
                            appendAll("i", itemIds)
                            postToken.get()?.let { token ->
                                append("T", token)
                            }
                        }
                        setBody(FormDataContent(formData))
                    }
            }
        }
    }

    private suspend fun <T> withPostToken(block: suspend () -> DataResult<T>): DataResult<T> {
        if (postToken.get() == null) {
            fetchToken()
        }

        val result = block()

        if (result.isError() && result.failure is NetworkFailure.BadToken) {
            fetchToken()
            return block()
        }

        return result
    }

    private suspend fun fetchToken() {
        val client = getOrCreateHttpClient()
        executeNetwork<String> {
            client.get(ReaderResource.Api.Zero.Token())
        }.onSuccess {
            postToken.set(it)
        }
    }

    private fun getOrCreateHttpClient(): HttpClient {
        val baseURL = networkSettings.getSyncUrl()
        return httpClient ?: createHttpClient(baseURL).also {
            httpClient = it
        }
    }

    private fun createHttpClient(
        baseURL: String,
    ): HttpClient =
        HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
                json(
                    contentType = ContentType.Text.Html,
                    json = Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        explicitNulls = false
                    },
                )
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
                header("Authorization", "GoogleLogin auth=${networkSettings.getSyncPwd()}")
                header(HttpHeaders.UserAgent, FEEDFLOW_USER_AGENT)
            }
            if (appEnvironment.isDebug()) {
                install(Logging) {
                    level = LogLevel.ALL
                    logger = object : io.ktor.client.plugins.logging.Logger {
                        override fun log(message: String) {
                            this@GReaderClient.logger.d { message }
                        }
                    }
                }
            }
        }

    private suspend fun executeLogin(
        block: suspend () -> HttpResponse,
    ): DataResult<String> {
        try {
            val response = block()

            if (!response.status.isSuccess()) {
                response.headers[BAZQUX_LOGIN_ERROR_HEADER]?.let { header ->
                    return DataResult.Error(header.toBazquxLoginFailure())
                }

                return when (response.status) {
                    io.ktor.http.HttpStatusCode.Unauthorized -> DataResult.Error(NetworkFailure.Unauthorised)
                    io.ktor.http.HttpStatusCode.NotFound -> DataResult.Error(DataNotFound)
                    io.ktor.http.HttpStatusCode.ServiceUnavailable,
                    io.ktor.http.HttpStatusCode.InternalServerError,
                    -> DataResult.Error(NetworkFailure.ServerFailure)
                    else -> DataResult.Error(NetworkFailure.UnhandledNetworkFailure)
                }
            }

            val res: String = response.body()
            return DataResult.Success(res)
        } catch (e: Throwable) {
            return if (e.isMissingConnectionError()) {
                DataResult.Error(NetworkFailure.NoConnection)
            } else {
                DataResult.Error(Unhandled(e))
            }
        }
    }

    private fun String.toBazquxLoginFailure(): BazquxLoginFailure =
        when (trim()) {
            "YearSubscriptionExpired" -> BazquxLoginFailure.YearSubscriptionExpired
            "FreeTrialExpired" -> BazquxLoginFailure.FreeTrialExpired
            else -> BazquxLoginFailure.Unknown
        }

    companion object {
        private const val BAZQUX_LOGIN_ERROR_HEADER = "X-BQ-LoginErrorReason"
    }
}
