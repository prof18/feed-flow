package com.prof18.feedflow.android.widget.components

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.ContentScale
import androidx.glance.layout.size
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.size.Precision
import coil3.size.Scale
import com.prof18.feedflow.android.widget.WidgetImageRequestIdentity
import com.prof18.feedflow.android.widget.WidgetImageRequestPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val ANDROID_S_API = 31
private const val ARGB_8888_BYTES_PER_PIXEL = 4.0

internal fun resolveWidgetArticleImageIdentity(
    requestIdentity: WidgetImageRequestIdentity?,
    renderPolicy: WidgetArticleImageRenderPolicy,
    sdkInt: Int,
    cornerRadiusDp: Int,
    displayViewportDp: Int,
): WidgetImageRequestIdentity? {
    val usesSoftwareRounding =
        renderPolicy == WidgetArticleImageRenderPolicy.CARD_COMPATIBLE &&
            sdkInt < ANDROID_S_API &&
            cornerRadiusDp > 0 &&
            displayViewportDp > 0
    return requestIdentity?.copy(
        softwareCornerRadiusDp = if (usesSoftwareRounding) cornerRadiusDp else 0,
        softwareDisplayViewportDp = if (usesSoftwareRounding) displayViewportDp else 0,
    )
}

@Composable
internal fun WidgetArticleImage(
    requestPolicy: WidgetImageRequestPolicy?,
    displayViewportDp: Dp,
    cornerRadiusDp: Dp,
    renderPolicy: WidgetArticleImageRenderPolicy,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val fullKey = resolveWidgetArticleImageIdentity(
        requestIdentity = requestPolicy?.identity,
        renderPolicy = renderPolicy,
        sdkInt = Build.VERSION.SDK_INT,
        cornerRadiusDp = cornerRadiusDp.value.roundToInt(),
        displayViewportDp = displayViewportDp.value.roundToInt(),
    )
    val currentFullKey by rememberUpdatedState(fullKey)
    var imageState by remember { mutableStateOf(WidgetArticleImageState<Bitmap>()) }

    LaunchedEffect(fullKey) {
        imageState = imageState.forKey(fullKey)
        val requestedKey = fullKey ?: return@LaunchedEffect
        val result = withContext(Dispatchers.IO) {
            loadWidgetArticleBitmap(
                context = context,
                resources = context.resources,
                key = requestedKey,
            )
        }
        imageState = imageState.accept(
            completedKey = requestedKey,
            currentKey = currentFullKey,
            value = result,
        )
    }

    imageState.valueFor(fullKey)?.let { bitmap ->
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(displayViewportDp)
                .cornerRadius(cornerRadiusDp),
        )
    }
}

internal enum class WidgetArticleImageRenderPolicy {
    GLANCE_ONLY,
    CARD_COMPATIBLE,
}

internal data class WidgetArticleImageState<T>(
    val requestKey: WidgetImageRequestIdentity? = null,
    val value: T? = null,
) {
    fun forKey(currentKey: WidgetImageRequestIdentity?): WidgetArticleImageState<T> =
        if (requestKey == currentKey) this else WidgetArticleImageState(requestKey = currentKey)

    fun accept(
        completedKey: WidgetImageRequestIdentity,
        currentKey: WidgetImageRequestIdentity?,
        value: T?,
    ): WidgetArticleImageState<T> {
        val currentState = forKey(currentKey)
        return if (completedKey == currentKey) currentState.copy(value = value) else currentState
    }

    fun valueFor(currentKey: WidgetImageRequestIdentity?): T? =
        value.takeIf { requestKey == currentKey }
}

internal fun buildWidgetArticleImageRequest(
    context: Context,
    key: WidgetImageRequestIdentity,
): ImageRequest = ImageRequest.Builder(context)
    .data(key.imageUrl)
    .size(key.edgePx, key.edgePx)
    .precision(Precision.EXACT)
    .scale(Scale.FILL)
    .allowHardware(false)
    .bitmapConfig(Bitmap.Config.ARGB_8888)
    .build()

private suspend fun loadWidgetArticleBitmap(
    context: Context,
    resources: Resources,
    key: WidgetImageRequestIdentity,
): Bitmap? = try {
    val result = context.imageLoader.execute(buildWidgetArticleImageRequest(context, key))
    val deliveredBitmap = (result as? SuccessResult)
        ?.image
        ?.asDrawable(resources)
        ?.toBitmapOrNull()
        ?: return null
    validateAndRenderWidgetArticleBitmap(
        bitmap = deliveredBitmap,
        key = key,
    )
} catch (exception: CancellationException) {
    throw exception
} catch (_: Exception) {
    null
}

internal fun validateAndRenderWidgetArticleBitmap(
    bitmap: Bitmap,
    key: WidgetImageRequestIdentity,
): Bitmap? = try {
    val validatedBitmap = WidgetBitmapValidator.validate(
        bitmap = bitmap,
        requestEdgePx = key.edgePx,
        payloadBudgetBytes = key.payloadBudgetBytes,
    ) ?: return null
    val renderedBitmap = softwareRoundWidgetArticleBitmap(
        bitmap = validatedBitmap,
        key = key,
    ) ?: return null
    WidgetBitmapValidator.validate(
        bitmap = renderedBitmap,
        requestEdgePx = key.edgePx,
        payloadBudgetBytes = key.payloadBudgetBytes,
    )
} catch (_: Exception) {
    null
}

private fun softwareRoundWidgetArticleBitmap(
    bitmap: Bitmap,
    key: WidgetImageRequestIdentity,
): Bitmap? {
    if (key.softwareCornerRadiusDp <= 0) {
        return bitmap
    }
    if (key.softwareDisplayViewportDp < 1) {
        return null
    }
    val budgetEdgePx = sqrt(key.payloadBudgetBytes / ARGB_8888_BYTES_PER_PIXEL).toInt()
    val outputEdgePx = minOf(
        max(bitmap.width, bitmap.height),
        key.edgePx,
        budgetEdgePx,
    )
    if (outputEdgePx < 1) {
        return null
    }
    val radiusPx = (
        key.softwareCornerRadiusDp.toFloat() /
            key.softwareDisplayViewportDp *
            outputEdgePx
        ).coerceAtMost(outputEdgePx / 2f)
    val sourceEdgePx = minOf(bitmap.width, bitmap.height)
    val sourceLeft = (bitmap.width - sourceEdgePx) / 2
    val sourceTop = (bitmap.height - sourceEdgePx) / 2
    val sourceRect = Rect(
        sourceLeft,
        sourceTop,
        sourceLeft + sourceEdgePx,
        sourceTop + sourceEdgePx,
    )
    val outputRect = Rect(0, 0, outputEdgePx, outputEdgePx)
    val outputRoundRect = RectF(outputRect)
    return Bitmap.createBitmap(outputEdgePx, outputEdgePx, Bitmap.Config.ARGB_8888).also { output ->
        val canvas = Canvas(output)
        canvas.clipPath(
            Path().apply {
                addRoundRect(outputRoundRect, radiusPx, radiusPx, Path.Direction.CW)
            },
        )
        canvas.drawBitmap(
            bitmap,
            sourceRect,
            outputRect,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
    }
}

internal object WidgetBitmapValidator {

    fun validate(
        bitmap: Bitmap,
        requestEdgePx: Int,
        payloadBudgetBytes: Long,
    ): Bitmap? = try {
        validateOrNull(
            bitmap = bitmap,
            requestEdgePx = requestEdgePx,
            payloadBudgetBytes = payloadBudgetBytes,
        )
    } catch (_: Exception) {
        null
    }

    private fun validateOrNull(
        bitmap: Bitmap,
        requestEdgePx: Int,
        payloadBudgetBytes: Long,
    ): Bitmap? {
        if (bitmap.isRecycled || requestEdgePx < 1 || payloadBudgetBytes < 1L) {
            return null
        }
        var candidate = if (bitmap.config == Bitmap.Config.ARGB_8888) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return null
        }
        val currentEdgePx = max(candidate.width, candidate.height)
        if (currentEdgePx > requestEdgePx) {
            candidate = rerasterizeToEdge(candidate, requestEdgePx)
        }
        val allocationByteCount = candidate.allocationByteCount.toLong()
        if (allocationByteCount > payloadBudgetBytes) {
            val candidateEdgePx = max(candidate.width, candidate.height)
            val reductionScale = sqrt(payloadBudgetBytes.toDouble() / allocationByteCount.toDouble())
            val reducedEdgePx = (candidateEdgePx * reductionScale)
                .toInt()
                .coerceAtMost(candidateEdgePx - 1)
            if (reducedEdgePx < 1) {
                return null
            }
            candidate = rerasterizeToEdge(candidate, reducedEdgePx)
        }
        return candidate.takeIf {
            it.config == Bitmap.Config.ARGB_8888 &&
                it.config != Bitmap.Config.HARDWARE &&
                it.width <= requestEdgePx &&
                it.height <= requestEdgePx &&
                it.allocationByteCount.toLong() <= payloadBudgetBytes
        }
    }

    private fun rerasterizeToEdge(bitmap: Bitmap, targetEdgePx: Int): Bitmap {
        val currentEdgePx = max(bitmap.width, bitmap.height)
        val scale = targetEdgePx.toDouble() / currentEdgePx.toDouble()
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
