package com.prof18.feedflow.android.widget.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.MainActivity
import com.prof18.feedflow.android.R
import com.prof18.feedflow.android.widget.ResolvedWidgetCardLayout
import com.prof18.feedflow.android.widget.ResolvedWidgetListImageLayout
import com.prof18.feedflow.android.widget.WIDGET_THUMBNAIL_VIEWPORT_DP
import com.prof18.feedflow.android.widget.WidgetColorProviderSource
import com.prof18.feedflow.android.widget.WidgetFontSizes
import com.prof18.feedflow.android.widget.WidgetImageBudgetPolicy
import com.prof18.feedflow.android.widget.createPreSWidgetCardSlabRemoteViews
import com.prof18.feedflow.android.widget.usesResourceBackedWidgetCardSlab
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.ReaderModeEligibility
import com.prof18.feedflow.core.model.isReaderMode
import com.prof18.feedflow.core.model.resolveWith
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetCardImageSizing
import com.prof18.feedflow.shared.domain.model.WidgetCardItemSeparation
import com.prof18.feedflow.shared.ui.style.Spacing

private const val THUMBNAIL_CORNER_RADIUS_DP = 8

@Composable
internal fun WidgetFeedItemList(
    feedItem: FeedItem,
    browserManager: BrowserManager,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    primaryTextColor: ColorProvider,
    secondaryTextColor: ColorProvider,
    imageBudgetPolicy: WidgetImageBudgetPolicy,
    imageLayout: ResolvedWidgetListImageLayout,
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
        ThumbnailContent(
            feedItem = feedItem,
            fontSizes = fontSizes,
            hideImages = hideImages,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            imageBudgetPolicy = imageBudgetPolicy,
            imageDisplayTargetPx = imageLayout.displayTargetPx,
            imageDisplayViewportDp = imageLayout.displayViewportDp,
            imageRenderPolicy = WidgetArticleImageRenderPolicy.GLANCE_ONLY,
        )
    }
}

@Composable
internal fun WidgetFeedItemCard(
    feedItem: FeedItem,
    browserManager: BrowserManager,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    appearance: WidgetCardAppearance,
    slabFillColor: ColorProvider?,
    slabFillColorSource: WidgetColorProviderSource,
    resolvedSlabFillColor: Color?,
    primaryTextColor: ColorProvider,
    secondaryTextColor: ColorProvider,
    cardLayout: ResolvedWidgetCardLayout,
    imageBudgetPolicy: WidgetImageBudgetPolicy,
    imageDisplayTargetPx: Int,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current.applicationContext
    val clickAction = createFeedItemClickAction(feedItem, context, browserManager)

    if (appearance.itemSeparation == WidgetCardItemSeparation.SPACING) {
        Box(
            modifier = modifier
                .padding(vertical = Spacing.xsmall),
        ) {
            WidgetFeedItemCardSlab(
                feedItem = feedItem,
                fontSizes = fontSizes,
                hideImages = hideImages,
                appearance = appearance,
                slabFillColor = slabFillColor,
                slabFillColorSource = slabFillColorSource,
                resolvedSlabFillColor = resolvedSlabFillColor,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
                cardLayout = cardLayout,
                imageBudgetPolicy = imageBudgetPolicy,
                imageDisplayTargetPx = imageDisplayTargetPx,
                clickAction = clickAction,
            )
        }
    } else {
        WidgetFeedItemCardSlab(
            feedItem = feedItem,
            fontSizes = fontSizes,
            hideImages = hideImages,
            appearance = appearance,
            slabFillColor = slabFillColor,
            slabFillColorSource = slabFillColorSource,
            resolvedSlabFillColor = resolvedSlabFillColor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            cardLayout = cardLayout,
            imageBudgetPolicy = imageBudgetPolicy,
            imageDisplayTargetPx = imageDisplayTargetPx,
            clickAction = clickAction,
            modifier = modifier,
        )
    }
}

@Composable
private fun WidgetFeedItemCardSlab(
    feedItem: FeedItem,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    appearance: WidgetCardAppearance,
    slabFillColor: ColorProvider?,
    slabFillColorSource: WidgetColorProviderSource,
    resolvedSlabFillColor: Color?,
    primaryTextColor: ColorProvider,
    secondaryTextColor: ColorProvider,
    cardLayout: ResolvedWidgetCardLayout,
    imageBudgetPolicy: WidgetImageBudgetPolicy,
    imageDisplayTargetPx: Int,
    clickAction: Action,
    modifier: GlanceModifier = GlanceModifier,
) {
    when (cardLayout.imageSizing) {
        WidgetCardImageSizing.THUMBNAIL -> WidgetCardSlabRow(
            cornerRadiusDp = appearance.cornerRadiusDp,
            slabFillColor = slabFillColor,
            slabFillColorSource = slabFillColorSource,
            resolvedSlabFillColor = resolvedSlabFillColor,
            clickAction = clickAction,
            contentPaddingDp = 16,
            modifier = modifier,
        ) {
            ThumbnailContent(
                feedItem = feedItem,
                fontSizes = fontSizes,
                hideImages = hideImages,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
                imageBudgetPolicy = imageBudgetPolicy,
                imageDisplayTargetPx = imageDisplayTargetPx,
                imageRenderPolicy = WidgetArticleImageRenderPolicy.CARD_COMPATIBLE,
            )
        }
        WidgetCardImageSizing.FILL_ROW_HEIGHT -> WidgetCardSlabRow(
            cornerRadiusDp = appearance.cornerRadiusDp,
            slabFillColor = slabFillColor,
            slabFillColorSource = slabFillColorSource,
            resolvedSlabFillColor = resolvedSlabFillColor,
            clickAction = clickAction,
            rowHeightDp = requireNotNull(cardLayout.fixedRowHeightDp),
            modifier = modifier,
        ) {
            FillContent(
                feedItem = feedItem,
                fontSizes = fontSizes,
                hideImages = hideImages,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
                imageBudgetPolicy = imageBudgetPolicy,
                imageDisplayTargetPx = imageDisplayTargetPx,
                rowHeightDp = cardLayout.imageViewportDp,
                cornerRadiusDp = appearance.cornerRadiusDp,
            )
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun WidgetCardSlabRow(
    cornerRadiusDp: Int,
    slabFillColor: ColorProvider?,
    slabFillColorSource: WidgetColorProviderSource,
    resolvedSlabFillColor: Color?,
    clickAction: Action,
    modifier: GlanceModifier = GlanceModifier,
    rowHeightDp: Int? = null,
    contentPaddingDp: Int? = null,
    content: @Composable RowScope.() -> Unit,
) {
    var sizeModifier = modifier.fillMaxWidth()
    rowHeightDp?.let { sizeModifier = sizeModifier.height(it.dp) }

    if (usesResourceBackedWidgetCardSlab(Build.VERSION.SDK_INT)) {
        AndroidRemoteViews(
            remoteViews = createPreSWidgetCardSlabRemoteViews(
                context = LocalContext.current,
                cornerRadiusDp = cornerRadiusDp,
                colorSource = slabFillColorSource,
                resolvedSlabFillColor = resolvedSlabFillColor,
            ),
            containerViewId = R.id.widget_card_slab_content,
            modifier = sizeModifier.clickable(clickAction),
        ) {
            var rowModifier = GlanceModifier.fillMaxWidth()
            rowHeightDp?.let { rowModifier = rowModifier.height(it.dp) }
            contentPaddingDp?.let { rowModifier = rowModifier.padding(it.dp) }
            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
        return
    }

    contentPaddingDp?.let { sizeModifier = sizeModifier.padding(it.dp) }
    val slabModifier = sizeModifier.cornerRadius(cornerRadiusDp.dp)
        .background(slabFillColor ?: ColorProvider(Color.Transparent))
    Row(
        modifier = slabModifier.clickable(clickAction),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun RowScope.ThumbnailContent(
    feedItem: FeedItem,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    primaryTextColor: ColorProvider,
    secondaryTextColor: ColorProvider,
    imageBudgetPolicy: WidgetImageBudgetPolicy,
    imageDisplayTargetPx: Int,
    imageRenderPolicy: WidgetArticleImageRenderPolicy,
    modifier: GlanceModifier = GlanceModifier,
    imageDisplayViewportDp: Int = WIDGET_THUMBNAIL_VIEWPORT_DP,
) {
    val textModifier = modifier.defaultWeight()

    Column(
        modifier = textModifier
            .padding(end = Spacing.regular),
    ) {
        val fontStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = fontSizes.meta.sp,
            color = secondaryTextColor,
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
                color = primaryTextColor,
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

    ArticleImageIfAvailable(
        feedItem = feedItem,
        hideImages = hideImages,
        imageBudgetPolicy = imageBudgetPolicy,
        imageDisplayTargetPx = imageDisplayTargetPx,
        displayViewportDp = imageDisplayViewportDp,
        cornerRadiusDp = THUMBNAIL_CORNER_RADIUS_DP,
        renderPolicy = imageRenderPolicy,
    )
}

@Composable
private fun RowScope.FillContent(
    feedItem: FeedItem,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    primaryTextColor: ColorProvider,
    secondaryTextColor: ColorProvider,
    imageBudgetPolicy: WidgetImageBudgetPolicy,
    imageDisplayTargetPx: Int,
    rowHeightDp: Int,
    cornerRadiusDp: Int,
) {
    val fontStyle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = fontSizes.meta.sp,
        color = secondaryTextColor,
    )
    Column(
        modifier = GlanceModifier
            .defaultWeight()
            .padding(16.dp),
    ) {
        Text(
            text = feedItem.feedSource.title,
            maxLines = 1,
            style = fontStyle,
        )
        Text(
            text = feedItem.title.orEmpty(),
            maxLines = 2,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = fontSizes.title.sp,
                color = primaryTextColor,
            ),
        )
        feedItem.dateString?.let { dateString ->
            Text(
                modifier = GlanceModifier.padding(top = Spacing.xsmall),
                text = dateString,
                maxLines = 1,
                style = fontStyle,
            )
        }
    }

    ArticleImageIfAvailable(
        feedItem = feedItem,
        hideImages = hideImages,
        imageBudgetPolicy = imageBudgetPolicy,
        imageDisplayTargetPx = imageDisplayTargetPx,
        displayViewportDp = rowHeightDp,
        cornerRadiusDp = cornerRadiusDp,
        renderPolicy = WidgetArticleImageRenderPolicy.CARD_COMPATIBLE,
    )
}

@Composable
private fun ArticleImageIfAvailable(
    feedItem: FeedItem,
    hideImages: Boolean,
    imageBudgetPolicy: WidgetImageBudgetPolicy,
    imageDisplayTargetPx: Int,
    displayViewportDp: Int,
    cornerRadiusDp: Int,
    renderPolicy: WidgetArticleImageRenderPolicy,
) {
    if (hideImages) {
        return
    }
    val imageUrl = feedItem.imageUrl?.takeIf(String::isNotBlank) ?: return
    WidgetArticleImage(
        requestPolicy = imageBudgetPolicy.resolveRequest(
            imageUrl = imageUrl,
            displayTargetPx = imageDisplayTargetPx,
        ),
        displayViewportDp = displayViewportDp.dp,
        cornerRadiusDp = cornerRadiusDp.dp,
        renderPolicy = renderPolicy,
    )
}

private fun createFeedItemClickAction(
    feedItem: FeedItem,
    context: Context,
    browserManager: BrowserManager,
): Action {
    // URL-less items can only be shown in the reader; deep-link into the app.
    if (feedItem.url.isBlank()) {
        return createDeepLinkAction(feedItem, context)
    }
    val openMode = feedItem.feedSource.articleOpenMode.resolveWith(browserManager.getArticleOpenMode())
    return if (openMode.isReaderMode() && feedItem.canOpenWebReaderMode()) {
        createDeepLinkAction(feedItem, context)
    } else {
        createBrowserAction(feedItem, browserManager)
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

private fun FeedItem.canOpenWebReaderMode(): Boolean =
    ReaderModeEligibility.canOpenReaderMode(url)
