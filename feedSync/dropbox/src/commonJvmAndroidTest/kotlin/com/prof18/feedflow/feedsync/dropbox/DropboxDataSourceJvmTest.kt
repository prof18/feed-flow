@file:Suppress("MagicNumber")

package com.prof18.feedflow.feedsync.dropbox

import co.touchlab.kermit.Logger
import com.dropbox.core.http.HttpRequestor
import com.dropbox.core.oauth.DbxCredential
import com.prof18.feedflow.core.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DropboxDataSourceJvmTest {

    @Test
    fun `upload sends SDK request and preserves metadata and bytes`() = runTest {
        val requestor = RecordingDropboxHttpRequestor(
            ResponseSpec(
                statusCode = 200,
                body = metadata("upload", 13).encodeToByteArray(),
            ),
        )
        val dataSource = createDataSource(requestor)
        val payload = "sqlite-bytes\u0000".encodeToByteArray()
        val file = Files.createTempFile("feedflow-dropbox-upload", ".db").toFile().apply {
            writeBytes(payload)
            deleteOnExit()
        }

        dataSource.restoreAuth(credentials())
        val result = dataSource.performUpload(
            DropboxUploadParam(path = "/FeedFlow.db", file = file),
        )

        assertEquals("id:upload", result.id)
        assertEquals(Instant.parse("2024-01-02T03:04:05Z").toEpochMilli(), result.editDateMillis)
        assertEquals(payload.size.toLong(), result.sizeInByte)
        assertEquals("a".repeat(64), result.contentHash)
        val request = requestor.requests.single()
        assertEquals("POST", request.method)
        assertTrue(request.url.endsWith("/2/files/upload"))
        assertTrue(request.headers["Dropbox-API-Arg"].orEmpty().contains("\"path\":\"/FeedFlow.db\""))
        assertTrue(request.headers["Dropbox-API-Arg"].orEmpty().contains("\"mode\":\"overwrite\""))
        assertContentEquals(payload, request.body)
    }

    @Test
    fun `download sends SDK request and preserves response bytes and metadata`() = runTest {
        val payload = byteArrayOf(0, 1, 2, 3, 127, -1)
        val requestor = RecordingDropboxHttpRequestor(
            ResponseSpec(
                statusCode = 200,
                headers = mapOf(
                    "Dropbox-API-Result" to metadata("download", 6),
                ),
                body = payload,
            ),
        )
        val dataSource = createDataSource(requestor)
        val destination = ByteArrayOutputStream()

        dataSource.restoreAuth(credentials())
        val result = dataSource.performDownload(
            DropboxDownloadParam(path = "/FeedFlow.db", outputStream = destination),
        )

        assertEquals("id:download", result.id)
        assertEquals(payload.size.toLong(), result.sizeInByte)
        assertEquals("a".repeat(64), result.contentHash)
        assertContentEquals(payload, destination.toByteArray())
        val request = requestor.requests.single()
        assertEquals("POST", request.method)
        assertTrue(request.url.endsWith("/2/files/download"))
        assertTrue(request.headers["Dropbox-API-Arg"].orEmpty().contains("\"path\":\"/FeedFlow.db\""))
    }

    private fun createDataSource(requestor: HttpRequestor) = DropboxDataSourceJvm(
        logger = Logger.withTag("DropboxDataSourceJvmTest"),
        dispatcherProvider = TestDispatcherProvider,
        httpRequestor = requestor,
    )

    private fun credentials() = DropboxStringCredentials(
        DbxCredential("test-access-token").toString(),
    )

    private fun metadata(id: String, size: Int): String = """
        {"name":"FeedFlow.db","id":"id:$id","client_modified":"2024-01-02T03:04:05Z",
        "server_modified":"2024-01-02T03:04:05Z","rev":"0123456789abc","size":$size,
        "content_hash":"${"a".repeat(64)}"}
    """.trimIndent()
}

private object TestDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Unconfined
    override val default: CoroutineDispatcher = Dispatchers.Unconfined
    override val io: CoroutineDispatcher = Dispatchers.Unconfined
}

private data class ResponseSpec(
    val statusCode: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray,
)

private data class RecordedRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: ByteArray,
)

private class RecordingDropboxHttpRequestor(
    vararg responseSpecs: ResponseSpec,
) : HttpRequestor() {
    private val responses = ConcurrentLinkedQueue(responseSpecs.toList())
    val requests = mutableListOf<RecordedRequest>()

    override fun doGet(
        url: String,
        headers: Iterable<HttpRequestor.Header>,
    ): HttpRequestor.Response = error("Unexpected GET request: $url")

    override fun startPost(
        url: String,
        headers: Iterable<HttpRequestor.Header>,
    ): HttpRequestor.Uploader = uploader("POST", url, headers)

    override fun startPut(
        url: String,
        headers: Iterable<HttpRequestor.Header>,
    ): HttpRequestor.Uploader = uploader("PUT", url, headers)

    private fun uploader(
        method: String,
        url: String,
        headers: Iterable<HttpRequestor.Header>,
    ) = object : HttpRequestor.Uploader() {
        private val body = ByteArrayOutputStream()
        private val requestHeaders = headers.associate { it.key to it.value }.toMutableMap()

        override fun getBody(): OutputStream = body

        override fun close() = Unit

        override fun abort() = Unit

        override fun finish(): HttpRequestor.Response {
            val recordedRequest = RecordedRequest(method, url, requestHeaders, body.toByteArray())
            synchronized(requests) { requests += recordedRequest }
            val response = responses.poll() ?: error("No response configured for $url")
            return HttpRequestor.Response(
                response.statusCode,
                response.body.inputStream(),
                response.headers.mapValues { listOf(it.value) },
            )
        }
    }
}
