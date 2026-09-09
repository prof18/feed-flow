@file:Suppress("MagicNumber")

package com.prof18.feedflow.feedsync.dropbox

import co.touchlab.kermit.Logger
import com.dropbox.core.http.HttpRequestor
import com.dropbox.core.oauth.DbxCredential
import com.prof18.feedflow.core.model.CloudBackupNotFoundException
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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DropboxDataSourceJvmTest {

    @Test
    fun `only path not found is reported as a missing backup`() = runTest {
        for (tag in listOf("not_found", "not_file")) {
            val requestor = RecordingDropboxHttpRequestor(
                ResponseSpec(
                    statusCode = 409,
                    headers = mapOf("Content-Type" to "application/json"),
                    body = """{"error_summary":"path/$tag/","error":{".tag":"path","path":{".tag":"$tag"}}}"""
                        .encodeToByteArray(),
                ),
            )
            val dataSource = createDataSource(requestor)
            dataSource.restoreAuth(credentials())
            val error = assertFailsWith<Exception> {
                dataSource.performDownload(DropboxDownloadParam("/missing.db", ByteArrayOutputStream()))
            }
            assertEquals(tag == "not_found", error is CloudBackupNotFoundException)
            assertEquals(1, requestor.requests.size)
        }
    }

    @Test
    fun `create upload is conditional and preserves metadata and bytes`() = runTest {
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
        assertEquals("0123456789abc", result.revision)
        val request = requestor.requests.single()
        assertEquals("POST", request.method)
        assertTrue(request.url.endsWith("/2/files/upload"))
        assertTrue(request.headers["Dropbox-API-Arg"].orEmpty().contains("\"path\":\"/FeedFlow.db\""))
        assertTrue(request.headers["Dropbox-API-Arg"].orEmpty().contains("\"mode\":\"add\""))
        assertTrue(request.headers["Dropbox-API-Arg"].orEmpty().contains("\"autorename\":false"))
        assertTrue(request.headers["Dropbox-API-Arg"].orEmpty().contains("\"strict_conflict\":true"))
        assertContentEquals(payload, request.body)
    }

    @Test
    fun `update upload sends exact expected revision without autorename`() = runTest {
        val requestor = RecordingDropboxHttpRequestor(
            ResponseSpec(statusCode = 200, body = metadata("upload", 7).encodeToByteArray()),
        )
        val dataSource = createDataSource(requestor)
        val file = Files.createTempFile("feedflow-dropbox-update", ".db").toFile().apply {
            writeBytes("updated".encodeToByteArray())
            deleteOnExit()
        }

        dataSource.restoreAuth(credentials())
        dataSource.performUpload(
            DropboxUploadParam(
                path = "/FeedFlow.db",
                file = file,
                expectedRevision = "fedcba987654321",
            ),
        )

        val apiArgument = requestor.requests.single().headers["Dropbox-API-Arg"].orEmpty()
        assertTrue(apiArgument.contains("\".tag\":\"update\""))
        assertTrue(apiArgument.contains("\"update\":\"fedcba987654321\""))
        assertTrue(apiArgument.contains("\"autorename\":false"))
        assertTrue(apiArgument.contains("\"strict_conflict\":true"))
    }

    @Test
    fun `upload path conflict maps to dedicated conflict exception`() = runTest {
        val requestor = RecordingDropboxHttpRequestor(
            ResponseSpec(
                statusCode = 409,
                headers = mapOf("Content-Type" to "application/json"),
                body = uploadPathError("conflict", "file").encodeToByteArray(),
            ),
        )
        val dataSource = createDataSource(requestor)
        val file = Files.createTempFile("feedflow-dropbox-conflict", ".db").toFile().apply {
            writeBytes(byteArrayOf(1))
            deleteOnExit()
        }

        dataSource.restoreAuth(credentials())

        assertFailsWith<DropboxUploadConflictException> {
            dataSource.performUpload(DropboxUploadParam("/FeedFlow.db", file))
        }
    }

    @Test
    fun `non conflict upload path failure stays a generic upload exception`() = runTest {
        val requestor = RecordingDropboxHttpRequestor(
            ResponseSpec(
                statusCode = 409,
                headers = mapOf("Content-Type" to "application/json"),
                body = uploadPathError("no_write_permission").encodeToByteArray(),
            ),
        )
        val dataSource = createDataSource(requestor)
        val file = Files.createTempFile("feedflow-dropbox-failure", ".db").toFile().apply {
            writeBytes(byteArrayOf(1))
            deleteOnExit()
        }

        dataSource.restoreAuth(credentials())

        assertFailsWith<DropboxUploadException> {
            dataSource.performUpload(DropboxUploadParam("/FeedFlow.db", file))
        }
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
        assertEquals("0123456789abc", result.revision)
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

    private fun uploadPathError(reason: String, conflict: String? = null): String {
        val reasonJson = if (conflict == null) {
            """{".tag":"$reason"}"""
        } else {
            """{".tag":"$reason","conflict":{".tag":"$conflict"}}"""
        }
        return """
            {"error_summary":"path/$reason/","error":{".tag":"path","reason":$reasonJson,
            "upload_session_id":"session-id"}}
        """.trimIndent()
    }
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
