package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.shared.ui.components.EditCategoryNameDialog
import com.prof18.feedflow.shared.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.shared.ui.feedsourcelist.FeedSourceContextMenu
import com.prof18.feedflow.shared.ui.feedsourcelist.singleAndLongClickModifier
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun Drawer(
    displayState: HomeDisplayState,
    feedManagementActions: FeedManagementActions,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    modifier: Modifier = Modifier,
    onFeedSuggestionsClick: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.regular),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            DrawerTimelineItem(
                currentFeedFilter = displayState.currentFeedFilter,
                onFeedFilterSelected = onFeedFilterSelected,
                drawerItem = displayState.navDrawerState.timeline.filterIsInstance<DrawerItem.Timeline>().firstOrNull()
                    ?: DrawerItem.Timeline(unreadCount = 0),
            )
        }

        item {
            DrawerReadItem(
                currentFeedFilter = displayState.currentFeedFilter,
                onFeedFilterSelected = onFeedFilterSelected,
            )
        }

        item {
            DrawerBookmarksItem(
                currentFeedFilter = displayState.currentFeedFilter,
                onFeedFilterSelected = onFeedFilterSelected,
                drawerItem = displayState.navDrawerState.bookmarks
                    .filterIsInstance<DrawerItem.Bookmarks>()
                    .firstOrNull()
                    ?: DrawerItem.Bookmarks(unreadCount = 0),
            )
        }

        item {
            DrawerFeedSuggestionsItem(
                onFeedSuggestionsClick = onFeedSuggestionsClick,
            )
        }

        item {
            DrawerAddItem(
                onAddFeedClicked = feedManagementActions.onAddFeedClick,
            )
        }

        if (displayState.navDrawerState.pinnedFeedSources.isNotEmpty()) {
            item {
                DrawerDivider()
            }

            item {
                Column {
                    Text(
                        modifier = Modifier
                            .padding(start = Spacing.regular)
                            .padding(bottom = Spacing.regular),
                        text = LocalFeedFlowStrings.current.drawerTitlePinnedFeeds,
                        style = MaterialTheme.typography.labelLarge,
                    )

                    FeedSourcesList(
                        drawerFeedSources = displayState.navDrawerState.pinnedFeedSources
                            .filterIsInstance<DrawerItem.DrawerFeedSource>().toImmutableList(),
                        currentFeedFilter = displayState.currentFeedFilter,
                        onFeedFilterSelected = onFeedFilterSelected,
                        onEditFeedClick = feedManagementActions.onEditFeedClick,
                        onDeleteFeedSourceClick = feedManagementActions.onDeleteFeedSourceClick,
                        onPinFeedClick = feedManagementActions.onPinFeedClick,
                        onChangeFeedCategoryClick = feedManagementActions.onChangeFeedCategoryClick,
                        onOpenWebsite = feedManagementActions.onOpenWebsite,
                    )
                }
            }
        }

        if (displayState.navDrawerState.feedSourcesByCategory.isNotEmpty() ||
            displayState.navDrawerState.feedSourcesWithoutCategory.isNotEmpty()
        ) {
            item {
                DrawerFeedSourcesByCategories(
                    navDrawerState = displayState.navDrawerState,
                    currentFeedFilter = displayState.currentFeedFilter,
                    onFeedFilterSelected = onFeedFilterSelected,
                    onEditFeedClick = feedManagementActions.onEditFeedClick,
                    onDeleteFeedSourceClick = feedManagementActions.onDeleteFeedSourceClick,
                    onPinFeedClick = feedManagementActions.onPinFeedClick,
                    onChangeFeedCategoryClick = feedManagementActions.onChangeFeedCategoryClick,
                    onOpenWebsite = feedManagementActions.onOpenWebsite,
                    onEditCategoryClick = feedManagementActions.onEditCategoryClick,
                    onDeleteCategoryClick = feedManagementActions.onDeleteCategoryClick,
                )
            }
        }
    }
}

@Composable
private fun DrawerDivider() {
    HorizontalDivider(
        modifier = Modifier
            .padding(vertical = Spacing.regular),
        thickness = 0.2.dp,
        color = Color.Gray,
    )
}

@Composable
private fun DrawerTimelineItem(
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    drawerItem: DrawerItem.Timeline,
) {
    NavigationDrawerItem(
        selected = currentFeedFilter is FeedFilter.Timeline,
        badge = if (drawerItem.unreadCount > 0) {
            {
                Text(
                    text = drawerItem.unreadCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        } else {
            null
        },
        label = {
            Text(
                text = LocalFeedFlowStrings.current.drawerTitleTimeline,
            )
        },
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Feed,
                contentDescription = null,
            )
        },
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        onClick = {
            onFeedFilterSelected(FeedFilter.Timeline)
        },
    )
}

@Composable
private fun DrawerReadItem(
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
) {
    NavigationDrawerItem(
        selected = currentFeedFilter is FeedFilter.Read,
        label = {
            Text(
                text = LocalFeedFlowStrings.current.drawerTitleRead,
            )
        },
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                contentDescription = null,
            )
        },
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        onClick = {
            onFeedFilterSelected(FeedFilter.Read)
        },
    )
}

@Composable
private fun DrawerBookmarksItem(
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    drawerItem: DrawerItem.Bookmarks,
) {
    NavigationDrawerItem(
        selected = currentFeedFilter is FeedFilter.Bookmarks,
        label = {
            Text(
                text = LocalFeedFlowStrings.current.drawerTitleBookmarks,
            )
        },
        badge = if (drawerItem.unreadCount > 0) {
            {
                Text(
                    text = drawerItem.unreadCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        } else {
            null
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Bookmarks,
                contentDescription = null,
            )
        },
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        onClick = {
            onFeedFilterSelected(FeedFilter.Bookmarks)
        },
    )
}

@Composable
private fun DrawerFeedSuggestionsItem(
    onFeedSuggestionsClick: () -> Unit,
) {
    NavigationDrawerItem(
        selected = false,
        label = {
            Text(
                text = LocalFeedFlowStrings.current.feedSuggestionsTitle,
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
            )
        },
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        onClick = {
            onFeedSuggestionsClick()
        },
    )
}

@Composable
private fun DrawerAddItem(
    onAddFeedClicked: () -> Unit,
) {
    NavigationDrawerItem(
        selected = false,
        label = {
            Text(
                text = LocalFeedFlowStrings.current.addFeed,
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Default.AddCircleOutline,
                contentDescription = null,
            )
        },
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        onClick = {
            onAddFeedClicked()
        },
    )
}

@Composable
private fun DrawerFeedSourcesByCategories(
    navDrawerState: NavDrawerState,
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onEditCategoryClick: (CategoryId, CategoryName) -> Unit,
    onChangeFeedCategoryClick: ((FeedSource) -> Unit),
    onDeleteCategoryClick: (CategoryId) -> Unit,
) {
    Column {
        Column {
            DrawerDivider()

            Text(
                modifier = Modifier
                    .padding(start = Spacing.regular)
                    .padding(bottom = Spacing.regular),
                text = LocalFeedFlowStrings.current.drawerTitleFeedSources,
                style = MaterialTheme.typography.labelLarge,
            )

            FeedSourcesList(
                drawerFeedSources = navDrawerState.feedSourcesWithoutCategory
                    .filterIsInstance<DrawerItem.DrawerFeedSource>().toImmutableList(),
                currentFeedFilter = currentFeedFilter,
                onFeedFilterSelected = onFeedFilterSelected,
                onEditFeedClick = onEditFeedClick,
                onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                onPinFeedClick = onPinFeedClick,
                onChangeFeedCategoryClick = onChangeFeedCategoryClick,
                onOpenWebsite = onOpenWebsite,
            )

            for ((categoryWrapper, drawerFeedSources) in navDrawerState.feedSourcesByCategory) {
                var isCategoryExpanded by rememberSaveable {
                    mutableStateOf(false)
                }

                DrawerFeedSourceByCategoryItem(
                    feedSourceCategoryWrapper = categoryWrapper,
                    drawerFeedSources = drawerFeedSources
                        .filterIsInstance<DrawerItem.DrawerFeedSource>().toImmutableList(),
                    currentFeedFilter = currentFeedFilter,
                    isCategoryExpanded = isCategoryExpanded,
                    onCategoryExpand = {
                        isCategoryExpanded = !isCategoryExpanded
                    },
                    onFeedFilterSelected = onFeedFilterSelected,
                    onEditFeedClick = onEditFeedClick,
                    onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                    onPinFeedClick = onPinFeedClick,
                    onChangeFeedCategoryClick = onChangeFeedCategoryClick,
                    onOpenWebsite = onOpenWebsite,
                    onEditCategoryClick = onEditCategoryClick,
                    onDeleteCategoryClick = onDeleteCategoryClick,
                )
            }
        }
    }
}

@Composable
private fun DrawerFeedSourceByCategoryItem(
    feedSourceCategoryWrapper: DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper,
    drawerFeedSources: ImmutableList<DrawerItem.DrawerFeedSource>,
    currentFeedFilter: FeedFilter,
    isCategoryExpanded: Boolean,
    onCategoryExpand: () -> Unit,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onChangeFeedCategoryClick: ((FeedSource) -> Unit),
    onOpenWebsite: (String) -> Unit,
    onEditCategoryClick: (CategoryId, CategoryName) -> Unit,
    onDeleteCategoryClick: (CategoryId) -> Unit,
) {
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }

    val category = feedSourceCategoryWrapper.feedSourceCategory
    val unreadCount = drawerFeedSources.sumOf { it.unreadCount }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        @Suppress("MagicNumber")
        val degrees by animateFloatAsState(
            targetValue = if (isCategoryExpanded) {
                -90f
            } else {
                90f
            },
            label = "Category arrow animation",
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            // Remove extra vertical padding to match feed source item spacing
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val headerText = if (category?.title != null) {
                requireNotNull(category.title)
            } else {
                LocalFeedFlowStrings.current.noCategory
            }

            // Category name - pill shaped like inner item when selectable category
            if (category != null) {
                val isSelected = currentFeedFilter is FeedFilter.Category &&
                    currentFeedFilter.feedCategory == category
                val navItemColors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                )

                Surface(
                    selected = isSelected,
                    onClick = { onFeedFilterSelected(FeedFilter.Category(feedCategory = category)) },
                    shape = CircleShape,
                    color = navItemColors.containerColor(isSelected).value,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.Tab }
                        .heightIn(min = 56.dp)
                        .hoverable(remember { MutableInteractionSource() }),
                ) {
                    Row(
                        modifier = Modifier
                            .singleAndLongClickModifier(
                                onClick = { onFeedFilterSelected(FeedFilter.Category(feedCategory = category)) },
                                onLongClick = { showMenu = true },
                            )
                            // Match inner feed item content padding (24.dp end)
                            .padding(start = Spacing.regular, end = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val labelColor = navItemColors.textColor(isSelected).value
                        CompositionLocalProvider(LocalContentColor provides labelColor) {
                            Text(
                                text = headerText,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (unreadCount > 0) {
                            Text(
                                modifier = Modifier.padding(start = Spacing.small),
                                text = unreadCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = navItemColors.textColor(isSelected).value,
                            )
                        }

                        // Arrow moved outside the pill to avoid including it in the hover/selection area
                    }
                }
            } else {
                // Uncategorized: render with the same pill shape/spacing as other category headers
                val isSelected = currentFeedFilter is FeedFilter.Uncategorized
                val navItemColors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                )

                Surface(
                    selected = isSelected,
                    onClick = { onFeedFilterSelected(FeedFilter.Uncategorized) },
                    shape = CircleShape,
                    color = navItemColors.containerColor(isSelected).value,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.Tab }
                        .heightIn(min = 56.dp)
                        .hoverable(remember { MutableInteractionSource() }),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(start = Spacing.regular, end = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val labelColor = navItemColors.textColor(isSelected).value
                        CompositionLocalProvider(LocalContentColor provides labelColor) {
                            Text(
                                text = headerText,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (unreadCount > 0) {
                            Text(
                                modifier = Modifier.padding(start = Spacing.small),
                                text = unreadCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = navItemColors.textColor(isSelected).value,
                            )
                        }

                        // Arrow moved outside the pill to avoid including it in the hover/selection area
                    }
                }
            }
            // Trailing toggle arrow aligned to the right edge
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = interaction,
                        indication = androidx.compose.material3.ripple(),
                    ) { onCategoryExpand() }
                    .padding(12.dp)
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.rotate(degrees),
                )
            }
        }

        FeedSourcesListWithCategorySelector(
            isCategoryExpanded = isCategoryExpanded,
            drawerFeedSources = drawerFeedSources,
            currentFeedFilter = currentFeedFilter,
            onFeedFilterSelected = onFeedFilterSelected,
            onEditFeedClick = onEditFeedClick,
            onDeleteFeedSourceClick = onDeleteFeedSourceClick,
            onPinFeedClick = onPinFeedClick,
            onChangeFeedCategoryClick = onChangeFeedCategoryClick,
            onOpenWebsite = onOpenWebsite,
        )
    }

    if (category != null) {
        CategoryContextMenu(
            showMenu = showMenu,
            hideMenu = { showMenu = false },
            categoryId = CategoryId(category.id),
            onEditCategoryClick = {
                showMenu = false
                showEditDialog = true
            },
            onDeleteCategoryClick = onDeleteCategoryClick,
        )

        EditCategoryNameDialog(
            showDialog = showEditDialog,
            categoryId = CategoryId(category.id),
            initialCategoryName = category.title,
            onDismiss = { showEditDialog = false },
            onEditCategory = onEditCategoryClick,
        )
    }
}

@Composable
private fun ColumnScope.FeedSourcesListWithCategorySelector(
    isCategoryExpanded: Boolean,
    drawerFeedSources: ImmutableList<DrawerItem.DrawerFeedSource>,
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onChangeFeedCategoryClick: ((FeedSource) -> Unit),
    onOpenWebsite: (String) -> Unit,
) {
    AnimatedVisibility(
        visible = isCategoryExpanded,
        enter = expandVertically(
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntSize.VisibilityThreshold,
            ),
        ),
        exit = shrinkVertically(),
    ) {
        FeedSourcesList(
            drawerFeedSources = drawerFeedSources,
            currentFeedFilter = currentFeedFilter,
            onFeedFilterSelected = onFeedFilterSelected,
            onEditFeedClick = onEditFeedClick,
            onDeleteFeedSourceClick = onDeleteFeedSourceClick,
            onPinFeedClick = onPinFeedClick,
            onChangeFeedCategoryClick = onChangeFeedCategoryClick,
            onOpenWebsite = onOpenWebsite,
        )
    }
}

@Composable
private fun FeedSourcesList(
    drawerFeedSources: ImmutableList<DrawerItem.DrawerFeedSource>,
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onChangeFeedCategoryClick: ((FeedSource) -> Unit),
    onOpenWebsite: (String) -> Unit,
) {
    Column {
        drawerFeedSources.forEach { feedSourceWrapper ->

            FeedSourceDrawerItem(
                label = {
                    Text(
                        text = feedSourceWrapper.feedSource.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                selected = currentFeedFilter is FeedFilter.Source &&
                    currentFeedFilter.feedSource == feedSourceWrapper.feedSource,
                onClick = {
                    onFeedFilterSelected(
                        FeedFilter.Source(
                            feedSource = feedSourceWrapper.feedSource,
                        ),
                    )
                },
                icon = {
                    val imageUrl = feedSourceWrapper.feedSource.logoUrl
                    if (imageUrl != null) {
                        FeedSourceLogoImage(
                            size = 24.dp,
                            imageUrl = imageUrl,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                        )
                    }
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                ),
                onEditFeedClick = onEditFeedClick,
                onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                onPinFeedClick = onPinFeedClick,
                onChangeFeedCategoryClick = onChangeFeedCategoryClick,
                onOpenWebsite = onOpenWebsite,
                feedSource = feedSourceWrapper.feedSource,
                unreadCount = feedSourceWrapper.unreadCount,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedSourceDrawerItem(
    feedSource: FeedSource,
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onChangeFeedCategoryClick: ((FeedSource) -> Unit),
    onOpenWebsite: (String) -> Unit,
    unreadCount: Long,
    modifier: Modifier = Modifier,
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
) {
    var showFeedMenu by remember {
        mutableStateOf(
            false,
        )
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (feedSource.fetchFailed) {
            TooltipBox(
                modifier = Modifier
                    .padding(start = Spacing.regular),
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
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
                .heightIn(min = 56.0.dp)
                .fillMaxWidth(),
            shape = CircleShape,
            color = colors.containerColor(selected).value,
        ) {
            val paddingStart = if (feedSource.fetchFailed) {
                Spacing.xsmall
            } else {
                Spacing.regular
            }
            Row(
                Modifier
                    .singleAndLongClickModifier(
                        onClick = {
                            onClick()
                        },
                        onLongClick = {
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
                    val iconColor = colors.iconColor(selected).value
                    CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
                    Spacer(Modifier.width(12.dp))

                    Box(Modifier.weight(1f)) {
                        val labelColor = colors.textColor(selected).value
                        CompositionLocalProvider(LocalContentColor provides labelColor, content = label)
                    }
                }

                if (unreadCount > 0) {
                    Text(
                        modifier = Modifier.padding(start = Spacing.small),
                        text = unreadCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textColor(selected).value,
                    )
                }
            }

            FeedSourceContextMenu(
                showFeedMenu = showFeedMenu,
                hideMenu = {
                    showFeedMenu = false
                },
                onEditFeedClick = onEditFeedClick,
                onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                feedSource = feedSource,
                onPinFeedClick = onPinFeedClick,
                onChangeFeedCategoryClick = onChangeFeedCategoryClick,
                onOpenWebsite = onOpenWebsite,
            )
        }
    }
}
