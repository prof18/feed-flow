package com.prof18.feedflow.android.home.drawer

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.feedsourcelist.singleAndLongClickModifier
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun AndroidFeedSourceDrawerItem(
    feedSource: FeedSource,
    label: @Composable () -> Unit,
    selected: Boolean,
    drawerItemVisualStyle: DrawerItemVisualStyle,
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

        Surface(
            selected = selected,
            onClick = onClick,
            modifier = Modifier
                .semantics { role = Role.Tab }
                .heightIn(min = drawerItemVisualStyle.itemMinHeight)
                .fillMaxWidth(),
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
                        onLongClick = { showFeedMenu = true },
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

            DropdownMenu(
                expanded = showFeedMenu,
                onDismissRequest = { showFeedMenu = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                ),
            ) {
                val strings = LocalFeedFlowStrings.current

                DropdownMenuItem(
                    text = { Text(strings.deleteFeed) },
                    onClick = {
                        onDeleteFeedSourceClick(feedSource)
                        showFeedMenu = false
                    },
                )

                val websiteUrl = feedSource.websiteUrlFallback()
                if (websiteUrl != null) {
                    DropdownMenuItem(
                        text = { Text(strings.openWebsiteButton) },
                        onClick = {
                            onOpenWebsite(websiteUrl)
                            showFeedMenu = false
                        },
                    )
                }

                DropdownMenuItem(
                    text = { Text(strings.editFeedSourceNameButton) },
                    onClick = {
                        onEditFeedClick(feedSource)
                        showFeedMenu = false
                    },
                )

                DropdownMenuItem(
                    text = { Text(strings.changeCategory) },
                    onClick = {
                        onChangeFeedCategoryClick(feedSource)
                        showFeedMenu = false
                    },
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            if (feedSource.isPinned) {
                                strings.menuRemoveFromPinned
                            } else {
                                strings.menuAddToPinned
                            },
                        )
                    },
                    onClick = {
                        onPinFeedClick(feedSource)
                        showFeedMenu = false
                    },
                )
            }
        }
    }
}
