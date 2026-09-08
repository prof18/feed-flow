package com.prof18.feedflow.feedsync.googledrive

import co.touchlab.kermit.Logger
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.LowLevelHttpRequest
import com.google.api.client.http.LowLevelHttpResponse
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GoogleDriveDataSourceJvmImplHttpTest {

    @Test
    fun `upload creates file and stores returned file id`() = runTest {
        val transport = RecordingDriveHttpTransport(
            ResponseSpec(body = """{"id":"drive-created"}""".encodeToByteArray()),
        )
        val dataSource = createDataSource(transport)
        val payload = "sqlite-upload\u0000".encodeToByteArray()
        val file = temporaryFile(payload)

        dataSource.performUpload(GoogleDriveUploadParam("FeedFlow.db", file))

        assertEquals("drive-created", dataSource.settings.getBackupFileId())
        assertEquals(2, transport.requests.size)
        val request = transport.requests.first()
        assertEquals("POST", request.method)
        assertTrue(request.url.contains("/drive/v3/files"))
        assertTrue(request.url.contains("uploadType=resumable"))
        assertTrue(request.body.decodeToString().contains("FeedFlow.db"))
        assertEquals("PUT", transport.requests.last().method)
        assertTrue(transport.requests.last().url.startsWith("https://fixture.invalid/upload-session"))
        assertTrue(transport.requests.last().body.contentEquals(payload))
    }

    @Test
    fun `upload updates cached file through the Drive SDK`() = runTest {
        val transport = RecordingDriveHttpTransport(
            ResponseSpec(body = """{"id":"drive-updated"}""".encodeToByteArray()),
        )
        val dataSource = createDataSource(transport, cachedFileId = "drive-existing")
        val payload = "sqlite-update".encodeToByteArray()

        dataSource.performUpload(GoogleDriveUploadParam("FeedFlow.db", temporaryFile(payload)))

        assertEquals(2, transport.requests.size)
        val request = transport.requests.first()
        assertEquals("PATCH", request.method)
        assertTrue(request.url.contains("/drive/v3/files/drive-existing"))
        assertEquals("PUT", transport.requests.last().method)
        assertTrue(transport.requests.last().url.startsWith("https://fixture.invalid/upload-session"))
        assertTrue(transport.requests.last().body.contentEquals(payload))
        assertEquals("drive-existing", dataSource.settings.getBackupFileId())
    }

    @Test
    fun `download resolves cached file id and preserves bytes`() = runTest {
        val payload = byteArrayOf(0, 1, 2, 3, 127, -1)
        val transport = RecordingDriveHttpTransport(ResponseSpec(body = payload))
        val dataSource = createDataSource(transport, cachedFileId = "drive-download")
        val output = ByteArrayOutputStream()

        dataSource.performDownload(GoogleDriveDownloadParam("FeedFlow.db", output))

        assertContentEquals(payload, output.toByteArray())
        val request = transport.requests.single()
        assertEquals("GET", request.method)
        assertTrue(request.url.contains("/drive/v3/files/drive-download"))
        assertTrue(request.url.contains("alt=media"))
    }

    @Test
    fun `download discovers file id and caches it before fetching bytes`() = runTest {
        val payload = "sqlite-discovered".encodeToByteArray()
        val transport = RecordingDriveHttpTransport(
            ResponseSpec(body = """{"files":[{"id":"drive-discovered"}]}""".encodeToByteArray()),
            ResponseSpec(body = payload),
        )
        val dataSource = createDataSource(transport)
        val output = ByteArrayOutputStream()

        dataSource.performDownload(GoogleDriveDownloadParam("FeedFlow.db", output))

        assertEquals("drive-discovered", dataSource.settings.getBackupFileId())
        assertContentEquals(payload, output.toByteArray())
        assertEquals("GET", transport.requests[0].method)
        assertTrue(transport.requests[0].url.contains("/drive/v3/files"))
        assertTrue(transport.requests[0].url.contains("spaces=appDataFolder"))
        assertEquals("GET", transport.requests[1].method)
        assertTrue(transport.requests[1].url.contains("/drive/v3/files/drive-discovered"))
    }

    private fun createDataSource(
        transport: RecordingDriveHttpTransport,
        cachedFileId: String? = null,
    ): TestDataSource {
        val settings = MapSettings()
        GoogleDriveSettings(settings).apply {
            cachedFileId?.let(::setBackupFileId)
        }
        val drive = Drive.Builder(transport, GsonFactory.getDefaultInstance(), null)
            .setApplicationName("GoogleDriveDataSourceJvmImplHttpTest")
            .build()
        val dataSource = GoogleDriveDataSourceJvmImpl(
            logger = Logger.withTag("GoogleDriveDataSourceJvmImplHttpTest"),
            dispatcherProvider = TestDispatcherProvider,
            googleDriveSettings = GoogleDriveSettings(settings),
            appEnvironment = AppEnvironment.Debug,
            driveService = drive,
        )
        return TestDataSource(dataSource, GoogleDriveSettings(settings))
    }

    private fun temporaryFile(payload: ByteArray): File = Files.createTempFile("feedflow-drive", ".db").toFile().apply {
        writeBytes(payload)
        deleteOnExit()
    }
}

private class TestDataSource(
    val dataSource: GoogleDriveDataSourceJvmImpl,
    val settings: GoogleDriveSettings,
) : GoogleDriveDataSourceJvm by dataSource

private object TestDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Unconfined
    override val default: CoroutineDispatcher = Dispatchers.Unconfined
    override val io: CoroutineDispatcher = Dispatchers.Unconfined
}

private data class ResponseSpec(
    val body: ByteArray,
    val statusCode: Int = 200,
    val contentType: String = "application/json",
    val headers: Map<String, String> = emptyMap(),
)

private data class RecordedRequest(
    val method: String,
    val url: String,
    val body: ByteArray,
)

private class RecordingDriveHttpTransport(
    private val responses: ArrayDeque<ResponseSpec>,
) : HttpTransport() {
    val requests = mutableListOf<RecordedRequest>()

    constructor(vararg responses: ResponseSpec) : this(ArrayDeque(responses.toList()))

    override fun supportsMethod(method: String): Boolean = true

    override fun buildRequest(method: String, url: String): LowLevelHttpRequest =
        object : LowLevelHttpRequest() {
            override fun addHeader(name: String, value: String) = Unit

            override fun execute(): LowLevelHttpResponse {
                val body = ByteArrayOutputStream()
                getStreamingContent()?.writeTo(body)
                synchronized(requests) {
                    val bytes = if (contentEncoding == "gzip") {
                        java.util.zip.GZIPInputStream(body.toByteArray().inputStream()).use { it.readBytes() }
                    } else {
                        body.toByteArray()
                    }
                    requests += RecordedRequest(method, url, bytes)
                }
                return if (url.contains("uploadType=resumable")) {
                    RecordingDriveHttpResponse(
                        ResponseSpec(
                            body = byteArrayOf(),
                            headers = mapOf("Location" to "https://fixture.invalid/upload-session"),
                        ),
                    )
                } else {
                    RecordingDriveHttpResponse(responses.removeFirst())
                }
            }
        }
}

private class RecordingDriveHttpResponse(
    private val response: ResponseSpec,
) : LowLevelHttpResponse() {
    override fun getContent(): InputStream = response.body.inputStream()

    override fun getContentEncoding(): String? = null

    override fun getContentLength(): Long = response.body.size.toLong()

    override fun getContentType(): String = response.contentType

    override fun getStatusLine(): String = "HTTP/1.1 ${response.statusCode}"

    override fun getStatusCode(): Int = response.statusCode

    override fun getReasonPhrase(): String = "OK"

    override fun getHeaderCount(): Int = response.headers.size

    override fun getHeaderName(index: Int): String = response.headers.keys.elementAt(index)

    override fun getHeaderValue(index: Int): String = response.headers.values.elementAt(index)
}
