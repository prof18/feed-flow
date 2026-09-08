@file:Suppress("MagicNumber")

package com.prof18.feedflow.feedsync.googledrive

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import co.touchlab.kermit.Logger
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.LowLevelHttpRequest
import com.google.api.client.http.LowLevelHttpResponse
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
class GoogleDriveAndroidDataSourceImplTest {

    @Test
    fun `stale download ID is rediscovered across pages and persisted after transfer`() = runTest {
        val settings = GoogleDriveSettings(MapSettings()).also { it.setBackupFileId("stale") }
        val payload = "new snapshot".encodeToByteArray()
        val transport = RecordingDriveHttpTransport(
            ResponseSpec(statusCode = 404, body = """{"error":{"code":404}}""".encodeToByteArray()),
            ResponseSpec(body = """{"files":[],"nextPageToken":"next"}""".encodeToByteArray()),
            ResponseSpec(body = """{"files":[{"id":"current"}]}""".encodeToByteArray()),
            ResponseSpec(body = payload),
        )
        val output = ByteArrayOutputStream()
        createDataSource(settings, transport).performDownload(GoogleDriveDownloadParam("FeedFlow.db", output))
        assertArrayEquals(payload, output.toByteArray())
        assertEquals("current", settings.getBackupFileId())
        assertEquals(4, transport.requests.size)
        assertTrue(transport.requests[2].url.contains("pageToken=next"))
    }

    @Test
    fun `upload creates file and stores returned file id`() = runTest {
        val transport = RecordingDriveHttpTransport(
            ResponseSpec(body = "{\"files\":[]}".encodeToByteArray()),
            ResponseSpec(body = """{"id":"android-created"}""".encodeToByteArray()),
        )
        val settings = GoogleDriveSettings(MapSettings())
        val dataSource = createDataSource(settings, transport)
        val payload = "android-upload\u0000".encodeToByteArray()
        val file = java.io.File.createTempFile("feedflow-drive-android", ".db").apply {
            writeBytes(payload)
            deleteOnExit()
        }

        dataSource.performUpload(GoogleDriveUploadParam("FeedFlow.db", file))

        assertEquals("android-created", settings.getBackupFileId())
        assertEquals(3, transport.requests.size)
        val request = transport.requests[1]
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
        val settings = GoogleDriveSettings(MapSettings())
        settings.setBackupFileId("android-existing")
        val transport = RecordingDriveHttpTransport(
            ResponseSpec(body = """{"id":"android-existing"}""".encodeToByteArray()),
        )
        val dataSource = createDataSource(settings, transport)
        val payload = "android-update".encodeToByteArray()
        val file = java.io.File.createTempFile("feedflow-drive-android", ".db").apply {
            writeBytes(payload)
            deleteOnExit()
        }

        dataSource.performUpload(GoogleDriveUploadParam("FeedFlow.db", file))

        assertEquals(2, transport.requests.size)
        val request = transport.requests.first()
        assertEquals("PATCH", request.method)
        assertTrue(request.url.contains("/drive/v3/files/android-existing"))
        assertEquals("PUT", transport.requests.last().method)
        assertTrue(transport.requests.last().url.startsWith("https://fixture.invalid/upload-session"))
        assertTrue(transport.requests.last().body.contentEquals(payload))
    }

    @Test
    fun `upload rediscoveres after stale cached id and updates the discovered file`() = runTest {
        val settings = GoogleDriveSettings(MapSettings()).also { it.setBackupFileId("stale") }
        val transport = RecordingDriveHttpTransport(
            ResponseSpec(
                body = """{"error":{"code":404,"message":"Not Found"}}""".encodeToByteArray(),
                statusCode = 404,
            ),
            ResponseSpec(body = """{"files":[{"id":"rediscovered"}]}""".encodeToByteArray()),
            ResponseSpec(body = """{"id":"rediscovered"}""".encodeToByteArray()),
        )
        val dataSource = createDataSource(settings, transport)

        dataSource.performUpload(GoogleDriveUploadParam("FeedFlow.db", temporaryFile("payload".encodeToByteArray())))

        assertEquals("rediscovered", settings.getBackupFileId())
        assertEquals(listOf("PATCH", "PUT", "GET", "PATCH", "PUT"), transport.requests.map { it.method })
    }

    @Test
    fun `upload propagates discovery failure without creating a file`() = runTest {
        val settings = GoogleDriveSettings(MapSettings())
        val transport = RecordingDriveHttpTransport(
            ResponseSpec(body = """{"error":{"code":500,"message":"backend"}}""".encodeToByteArray(), statusCode = 500),
        )
        val dataSource = createDataSource(settings, transport)

        assertThrows(GoogleJsonResponseException::class.java) {
            kotlinx.coroutines.runBlocking {
                dataSource.performUpload(GoogleDriveUploadParam("FeedFlow.db", temporaryFile(byteArrayOf(1))))
            }
        }
        assertEquals(1, transport.requests.size)
    }

    @Test
    fun `duplicate discovered files fail safely`() = runTest {
        val settings = GoogleDriveSettings(MapSettings())
        val transport = RecordingDriveHttpTransport(
            ResponseSpec(body = """{"files":[{"id":"one"},{"id":"two"}]}""".encodeToByteArray()),
        )
        val dataSource = createDataSource(settings, transport)

        assertThrows(GoogleDriveUploadException::class.java) {
            kotlinx.coroutines.runBlocking {
                dataSource.performUpload(GoogleDriveUploadParam("FeedFlow.db", temporaryFile(byteArrayOf(1))))
            }
        }
        assertEquals(1, transport.requests.size)
    }

    @Test
    fun `typed reauth exception from token provider propagates`() = runTest {
        val settings = GoogleDriveSettings(MapSettings())
        val dataSource = GoogleDriveAndroidDataSourceImpl(
            context = ApplicationProvider.getApplicationContext(),
            googleDriveSettings = settings,
            logger = Logger.withTag("GoogleDriveAndroidDataSourceImplTest"),
            dispatcherProvider = TestDispatcherProvider,
            accessTokenProvider = { throw GoogleDriveNeedsReAuthException("reauth") },
        )

        assertThrows(GoogleDriveNeedsReAuthException::class.java) {
            kotlinx.coroutines.runBlocking {
                dataSource.performUpload(GoogleDriveUploadParam("FeedFlow.db", temporaryFile(byteArrayOf(1))))
            }
        }
    }

    @Test
    fun `download resolves cached file id and preserves bytes`() = runTest {
        val payload = byteArrayOf(0, 1, 2, 3, 127, -1)
        val settings = GoogleDriveSettings(MapSettings())
        settings.setBackupFileId("android-download")
        val transport =
            RecordingDriveHttpTransport(ResponseSpec(body = payload, contentType = "application/octet-stream"))
        val dataSource = createDataSource(settings, transport)
        val output = ByteArrayOutputStream()

        dataSource.performDownload(GoogleDriveDownloadParam("FeedFlow.db", output))

        assertArrayEquals(payload, output.toByteArray())
        val request = transport.requests.single()
        assertEquals("GET", request.method)
        assertTrue(request.url.contains("/drive/v3/files/android-download"))
        assertTrue(request.url.contains("alt=media"))
    }

    @Test
    fun `fresh device discovers backup identity before downloading`() = runTest {
        val payload = byteArrayOf(0, -1, 42, 127)
        val settings = GoogleDriveSettings(MapSettings())
        val transport = RecordingDriveHttpTransport(
            ResponseSpec(body = """{"files":[{"id":"discovered"}]}""".encodeToByteArray()),
            ResponseSpec(body = payload, contentType = "application/octet-stream"),
        )
        val output = ByteArrayOutputStream()
        createDataSource(settings, transport).performDownload(GoogleDriveDownloadParam("FeedFlow.db", output))

        assertEquals("discovered", settings.getBackupFileId())
        assertArrayEquals(payload, output.toByteArray())
        assertEquals(2, transport.requests.size)
        assertTrue(transport.requests.first().url.contains("spaces=appDataFolder"))
        assertTrue(transport.requests.last().url.contains("/drive/v3/files/discovered"))
    }

    private fun createDataSource(
        settings: GoogleDriveSettings,
        transport: RecordingDriveHttpTransport,
    ) = GoogleDriveAndroidDataSourceImpl(
        context = ApplicationProvider.getApplicationContext<Context>(),
        googleDriveSettings = settings,
        logger = Logger.withTag("GoogleDriveAndroidDataSourceImplTest"),
        dispatcherProvider = TestDispatcherProvider,
        httpTransport = transport,
        accessTokenProvider = { "android-test-token" },
    )

    private fun temporaryFile(payload: ByteArray): java.io.File = java.io.File.createTempFile(
        "feedflow-drive-android",
        ".db",
    ).apply {
        writeBytes(payload)
        deleteOnExit()
    }
}

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
                val body = java.io.ByteArrayOutputStream()
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
