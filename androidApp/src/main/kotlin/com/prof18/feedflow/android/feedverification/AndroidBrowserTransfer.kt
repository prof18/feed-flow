package com.prof18.feedflow.android.feedverification

import android.util.Base64
import android.webkit.WebView
import com.prof18.feedflow.core.utils.HttpHostValidator
import com.prof18.feedflow.shared.domain.recovery.BrowserCapture
import com.prof18.feedflow.shared.domain.recovery.RecoveryPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.UUID
import kotlin.coroutines.resume

internal class AndroidBrowserTransfer(private val webView: WebView) {
    private var active = false

    suspend fun capture(url: String): BrowserCapture = withContext(Dispatchers.Main.immediate) {
        check(!active) { "A browser transfer is already active" }
        require(HttpHostValidator.isUrlSafeForHttpClient(url))
        val request = url.toHttpUrl()
        require(request.scheme == "https" && request.username.isEmpty() && request.password.isEmpty())
        val requestUrl = request.newBuilder().fragment(null).build().toString()
        val origin = origin(requestUrl)
        check(origin(webView.url.orEmpty()) == origin) { "The visible browser must be on the requested origin" }
        active = true
        val nonce = UUID.randomUUID().toString()
        val quotedNonce = JSONObject.quote(nonce)
        val config = JSONObject().apply {
            put("url", requestUrl)
            put("nonce", nonce)
            put("maxBytes", RecoveryPolicy.MAX_BODY_BYTES)
            put("chunkBytes", RecoveryPolicy.TRANSFER_CHUNK_BYTES)
            put("timeoutMs", RecoveryPolicy.BROWSER_FETCH_TIMEOUT_MILLIS)
        }
        try {
            withTimeoutOrNull(RecoveryPolicy.BROWSER_FETCH_TIMEOUT_MILLIS) {
                val script = webView.context.assets.open("feed-recovery-transfer.js")
                    .bufferedReader().use { it.readText() }
                evaluate("$script\nglobalThis.__feedFlowTransfer = __feedFlowCreateTransfer($config); true;")
                val bytes = ByteArrayOutputStream()
                var sequence = 0
                while (true) {
                    check(origin(webView.url.orEmpty()) == origin) { "Browser navigation invalidated transfer" }
                    val raw = evaluate("JSON.stringify(globalThis.__feedFlowTransfer.next($quotedNonce))")
                    val message = JSONTokener(raw).nextValue() as? String
                        ?: throw BrowserTransferFailure(kind = "navigation")
                    val json = JSONObject(message)
                    check(json.getString("nonce") == nonce) { "Transfer ownership changed" }
                    when (json.getString("kind")) {
                        "waiting" -> delay(RESULT_POLL_MILLIS)
                        "chunk" -> {
                            check(json.getInt("sequence") == sequence++) { "Transfer sequence changed" }
                            val encoded = json.getString("base64")
                            check(encoded.length <= MAX_ENCODED_CHUNK)
                            val chunk = Base64.decode(encoded, Base64.NO_WRAP)
                            check(chunk.size <= RecoveryPolicy.TRANSFER_CHUNK_BYTES)
                            check(bytes.size() + chunk.size <= RecoveryPolicy.MAX_BODY_BYTES)
                            bytes.write(chunk)
                        }
                        "done" -> {
                            check(json.getInt("total") == bytes.size()) { "Transfer length changed" }
                            check(
                                json.getString("effectiveUrl") == requestUrl &&
                                    json.getInt("status") in HTTP_OK..HTTP_LAST_SUCCESS,
                            )
                            return@withTimeoutOrNull BrowserCapture(
                                effectiveUrl = requestUrl,
                                statusCode = json.getInt("status"),
                                contentType = json.optionalString("contentType"),
                                bytes = bytes.toByteArray(),
                            )
                        }
                        "error" -> throw BrowserTransferFailure(
                            kind = json.getString("reason").safeReason(),
                            status = if (json.has("status")) json.getInt("status") else null,
                            retryAfter = if (json.has("retryAfter")) json.optionalString("retryAfter") else null,
                        )
                        else -> error("Unexpected transfer message")
                    }
                }
                @Suppress("UNREACHABLE_CODE")
                error("Transfer did not complete")
            } ?: throw BrowserTransferFailure(kind = "timeout")
        } finally {
            webView.evaluateJavascript("globalThis.__feedFlowTransfer?.cancel($quotedNonce);", null)
            active = false
        }
    }

    private suspend fun evaluate(script: String): String = suspendCancellableCoroutine { continuation ->
        webView.evaluateJavascript(script) { value ->
            if (continuation.isActive) continuation.resume(value)
        }
    }

    private fun origin(url: String): String? = runCatching {
        val uri = URI(url)
        if (uri.scheme != "https" || uri.host == null) return@runCatching null
        "https://${uri.host.lowercase()}:${if (uri.port == -1) HTTPS_PORT else uri.port}"
    }.getOrNull()

    private fun JSONObject.optionalString(key: String): String? = if (isNull(key)) null else getString(key)

    private fun String.safeReason(): String = takeIf { it in FAILURE_REASONS } ?: "protocol"

    companion object {
        private const val HTTPS_PORT = 443
        private const val RESULT_POLL_MILLIS = 250L
        private const val HTTP_OK = 200
        private const val HTTP_LAST_SUCCESS = 299
        private const val MAX_ENCODED_CHUNK = (RecoveryPolicy.TRANSFER_CHUNK_BYTES + 2) / 3 * 4
        private val FAILURE_REASONS = setOf(
            "timeout", "navigation", "origin", "redirect", "challenge", "http", "empty", "too-large",
            "transport", "owner", "canceled",
        )
    }
}

internal class BrowserTransferFailure(
    val kind: String,
    val status: Int? = null,
    val retryAfter: String? = null,
) : IllegalStateException("Browser transfer failed: $kind")
