package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import com.prof18.feedflow.shared.utils.DispatcherProvider
import com.prof18.rssparser.exception.HttpException
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

@Suppress("CAST_NEVER_SUCCEEDS")
@OptIn(BetaInteropApi::class)
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
                } else if (response != null && (response as NSHTTPURLResponse).statusCode !in 200..299) {
                    val exception = HttpException(
                        code = response.statusCode.toInt(),
                        message = response.description,
                    )
                    logger.e(exception) { "Unable to retrieve HTML, skipping" }

                    continuation.resume(null)
                } else if (data != null) {
                    val htmlString = NSString.create(data = data, encoding = NSUTF8StringEncoding)
                    continuation.resume(htmlString as String)
                }
            }

            continuation.invokeOnCancellation {
                task.cancel()
            }

            task.resume()
        }
    }
}
