package com.prof18.feedflow.android.widget.components

import android.content.Context
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
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
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
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.MainActivity
import com.prof18.feedflow.android.widget.WidgetFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.shared.ui.style.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun WidgetFeedItemList(
    feedItem: FeedItem,
    browserManager: BrowserManager,
    fontSizes: WidgetFontSizes,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current.applicationContext
    val clickAction = createFeedItemClickAction(feedItem, context, browserManager)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(clickAction),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Content(feedItem, fontSizes)
    }
}

@Composable
internal fun WidgetFeedItemCard(
    feedItem: FeedItem,
    browserManager: BrowserManager,
    fontSizes: WidgetFontSizes,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current.applicationContext
    val clickAction = createFeedItemClickAction(feedItem, context, browserManager)

    Column(modifier = modifier) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(16.dp)
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .clickable(clickAction),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Content(feedItem, fontSizes)
        }
        Spacer(GlanceModifier.height(8.dp))
    }
}

@Composable
private fun RowScope.Content(
    feedItem: FeedItem,
    fontSizes: WidgetFontSizes,
    modifier: GlanceModifier = GlanceModifier,
) {
    val modifier = modifier.defaultWeight()

    Column(
        modifier = modifier
            .padding(end = Spacing.regular),
    ) {
        val fontStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = fontSizes.meta.sp,
            color = (GlanceTheme.colors.onSurface),
        )

        Row {
            Text(
                text = feedItem.feedSource.title,
                style = fontStyle,
            )
        }
        Text(
            text = feedItem.title.orEmpty(),
            maxLines = 2,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = fontSizes.title.sp,
                color = (GlanceTheme.colors.onSurface),
            ),
        )

        feedItem.dateString?.let { dateString ->
            Text(
                modifier = GlanceModifier.padding(top = Spacing.xsmall),
                text = dateString,
                style = fontStyle,
            )
        }
    }

    feedItem.imageUrl?.let { imageUrl ->
        FeedItemImage(imageUrl)
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

private fun createFeedItemClickAction(
    feedItem: FeedItem,
    context: Context,
    browserManager: BrowserManager,
): Action = when (feedItem.feedSource.linkOpeningPreference) {
    LinkOpeningPreference.READER_MODE -> {
        createDeepLinkAction(feedItem, context)
    }
    LinkOpeningPreference.INTERNAL_BROWSER, LinkOpeningPreference.PREFERRED_BROWSER -> {
        createBrowserAction(feedItem, browserManager)
    }
    LinkOpeningPreference.DEFAULT -> {
        if (browserManager.openReaderMode() && !feedItem.shouldOpenInBrowser()) {
            createDeepLinkAction(feedItem, context)
        } else {
            createBrowserAction(feedItem, browserManager)
        }
    }
}

private fun createBrowserAction(feedItem: FeedItem, browserManager: BrowserManager): Action {
    val intent = Intent(Intent.ACTION_VIEW, feedItem.url.toUri()).apply {
        browserManager.getBrowserPackageNameWithoutInApp()?.let { packageName ->
            setPackage(packageName)
        }
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    return actionStartActivity(intent)
}

private fun createDeepLinkAction(feedItem: FeedItem, context: Context): Action {
    return actionStartActivity(
        Intent(
            context,
            MainActivity::class.java,
        )
            .setAction(Intent.ACTION_VIEW)
            .setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            .setData("feedflow://feed/${feedItem.id}".toUri()),
    )
}

private fun FeedItem.shouldOpenInBrowser(): Boolean =
    url.contains("type=pdf") || url.contains("youtube.com") ||
        feedSource.linkOpeningPreference == LinkOpeningPreference.PREFERRED_BROWSER
