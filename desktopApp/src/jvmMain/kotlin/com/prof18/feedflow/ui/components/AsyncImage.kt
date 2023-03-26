package com.prof18.feedflow.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.res.loadXmlImageVector
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xml.sax.InputSource
import java.io.IOException
import java.net.URL

@Composable
fun <T> AsyncImage(
    load: suspend () -> T,
    painterFor: @Composable (T) -> Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val imageData: ImageData<T> by produceState<ImageData<T>>(ImageData.Loading) {
        value = withContext(Dispatchers.IO) {
            try {
                val result = load()
                ImageData.Success(result)
            } catch (e: Exception) {
                ImageData.Error
            }
        }
    }

    when (imageData) {
        ImageData.Loading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

        }

        is ImageData.Success -> {
            Image(
                painter = painterFor((imageData as ImageData.Success<T>).data),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }

        ImageData.Error -> {}
    }
}

sealed class ImageData<out T> {
    object Loading : ImageData<Nothing>()
    data class Success<T>(val data: T) : ImageData<T>()
    object Error : ImageData<Nothing>()
}

fun loadImageBitmap(url: String): ImageBitmap =
    URL(url).openStream().buffered().use(::loadImageBitmap)

fun loadSvgPainter(url: String, density: Density): Painter =
    URL(url).openStream().buffered().use { loadSvgPainter(it, density) }

fun loadXmlImageVector(url: String, density: Density): ImageVector =
    URL(url).openStream().buffered().use { loadXmlImageVector(InputSource(it), density) }