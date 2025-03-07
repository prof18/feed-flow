package com.prof18.feedflow.android.widget

import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.core.net.toUri
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Precision
import coil3.size.Scale
import com.prof18.feedflow.android.MainActivity
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun Content(feedItems: ImmutableList<FeedItem>) {
    Scaffold(
        titleBar = {
            Text(
                modifier = GlanceModifier
                    .padding(top = Spacing.regular)
                    .padding(bottom = Spacing.small)
                    .padding(horizontal = Spacing.medium)
                    .fillMaxWidth(),
                text = LocalFeedFlowStrings.current.widgetLatestItems,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GlanceTheme.colors.onSurface,
                ),
            )
        },
        backgroundColor = GlanceTheme.colors.widgetBackground,
        modifier = GlanceModifier.fillMaxSize(),
    ) {
        if (feedItems.isEmpty()) {
            Column(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = LocalFeedFlowStrings.current.emptyFeedMessage,
                    style = TextStyle(
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        color = (GlanceTheme.colors.onSurface),
                    ),
                )

                Text(
                    modifier = GlanceModifier.padding(top = Spacing.small),
                    text = LocalFeedFlowStrings.current.widgetCheckFeedSources,
                    style = TextStyle(
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        color = (GlanceTheme.colors.onSurface),
                    ),
                )
            }
        } else {
            LazyColumn {
                items(feedItems) { feedItem ->
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable(
                                actionStartActivity(
                                    Intent(LocalContext.current.applicationContext, MainActivity::class.java)
                                        .setAction(Intent.ACTION_VIEW)
                                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        .setData("feedflow://feed/${feedItem.id}".toUri()),
                                ),

                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val modifier = GlanceModifier.defaultWeight()

                        Column(
                            modifier = modifier
                                .padding(end = Spacing.regular),
                        ) {
                            Row {
                                val fontStyle = TextStyle(
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = (GlanceTheme.colors.onSurface),
                                )
                                Text(
                                    text = feedItem.feedSource.title,
                                    style = fontStyle,
                                )
                                feedItem.dateString?.let { dateString ->
                                    Text(
                                        text = "  •  ",
                                        style = fontStyle,
                                    )
                                    Text(text = dateString, style = fontStyle)
                                }
                            }
                            Text(
                                text = feedItem.title.orEmpty(),
                                maxLines = 2,
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = (GlanceTheme.colors.onSurface),
                                ),
                            )
                        }

                        feedItem.imageUrl?.let { imageUrl ->
                            FeedItemImage(imageUrl, modifier)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedItemImage(imageUrl: String, modifier: GlanceModifier = GlanceModifier) {
    val context = LocalContext.current
    var loadedBitmap by remember(imageUrl) { mutableStateOf<Bitmap?>(null) }

    val sizeDp = 50.dp
    val size = sizeDp.value.dpToPx.toInt()
    LaunchedEffect(imageUrl) {
        withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .precision(Precision.EXACT)
                .size(size, size)
                .scale(Scale.FILL)
                .build()

            loadedBitmap = when (val result = context.imageLoader.execute(request)) {
                is ErrorResult -> null
                is SuccessResult -> result.image.asDrawable(context.resources).toBitmapOrNull()
            }
        }
    }

    if (loadedBitmap != null) {
        Image(
            provider = ImageProvider(loadedBitmap!!),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(sizeDp)
                .cornerRadius(8.dp),
        )
    }
}

private val Float.dpToPx: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics)
