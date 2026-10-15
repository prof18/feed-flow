package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.utils.ContentDirection
import com.prof18.feedflow.desktop.testing.FeedItemE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys.Actions
import org.jetbrains.jewel.ui.icons.AllIconsKeys.Nodes
import org.jetbrains.jewel.ui.component.ContextMenuRepresentation as DesktopContextMenuRepresentation

private const val ArticleHeroAspectRatio = 16f / 9f

// Read articles dim every text line and the image to one level: 0.65 is the faintest that keeps 4.5:1 text
// contrast on the light theme (the tightest one); together with regular weight and no unread dot it makes read
// rows clearly recede. Unread rows keep their lighter secondary tones.
private const val ArticleReadAlpha = 0.65f
private const val ArticleSourceAlpha = 0.8f
private const val ArticleDescriptionAlpha = 0.82f
private const val ArticleDateAlpha = 0.8f
private val InlineActionSize = 24.dp

@Composable
internal fun ArticleRow(
    feedItem: FeedItem,
    feedFontSize: FeedFontSizes,
    shareMenuLabel: String,
    shareCommentsMenuLabel: String,
    feedLayout: FeedLayout,
    currentFeedFilter: FeedFilter,
    feedItemDisplaySettings: FeedItemDisplaySettings,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onOpenInBrowser: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    onOpenFeedSettings: (FeedSource) -> Unit,
    onOpenFeedWebsite: (String) -> Unit,
    onShareClick: (FeedItemUrlTitle) -> Unit,
    onMarkAllAboveAsRead: (String) -> Unit,
    onMarkAllBelowAsRead: (String) -> Unit,
    modifier: Modifier = Modifier,
    isGridCell: Boolean = false,
    isSelected: Boolean = false,
) {
    val strings = LocalFeedFlowStrings.current
    val readAlpha = feedItem.articleReadAlpha(currentFeedFilter)
    val isHero = feedLayout == FeedLayout.BIG_IMAGE || feedLayout == FeedLayout.GRID
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isFocused by remember { mutableStateOf(false) }
    // A click also focuses the row; the focus ring is for keyboard navigation, so a pointer focus hides it.
    var isFocusedByPointer by remember { mutableStateOf(false) }
    val showFocusRing = isFocused && !isFocusedByPointer
    val cardShape = RoundedCornerShape(DesktopDesignTokens.selectionCornerSize)
    var pendingConfirmation by remember(feedItem.id) { mutableStateOf<MarkAllDirection?>(null) }

    val contextMenuState = remember(feedItem.id) { ContextMenuState() }
    val menuAnchor = with(LocalDensity.current) { DesktopDesignTokens.spaceSection.toPx() }
    val latestMarkAbove by rememberUpdatedState(onMarkAllAboveAsRead)
    val latestMarkBelow by rememberUpdatedState(onMarkAllBelowAsRead)
    val footer: @Composable () -> Unit = {
        ArticleFooterLine(
            feedItem = feedItem,
            feedFontSize = feedFontSize,
            readAlpha = readAlpha,
            showActions = isHovered || isFocused,
            onBookmarkClick = onBookmarkClick,
            onReadStatusClick = onReadStatusClick,
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalContextMenuRepresentation provides DesktopContextMenuRepresentation,
    ) {
        ContextMenuArea(
            state = contextMenuState,
            items = {
                buildArticleContextMenu(
                    feedItem = feedItem,
                    strings = strings,
                    shareMenuLabel = shareMenuLabel,
                    shareCommentsMenuLabel = shareCommentsMenuLabel,
                    onOpenInBrowser = onOpenInBrowser,
                    onBookmarkClick = onBookmarkClick,
                    onReadStatusClick = onReadStatusClick,
                    onCommentClick = onCommentClick,
                    onOpenFeedSettings = onOpenFeedSettings,
                    onOpenFeedWebsite = onOpenFeedWebsite,
                    onShareClick = onShareClick,
                    onMarkAllAboveAsRead = { pendingConfirmation = MarkAllDirection.Above },
                    onMarkAllBelowAsRead = { pendingConfirmation = MarkAllDirection.Below },
                )
            },
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .then(articleLayoutModifier(feedLayout, isGridCell))
                    .then(articleSurfaceModifier(feedLayout, isHovered, showFocusRing, cardShape))
                    .articleSelection(isSelected, cardShape)
                    .testTag(FeedItemE2eIds.row(feedItem.id))
                    .semantics {
                        selected = isSelected
                        if (feedItem.isBookmarked) stateDescription = strings.articleBookmarkedContentDescription
                    }
                    .onPointerPress { isFocusedByPointer = true }
                    .onFocusChanged {
                        isFocused = it.hasFocus
                        isFocusedByPointer = isFocusedByPointer && it.hasFocus
                    }
                    .articleContextMenuKeys(contextMenuState, menuAnchor)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        onFeedItemClick(feedItem.toArticleUrlInfo())
                    }
                    .padding(horizontal = if (isHero) 0.dp else 16.dp),
            ) {
                CompositionDirection(feedItem) {
                    if (isHero) {
                        ArticleHero(
                            feedItem = feedItem,
                            isGridCell = isGridCell || feedLayout == FeedLayout.GRID,
                            feedFontSize = feedFontSize,
                            feedItemDisplaySettings = feedItemDisplaySettings,
                            readAlpha = readAlpha,
                            currentFeedFilter = currentFeedFilter,
                            footer = footer,
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = DesktopDesignTokens.spaceSection),
                        ) {
                            ArticleSourceLine(
                                feedItem = feedItem,
                                feedFontSize = feedFontSize,
                                feedItemDisplaySettings = feedItemDisplaySettings,
                                readAlpha = readAlpha,
                                isCardRow = false,
                                trailing = {
                                    ArticleRowTrailing(
                                        feedItem = feedItem,
                                        feedFontSize = feedFontSize,
                                        readAlpha = readAlpha,
                                        showActions = isHovered || isFocused,
                                        onBookmarkClick = onBookmarkClick,
                                        onReadStatusClick = onReadStatusClick,
                                    )
                                },
                            )
                            Row(
                                modifier = Modifier
                                    .height(IntrinsicSize.Min)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    ArticleTitleAndDescription(
                                        feedItem = feedItem,
                                        feedFontSize = feedFontSize,
                                        feedItemDisplaySettings = feedItemDisplaySettings,
                                        currentFeedFilter = currentFeedFilter,
                                        isGridCell = false,
                                    )
                                }
                                feedItem.imageUrl?.let { imageUrl ->
                                    ArticleThumbnail(
                                        imageUrl = imageUrl,
                                        feedItem = feedItem,
                                        currentFeedFilter = currentFeedFilter,
                                    )
                                }
                            }
                        }
                    }
                }
                if (feedLayout == FeedLayout.LIST) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.5f)),
                    )
                }
            }
        }
    }

    pendingConfirmation?.let { direction ->
        MarkAllConfirmation(
            direction = direction,
            onDismiss = { pendingConfirmation = null },
            onConfirm = {
                if (pendingConfirmation == direction) {
                    pendingConfirmation = null
                    if (direction == MarkAllDirection.Above) {
                        latestMarkAbove(feedItem.id)
                    } else {
                        latestMarkBelow(feedItem.id)
                    }
                }
            },
        )
    }
}

// Card layouts sit on a tinted container so the panel-colored cards stand out, in dark themes too.
@Composable
internal fun articleCardContainerColor(): Color =
    JewelTheme.globalColors.text.normal.copy(alpha = 0.045f).compositeOver(JewelTheme.globalColors.panelBackground)

@Composable
private fun Modifier.articleSelection(isSelected: Boolean, shape: RoundedCornerShape): Modifier =
    if (isSelected) background(JewelTheme.globalColors.outlines.focused.copy(alpha = 0.12f), shape) else this

private fun Modifier.onPointerPress(onPress: () -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            if (awaitPointerEvent(PointerEventPass.Initial).type == PointerEventType.Press) onPress()
        }
    }
}

private fun Modifier.articleContextMenuKeys(state: ContextMenuState, menuAnchor: Float): Modifier =
    onPreviewKeyEvent { event ->
        if (
            event.type == KeyEventType.KeyDown &&
            (event.key == Key.Menu || event.key == Key.F10 && event.isShiftPressed)
        ) {
            state.status = ContextMenuState.Status.Open(
                Rect(menuAnchor, menuAnchor, menuAnchor, menuAnchor),
            )
            true
        } else {
            false
        }
    }

private fun articleLayoutModifier(feedLayout: FeedLayout, isGridCell: Boolean): Modifier =
    when (feedLayout) {
        FeedLayout.CARD -> Modifier.padding(4.dp)
        FeedLayout.BIG_IMAGE -> if (isGridCell) Modifier else Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        FeedLayout.GRID, FeedLayout.LIST -> Modifier
    }

@Composable
private fun articleSurfaceModifier(
    feedLayout: FeedLayout,
    isHovered: Boolean,
    isFocused: Boolean,
    cardShape: RoundedCornerShape,
): Modifier = when {
    feedLayout != FeedLayout.LIST ->
        Modifier
            .shadow(elevation = 1.dp, shape = cardShape)
            .clip(cardShape)
            .background(JewelTheme.globalColors.panelBackground)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) {
                    JewelTheme.globalColors.outlines.focused
                } else {
                    JewelTheme.globalColors.borders.normal.copy(alpha = 0.35f)
                },
                shape = cardShape,
            )
    isFocused -> Modifier.border(width = 2.dp, color = JewelTheme.globalColors.outlines.focused, shape = cardShape)
    isHovered -> Modifier.background(JewelTheme.globalColors.text.normal.copy(alpha = 0.04f), cardShape)
    else -> Modifier
}

@Composable
private fun MarkAllConfirmation(
    direction: MarkAllDirection,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    ConfirmationDialog(
        title = if (direction == MarkAllDirection.Above) {
            strings.markAllAboveAsReadConfirmationTitle
        } else {
            strings.markAllBelowAsReadConfirmationTitle
        },
        message = if (direction == MarkAllDirection.Above) {
            strings.markAllAboveAsReadConfirmationMessage
        } else {
            strings.markAllBelowAsReadConfirmationMessage
        },
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
private fun CompositionDirection(feedItem: FeedItem, content: @Composable () -> Unit) {
    val direction = when (feedItem.contentDirection) {
        ContentDirection.LEFT_TO_RIGHT -> LayoutDirection.Ltr
        ContentDirection.RIGHT_TO_LEFT -> LayoutDirection.Rtl
        null -> LocalLayoutDirection.current
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides direction, content = content)
}

private fun FeedItem.hasSourceLineMarkers(): Boolean = isBookmarked || audioUrl != null

@Composable
private fun ArticleAudioBadge(feedItem: FeedItem) {
    if (feedItem.audioUrl == null) return
    Icon(
        painter = rememberVectorPainter(Icons.Outlined.Headphones),
        contentDescription = LocalFeedFlowStrings.current.audioEpisodeBadgeContentDescription,
        tint = JewelTheme.globalColors.text.info,
        modifier = Modifier
            .padding(end = 4.dp)
            .testTag(FeedItemE2eIds.audio(feedItem.id))
            .size(15.dp),
    )
}

@Composable
private fun ArticleSourceLine(
    feedItem: FeedItem,
    feedFontSize: FeedFontSizes,
    feedItemDisplaySettings: FeedItemDisplaySettings,
    readAlpha: Float,
    isCardRow: Boolean,
    trailing: (@Composable () -> Unit)? = null,
) {
    val showUnread = !feedItem.isRead && !feedItemDisplaySettings.isHideUnreadDotEnabled
    val showSource = !feedItemDisplaySettings.isHideFeedSourceEnabled
    if (!showUnread && !showSource && !feedItem.hasSourceLineMarkers() && trailing == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (trailing != null) Modifier.heightIn(min = InlineActionSize) else Modifier)
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showUnread && !isCardRow) {
            Box(
                Modifier.padding(end = 6.dp).size(8.dp).clip(CircleShape)
                    .background(JewelTheme.globalColors.outlines.focused),
            )
        }
        if (showSource) {
            if (isCardRow) {
                FeedSourceLogo(feedItem)
                Spacer(Modifier.width(DesktopDesignTokens.spaceMedium))
            }
            Text(
                text = feedItem.feedSource.title,
                modifier = Modifier.weight(1f),
                fontSize = feedFontSize.feedMetaFontSize.sp,
                style = DesktopDesignTokens.metadataText,
                lineHeight = (feedFontSize.feedMetaFontSize + 6).sp,
                color = JewelTheme.globalColors.text.normal.copy(
                    alpha = articleTextAlpha(readAlpha, ArticleSourceAlpha),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ArticleAudioBadge(feedItem)
        if (feedItem.isBookmarked) {
            Icon(
                key = Nodes.Bookmark,
                contentDescription = null,
                modifier = Modifier
                    .testTag("article_bookmarked_${feedItem.id}")
                    .size(15.dp),
            )
        }
        if (showUnread && isCardRow) {
            Box(
                Modifier.padding(start = 6.dp).size(8.dp).clip(CircleShape)
                    .background(JewelTheme.globalColors.outlines.focused),
            )
        }
        if (trailing != null) {
            if (!showSource) Spacer(Modifier.weight(1f))
            Box(Modifier.padding(start = 8.dp)) { trailing() }
        }
    }
}

@Composable
private fun FeedSourceLogo(feedItem: FeedItem) {
    val logoUrl = feedItem.feedSource.logoUrl
    Box(
        modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (logoUrl == null) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(JewelTheme.globalColors.text.normal.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("•", color = JewelTheme.globalColors.text.normal)
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(logoUrl).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ArticleTitleAndDescription(
    feedItem: FeedItem,
    feedFontSize: FeedFontSizes,
    feedItemDisplaySettings: FeedItemDisplaySettings,
    currentFeedFilter: FeedFilter,
    isGridCell: Boolean,
) {
    val alpha = feedItem.articleReadAlpha(currentFeedFilter)
    Column {
        feedItem.title?.let { title ->
            Text(
                text = title,
                fontSize = feedFontSize.feedTitleFontSize.sp,
                fontWeight = if (feedItem.isRead) FontWeight.Normal else FontWeight.SemiBold,
                lineHeight = (feedFontSize.feedTitleFontSize + 4).sp,
                color = JewelTheme.globalColors.text.normal.copy(alpha = alpha),
                maxLines = if (isGridCell) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        feedItem.subtitle?.let { subtitle ->
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = feedItemDisplaySettings.descriptionLineLimit.lines,
                overflow = TextOverflow.Ellipsis,
                fontSize = feedFontSize.feedDescFontSize.sp,
                lineHeight = (feedFontSize.feedDescFontSize + 6).sp,
                color = JewelTheme.globalColors.text.normal.copy(
                    alpha = articleTextAlpha(alpha, ArticleDescriptionAlpha),
                ),
            )
        }
    }
}

@Composable
private fun ArticleThumbnail(feedItem: FeedItem, imageUrl: String, currentFeedFilter: FeedFilter) {
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current).data(imageUrl).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        colorFilter = feedItem.articleImageColorFilter(currentFeedFilter),
        modifier = Modifier
            .padding(start = 16.dp)
            .size(96.dp)
            .clip(RoundedCornerShape(4.dp))
            .alpha(feedItem.articleImageAlpha(currentFeedFilter))
            .testTag(FeedItemE2eIds.image(feedItem.id)),
    )
}

@Composable
private fun ArticleHero(
    feedItem: FeedItem,
    isGridCell: Boolean,
    feedFontSize: FeedFontSizes,
    feedItemDisplaySettings: FeedItemDisplaySettings,
    readAlpha: Float,
    footer: @Composable () -> Unit,
    currentFeedFilter: FeedFilter,
) {
    Column(Modifier.fillMaxWidth()) {
        feedItem.imageUrl?.let { imageUrl ->
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(imageUrl).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = feedItem.articleImageColorFilter(currentFeedFilter),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ArticleHeroAspectRatio)
                    .alpha(feedItem.articleImageAlpha(currentFeedFilter))
                    .testTag(FeedItemE2eIds.image(feedItem.id)),
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            ArticleSourceLine(
                feedItem = feedItem,
                feedFontSize = feedFontSize,
                feedItemDisplaySettings = feedItemDisplaySettings,
                readAlpha = readAlpha,
                isCardRow = true,
            )
            ArticleTitleAndDescription(
                feedItem = feedItem,
                feedFontSize = feedFontSize,
                feedItemDisplaySettings = feedItemDisplaySettings,
                currentFeedFilter = currentFeedFilter,
                isGridCell = isGridCell,
            )
            footer()
        }
    }
}

@Composable
private fun ArticleFooterLine(
    feedItem: FeedItem,
    feedFontSize: FeedFontSizes,
    readAlpha: Float,
    showActions: Boolean,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { ArticleDate(feedItem, feedFontSize, readAlpha) }
        Box(Modifier.size(width = 64.dp, height = DesktopDesignTokens.iconActionMinSize)) {
            if (showActions) {
                ArticleHoverActions(
                    feedItem = feedItem,
                    buttonSize = DesktopDesignTokens.iconActionMinSize,
                    onBookmarkClick = onBookmarkClick,
                    onReadStatusClick = onReadStatusClick,
                )
            }
        }
    }
}

// List rows show the date until the row is hovered or focused, then swap it for the quick actions.
@Composable
private fun ArticleRowTrailing(
    feedItem: FeedItem,
    feedFontSize: FeedFontSizes,
    readAlpha: Float,
    showActions: Boolean,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
) {
    if (showActions) {
        ArticleHoverActions(
            feedItem = feedItem,
            buttonSize = InlineActionSize,
            onBookmarkClick = onBookmarkClick,
            onReadStatusClick = onReadStatusClick,
        )
    } else {
        ArticleInlineDate(feedItem, feedFontSize, readAlpha)
    }
}

@Composable
private fun ArticleHoverActions(
    feedItem: FeedItem,
    buttonSize: Dp,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Row {
        IconButton(
            onClick = { onBookmarkClick(FeedItemId(feedItem.id), !feedItem.isBookmarked) },
            modifier = Modifier.size(buttonSize),
        ) {
            Icon(
                key = Nodes.Bookmark,
                contentDescription = if (feedItem.isBookmarked) {
                    strings.menuRemoveFromBookmark
                } else {
                    strings.menuAddToBookmark
                },
                tint = if (feedItem.isBookmarked) {
                    JewelTheme.globalColors.outlines.focused
                } else {
                    JewelTheme.globalColors.text.info
                },
            )
        }
        IconButton(
            onClick = { onReadStatusClick(FeedItemId(feedItem.id), !feedItem.isRead) },
            modifier = Modifier.size(buttonSize),
        ) {
            Icon(
                key = Actions.Checked,
                contentDescription = if (feedItem.isRead) {
                    strings.menuMarkAsUnread
                } else {
                    strings.menuMarkAsRead
                },
                tint = JewelTheme.globalColors.text.info,
            )
        }
    }
}

@Composable
private fun ArticleInlineDate(feedItem: FeedItem, feedFontSize: FeedFontSizes, readAlpha: Float) {
    feedItem.dateString?.let { dateString ->
        Text(
            text = dateString,
            fontSize = feedFontSize.feedMetaFontSize.sp,
            style = DesktopDesignTokens.metadataText,
            lineHeight = (feedFontSize.feedMetaFontSize + 6).sp,
            color = JewelTheme.globalColors.text.normal.copy(alpha = articleTextAlpha(readAlpha, ArticleDateAlpha)),
            maxLines = 1,
        )
    }
}

@Composable
private fun ArticleDate(feedItem: FeedItem, feedFontSize: FeedFontSizes, readAlpha: Float) {
    feedItem.dateString?.let { dateString ->
        Text(
            text = dateString,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            fontSize = feedFontSize.feedMetaFontSize.sp,
            style = DesktopDesignTokens.metadataText,
            lineHeight = (feedFontSize.feedMetaFontSize + 6).sp,
            color = JewelTheme.globalColors.text.normal.copy(alpha = articleTextAlpha(readAlpha, ArticleDateAlpha)),
        )
    }
}

private fun buildArticleContextMenu(
    feedItem: FeedItem,
    strings: com.prof18.feedflow.i18n.FeedFlowStrings,
    shareMenuLabel: String,
    shareCommentsMenuLabel: String,
    onOpenInBrowser: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    onOpenFeedSettings: (FeedSource) -> Unit,
    onOpenFeedWebsite: (String) -> Unit,
    onShareClick: (FeedItemUrlTitle) -> Unit,
    onMarkAllAboveAsRead: (String) -> Unit,
    onMarkAllBelowAsRead: (String) -> Unit,
): List<ContextMenuItem> {
    return buildList {
        add(ContextMenuItem(strings.openFeedSettings) { onOpenFeedSettings(feedItem.feedSource) })
        feedItem.feedSource.websiteUrlFallback()?.let { websiteUrl ->
            add(ContextMenuItem(strings.openFeedWebsiteButton) { onOpenFeedWebsite(websiteUrl) })
        }
        add(ContextMenuItem(strings.menuMarkAllAboveAsRead) { onMarkAllAboveAsRead(feedItem.id) })
        add(ContextMenuItem(strings.menuMarkAllBelowAsRead) { onMarkAllBelowAsRead(feedItem.id) })
        feedItem.url.takeIf(String::isNotBlank)?.let { url ->
            add(ContextMenuItem(shareMenuLabel) { onShareClick(FeedItemUrlTitle(url = url, title = feedItem.title)) })
            add(
                ContextMenuItem(strings.settingsSwipeActionOpenInBrowser) {
                    onOpenInBrowser(
                        feedItem.toArticleUrlInfo().copy(
                            openOnlyOnBrowser = true,
                            articleOpenMode = ArticleOpenMode.PREFERRED_BROWSER,
                        ),
                    )
                    if (!feedItem.isRead) onReadStatusClick(FeedItemId(feedItem.id), true)
                },
            )
        }
        feedItem.commentsUrl?.let { commentsUrl ->
            add(
                ContextMenuItem(shareCommentsMenuLabel) {
                    onShareClick(FeedItemUrlTitle(url = commentsUrl, title = feedItem.title))
                },
            )
            add(
                ContextMenuItem(strings.menuOpenComments) {
                    onCommentClick(
                        FeedItemUrlInfo(
                            id = feedItem.id,
                            url = commentsUrl,
                            title = feedItem.title,
                            openOnlyOnBrowser = true,
                            isBookmarked = feedItem.isBookmarked,
                            articleOpenMode = ArticleOpenMode.PREFERRED_BROWSER,
                            commentsUrl = feedItem.commentsUrl,
                        ),
                    )
                },
            )
        }
        add(
            ContextMenuItem(
                if (feedItem.isBookmarked) strings.menuRemoveFromBookmark else strings.menuAddToBookmark,
            ) { onBookmarkClick(FeedItemId(feedItem.id), !feedItem.isBookmarked) },
        )
        add(
            ContextMenuItem(
                if (feedItem.isRead) strings.menuMarkAsUnread else strings.menuMarkAsRead,
            ) { onReadStatusClick(FeedItemId(feedItem.id), !feedItem.isRead) },
        )
    }
}

private fun FeedItem.toArticleUrlInfo() =
    FeedItemUrlInfo(
        id = id,
        url = url,
        title = title,
        isBookmarked = isBookmarked,
        articleOpenMode = feedSource.articleOpenMode,
        commentsUrl = commentsUrl,
        imageUrl = imageUrl,
        feedSourceTitle = feedSource.title,
    )

// Dimming only helps where read and unread articles mix. The Read view holds read articles only, so dimming every
// row there would add no information and just lower readability; Bookmarks keeps saved articles at full contrast.
private fun FeedItem.isDimmedAsRead(currentFeedFilter: FeedFilter): Boolean =
    isRead && currentFeedFilter !is FeedFilter.Read && currentFeedFilter !is FeedFilter.Bookmarks

private fun FeedItem.articleReadAlpha(currentFeedFilter: FeedFilter): Float =
    if (isDimmedAsRead(currentFeedFilter)) ArticleReadAlpha else 1f

private fun articleTextAlpha(readAlpha: Float, unreadAlpha: Float): Float =
    if (readAlpha < 1f) readAlpha else unreadAlpha

private fun FeedItem.articleImageAlpha(currentFeedFilter: FeedFilter): Float =
    if (isDimmedAsRead(currentFeedFilter)) ArticleReadAlpha else 1f

// Read images also lose most of their color: fading alone barely shows on dark themes, while desaturation is
// obvious at a glance and costs no text readability.
private fun FeedItem.articleImageColorFilter(currentFeedFilter: FeedFilter): ColorFilter? =
    if (isDimmedAsRead(currentFeedFilter)) ReadImageColorFilter else null

private val ReadImageColorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.3f) })

private enum class MarkAllDirection { Above, Below }
