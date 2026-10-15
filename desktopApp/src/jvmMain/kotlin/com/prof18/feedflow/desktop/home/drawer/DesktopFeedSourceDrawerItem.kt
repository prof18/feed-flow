@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.ConfirmationDialog
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.DesktopPopupMenu
import com.prof18.feedflow.desktop.ui.components.DesktopPopupMenuEntry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.foundation.theme.LocalContentColor
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
internal fun DesktopFeedSourceDrawerItem(
    feedSource: FeedSource,
    label: @Composable () -> Unit,
    selected: Boolean,
    drawerItemVisualStyle: DrawerItemVisualStyle,
    isMultiSelected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onChangeFeedCategoryClick: (FeedSource) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onMarkAllReadForFeedSourceClick: (FeedSource) -> Unit,
    unreadCount: Long,
    modifier: Modifier = Modifier,
    onContextClick: () -> Unit = {},
    batchMenuEntries: ImmutableList<DesktopPopupMenuEntry>? = null,
    onPointerModifiersChanged: (PointerKeyboardModifiers) -> Unit = {},
) {
    var showFeedMenu by remember(feedSource.id) { mutableStateOf(false) }
    var menuPositionInWindow by remember(feedSource.id) { mutableStateOf<Offset?>(null) }
    var showDeleteDialog by remember(feedSource.id) { mutableStateOf(false) }
    var itemPosition by remember(feedSource.id) { mutableStateOf(Offset.Zero) }
    val strings = LocalFeedFlowStrings.current
    val itemShape = drawerItemVisualStyle.itemShape
    val selectedColor = drawerItemVisualStyle.selectedContainerColor ?: sidebarSelectedColor()
    val websiteUrl = feedSource.websiteUrlFallback()
    val menuEntries = buildFeedSourceMenuEntries(
        feedSource = feedSource,
        unreadCount = unreadCount,
        websiteUrl = websiteUrl,
        onMarkAllRead = { onMarkAllReadForFeedSourceClick(feedSource) },
        onOpenWebsite = onOpenWebsite,
        onEditFeed = { onEditFeedClick(feedSource) },
        onChangeCategory = { onChangeFeedCategoryClick(feedSource) },
        onPinFeed = { onPinFeedClick(feedSource) },
        onDeleteFeed = { showDeleteDialog = true },
    )
    val interactionSource = remember(feedSource.id) { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val finalBackground = feedSourceBackground(
        selected = selected,
        multiSelected = isMultiSelected,
        highlighted = hovered || focused,
        selectedColor = selectedColor,
        multiSelectedColor = sidebarMultiSelectedColor(),
        highlightColor = sidebarHighlightColor(),
    )
    Row(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                itemPosition = coordinates.positionInWindow()
            }
            .heightIn(min = drawerItemVisualStyle.itemMinHeight)
            .fillMaxWidth()
            .clip(itemShape)
            .background(finalBackground)
            .capturePrimaryModifiers(onPointerModifiersChanged)
            .feedSourceKeyboardContextMenu {
                onContextClick()
                menuPositionInWindow = itemPosition
                showFeedMenu = true
            }
            .selectable(
                selected = selected || isMultiSelected,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics {
                if (feedSource.fetchFailed) stateDescription = strings.feedFetchFailedTooltip
            }
            .feedSourceContextMenu { position ->
                onContextClick()
                menuPositionInWindow = itemPosition + position
                showFeedMenu = true
            }
            .padding(
                start = if (feedSource.fetchFailed) {
                    8.dp
                } else {
                    28.dp
                },
                end = 8.dp,
                top = 4.dp,
                bottom = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            FeedFetchWarning(feedSource.fetchFailed, strings.feedFetchFailedTooltip)
            val content = drawerItemVisualStyle.selectedContentColor.takeIf { selected }
                ?: JewelTheme.globalColors.text.normal
            CompositionLocalProvider(LocalContentColor provides content, content = icon)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                CompositionLocalProvider(LocalContentColor provides content, content = label)
            }
        }
        if (unreadCount > 0) {
            Text(
                unreadCount.toString(),
                modifier = Modifier.padding(start = DesktopDesignTokens.spaceMedium),
                style = DesktopDesignTokens.metadataText,
                color = JewelTheme.globalColors.text.info,
            )
        }
    }
    DesktopPopupMenu(
        showMenu = showFeedMenu,
        menuPositionInWindow = menuPositionInWindow,
        menuEntries = batchMenuEntries ?: menuEntries,
        closeMenu = {
            showFeedMenu = false
            menuPositionInWindow = null
        },
    )
    if (showDeleteDialog) {
        ConfirmationDialog(
            title = strings.deleteFeedConfirmationTitle,
            message = strings.deleteFeedConfirmationMessage,
            onConfirm = {
                onDeleteFeedSourceClick(feedSource)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

private fun feedSourceBackground(
    selected: Boolean,
    multiSelected: Boolean,
    highlighted: Boolean,
    selectedColor: Color,
    multiSelectedColor: Color,
    highlightColor: Color,
): Color = when {
    selected -> selectedColor
    multiSelected -> multiSelectedColor
    highlighted -> highlightColor
    else -> Color.Transparent
}

private fun Modifier.feedSourceContextMenu(onShow: (Offset) -> Unit): Modifier =
    onPointerEvent(PointerEventType.Press) { event ->
        if (event.buttons.isSecondaryPressed) {
            onShow(event.changes.first().position)
            event.changes.forEach { it.consume() }
        }
    }

private fun Modifier.capturePrimaryModifiers(onModifiers: (PointerKeyboardModifiers) -> Unit): Modifier =
    onPointerEvent(PointerEventType.Press, pass = PointerEventPass.Initial) { event ->
        if (event.buttons.isPrimaryPressed) onModifiers(event.keyboardModifiers)
    }

private fun Modifier.feedSourceKeyboardContextMenu(onShow: () -> Unit): Modifier =
    onPreviewKeyEvent { event ->
        val isContextKey = event.key == Key.Menu || (event.key == Key.F10 && event.isShiftPressed)
        if (event.type == KeyEventType.KeyDown && isContextKey) {
            onShow()
            true
        } else {
            false
        }
    }

@Composable
private fun buildFeedSourceMenuEntries(
    feedSource: FeedSource,
    unreadCount: Long,
    websiteUrl: String?,
    onMarkAllRead: () -> Unit,
    onOpenWebsite: (String) -> Unit,
    onEditFeed: () -> Unit,
    onChangeCategory: () -> Unit,
    onPinFeed: () -> Unit,
    onDeleteFeed: () -> Unit,
) = buildList {
    val strings = LocalFeedFlowStrings.current
    if (unreadCount > 0) {
        add(DesktopPopupMenuEntry.Action(strings.markAllReadButton, onMarkAllRead))
    }
    websiteUrl?.let { url ->
        add(DesktopPopupMenuEntry.Action(strings.openWebsiteButton, onClick = { onOpenWebsite(url) }))
    }
    add(DesktopPopupMenuEntry.Action(strings.openFeedSettings, onEditFeed))
    add(DesktopPopupMenuEntry.Action(strings.changeCategory, onChangeCategory))
    val pinLabel = if (feedSource.isPinned) strings.menuRemoveFromPinned else strings.menuAddToPinned
    add(DesktopPopupMenuEntry.Action(pinLabel, onPinFeed))
    add(DesktopPopupMenuEntry.Divider)
    add(DesktopPopupMenuEntry.Action(strings.deleteFeed, onDeleteFeed))
}.toImmutableList()

@Composable
private fun FeedFetchWarning(failed: Boolean, message: String) {
    if (!failed) return
    Row {
        Tooltip(tooltip = { Text(message) }) {
            Icon(AllIconsKeys.General.Warning, null, tint = JewelTheme.globalColors.text.warning)
        }
        Spacer(Modifier.width(DesktopDesignTokens.spaceMedium))
    }
}
