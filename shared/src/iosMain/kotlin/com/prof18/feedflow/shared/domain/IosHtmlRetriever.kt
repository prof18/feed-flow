package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import com.prof18.feedflow.shared.utils.DispatcherProvider
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataTaskWithURL
import kotlin.coroutines.resume

@OptIn(BetaInteropApi::class)
@Suppress("MagicNumber")
internal class IosHtmlRetriever(
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
) : HtmlRetriever {
    override suspend fun retrieveHtml(url: String): String? = withContext(dispatcherProvider.io) {
        suspendCancellableCoroutine { continuation ->
            val task = NSURLSession.sharedSession.dataTaskWithURL(
                url = NSURL(string = url),
            ) { data: NSData?, response: NSURLResponse?, error: NSError? ->
                if (error != null) {
                    val throwable = Throwable(
                        message = error.description,
                    )
                    logger.e(throwable) { "Unable to retrieve HTML, skipping" }

                    continuation.resume(null)
                } else if (data != null) {
                    val responseCode = (response as? NSHTTPURLResponse)?.statusCode ?: 0
                    if (responseCode == 200L) {
                        val htmlString = NSString.create(data, NSUTF8StringEncoding).toString()
                        continuation.resume(htmlString)
                    } else {
                        continuation.resume(null)
                    }
                }
            }

            continuation.invokeOnCancellation {
                task.cancel()
            }

            task.resume()
        }
    }
}
