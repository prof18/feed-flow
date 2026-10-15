package com.prof18.feedflow.android.notifications

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmapOrNull
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Scale
import com.prof18.feedflow.core.model.ArticleNotificationPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URI

class AndroidNotificationImageLoader(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val execute: suspend (ImageRequest) -> Bitmap? = { request ->
        (context.imageLoader.execute(request) as? SuccessResult)?.image
            ?.asDrawable(context.resources)?.toBitmapOrNull()
    },
) {
    suspend fun load(url: String?, allowanceMillis: Long): Bitmap? {
        if (allowanceMillis <= 0 || !isWebImage(url)) return null
        return withContext(dispatcher) {
            withTimeoutOrNull(allowanceMillis) {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .allowHardware(false)
                        .size(ArticleNotificationPolicy.IMAGE_EDGE_PX)
                        .scale(Scale.FIT)
                        .build()
                    execute(request)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    private fun isWebImage(url: String?): Boolean = try {
        val uri = URI(url.orEmpty())
        uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()
    } catch (_: Exception) {
        false
    }
}
