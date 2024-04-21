package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import com.prof18.feedflow.shared.utils.DispatcherProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class JvmHtmlRetriever(
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
) : HtmlRetriever {
    override suspend fun retrieveHtml(url: String): String? = withContext(dispatcherProvider.io) {
        try {
            val request = Request.Builder()
                .url(url)
                .build()
            return@withContext OkHttpClient().newCall(request).awaitForString()
        } catch (e: Exception) {
            logger.e(e) { "Unable to retrieve HTML, skipping" }
            return@withContext null
        }
    }

    private suspend fun Call.awaitForString(): String? = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            cancel()
        }

        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = requireNotNull(response.body)
                    continuation.resume(body.string())
                } else {
                    continuation.resume(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        })
    }
}
