package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.components.menu.DesktopPopupMenu
import com.prof18.feedflow.shared.ui.components.menu.DesktopPopupMenuEntry
import com.prof18.feedflow.shared.ui.feedsourcelist.singleAndLongClickModifier
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.toImmutableList

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
    unreadCount: Long,
    modifier: Modifier = Modifier,
) {
    var showFeedMenu by remember { mutableStateOf(false) }
    var menuPositionInWindow by remember { mutableStateOf<Offset?>(null) }
    val itemShape = drawerItemVisualStyle.itemShape
    val itemColors = drawerItemColors(drawerItemVisualStyle)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (feedSource.fetchFailed) {
            TooltipBox(
                modifier = Modifier
                    .padding(start = Spacing.regular),
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = {
                    PlainTooltip {
                        Text(LocalFeedFlowStrings.current.feedFetchFailedTooltip)
                    }
                },
                state = rememberTooltipState(),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(color = 0xFFFF8F00),
                )
            }
            Spacer(Modifier.width(Spacing.small))
        }

        val multiSelectModifier = if (isMultiSelected && !selected) {
            Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                shape = itemShape,
            )
        } else {
            Modifier
        }

        Surface(
            selected = selected,
            onClick = onClick,
            modifier = Modifier
                .semantics { role = Role.Tab }
                .heightIn(min = drawerItemVisualStyle.itemMinHeight)
                .fillMaxWidth()
                .then(multiSelectModifier),
            shape = itemShape,
            color = itemColors.containerColor(selected).value,
        ) {
            val paddingStart = if (feedSource.fetchFailed) {
                Spacing.xsmall
            } else {
                Spacing.regular
            }
            Row(
                Modifier
                    .singleAndLongClickModifier(
                        onClick = { onClick() },
                        onLongClick = {
                            menuPositionInWindow = null
                            showFeedMenu = true
                        },
                        onLongClickPositioned = { position ->
                            menuPositionInWindow = position
                            showFeedMenu = true
                        },
                    )
                    .padding(start = paddingStart, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    val iconColor = itemColors.iconColor(selected).value
                    CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
                    Spacer(Modifier.width(12.dp))

                    Box(Modifier.weight(1f)) {
                        val labelColor = itemColors.textColor(selected).value
                        CompositionLocalProvider(LocalContentColor provides labelColor, content = label)
                    }
                }

                if (unreadCount > 0) {
                    Text(
                        modifier = Modifier.padding(start = Spacing.small),
                        text = unreadCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = itemColors.textColor(selected).value,
                    )
                }
            }

            val strings = LocalFeedFlowStrings.current
            val menuEntries = buildList {
                add(
                    DesktopPopupMenuEntry.Action(
                        text = strings.deleteFeed,
                        onClick = {
                            onDeleteFeedSourceClick(feedSource)
                            showFeedMenu = false
                        },
                    ),
                )

                val websiteUrl = feedSource.websiteUrlFallback()
                if (websiteUrl != null) {
                    add(
                        DesktopPopupMenuEntry.Action(
                            text = strings.openWebsiteButton,
                            onClick = {
                                onOpenWebsite(websiteUrl)
                                showFeedMenu = false
                            },
                        ),
                    )
                }

                add(
                    DesktopPopupMenuEntry.Action(
                        text = strings.editFeedSourceNameButton,
                        onClick = {
                            onEditFeedClick(feedSource)
                            showFeedMenu = false
                        },
                    ),
                )

                add(
                    DesktopPopupMenuEntry.Action(
                        text = strings.changeCategory,
                        onClick = {
                            onChangeFeedCategoryClick(feedSource)
                            showFeedMenu = false
                        },
                    ),
                )

                add(
                    DesktopPopupMenuEntry.Action(
                        text = if (feedSource.isPinned) {
                            strings.menuRemoveFromPinned
                        } else {
                            strings.menuAddToPinned
                        },
                        onClick = {
                            onPinFeedClick(feedSource)
                            showFeedMenu = false
                        },
                    ),
                )
            }.toImmutableList()

            DesktopPopupMenu(
                showMenu = showFeedMenu,
                menuPositionInWindow = menuPositionInWindow,
                menuEntries = menuEntries,
                closeMenu = {
                    showFeedMenu = false
                    menuPositionInWindow = null
                },
            )
        }
    }
}
