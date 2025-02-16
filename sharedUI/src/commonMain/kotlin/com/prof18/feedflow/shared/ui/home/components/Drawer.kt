package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.shared.ui.feedsourcelist.FeedSourceContextMenu
import com.prof18.feedflow.shared.ui.feedsourcelist.feedSourceMenuClickModifier
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.tagForTesting
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun Drawer(
    navDrawerState: NavDrawerState,
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    onAddFeedClicked: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    modifier: Modifier = Modifier,
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
                currentFeedFilter = currentFeedFilter,
                onFeedFilterSelected = onFeedFilterSelected,
                drawerItem = navDrawerState.timeline.filterIsInstance<DrawerItem.Timeline>().firstOrNull()
                    ?: DrawerItem.Timeline(unreadCount = 0),
            )
        }

        item {
            DrawerReadItem(
                currentFeedFilter = currentFeedFilter,
                onFeedFilterSelected = onFeedFilterSelected,
            )
        }

        item {
            DrawerBookmarksItem(
                currentFeedFilter = currentFeedFilter,
                onFeedFilterSelected = onFeedFilterSelected,
                drawerItem = navDrawerState.bookmarks.filterIsInstance<DrawerItem.Bookmarks>().firstOrNull()
                    ?: DrawerItem.Bookmarks(unreadCount = 0),
            )
        }

        item {
            DrawerAddItem(
                onAddFeedClicked = onAddFeedClicked,
            )
        }

        if (navDrawerState.pinnedFeedSources.isNotEmpty()) {
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
                        drawerFeedSources = navDrawerState.pinnedFeedSources
                            .filterIsInstance<DrawerItem.DrawerFeedSource>().toImmutableList(),
                        currentFeedFilter = currentFeedFilter,
                        onFeedFilterSelected = onFeedFilterSelected,
                        onEditFeedClick = onEditFeedClick,
                        onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                    )
                }
            }
        }

        if (navDrawerState.categories.isNotEmpty()) {
            item {
                DrawerDivider()
            }

            item {
                DrawerCategoriesSection(
                    navDrawerState = navDrawerState,
                    currentFeedFilter = currentFeedFilter,
                    onFeedFilterSelected = onFeedFilterSelected,
                )
            }
        }

        if (navDrawerState.feedSourcesByCategory.isNotEmpty() ||
            navDrawerState.feedSourcesWithoutCategory.isNotEmpty()
        ) {
            item {
                DrawerFeedSourcesByCategories(
                    navDrawerState = navDrawerState,
                    currentFeedFilter = currentFeedFilter,
                    onFeedFilterSelected = onFeedFilterSelected,
                    onEditFeedClick = onEditFeedClick,
                    onDeleteFeedSourceClick = onDeleteFeedSourceClick,
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
private fun DrawerCategoriesSection(
    navDrawerState: NavDrawerState,
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
) {
    Column {
        Text(
            modifier = Modifier
                .padding(start = Spacing.regular)
                .padding(bottom = Spacing.regular),
            text = LocalFeedFlowStrings.current.drawerTitleCategories,
            style = MaterialTheme.typography.labelLarge,
        )

        for (category in navDrawerState.categories) {
            DrawerCategoryItem(
                currentFeedFilter = currentFeedFilter,
                drawerCategory = category as DrawerItem.DrawerCategory,
                onFeedFilterSelected = onFeedFilterSelected,
            )
        }
    }
}

@Composable
private fun DrawerCategoryItem(
    currentFeedFilter: FeedFilter,
    drawerCategory: DrawerItem.DrawerCategory,
    onFeedFilterSelected: (FeedFilter) -> Unit,
) {
    NavigationDrawerItem(
        selected = currentFeedFilter is FeedFilter.Category &&
            drawerCategory.category == currentFeedFilter.feedCategory,
        label = {
            Text(
                text = drawerCategory.category.title,
            )
        },
        badge = if (drawerCategory.unreadCount > 0) {
            {
                Text(
                    text = drawerCategory.unreadCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        } else {
            null
        },
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Label,
                contentDescription = null,
            )
        },
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        onClick = {
            onFeedFilterSelected(
                FeedFilter.Category(feedCategory = drawerCategory.category),
            )
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
            )

            for ((categoryWrapper, drawerFeedSources) in navDrawerState.feedSourcesByCategory) {
                var isCategoryExpanded by remember {
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
) {
    val categoryTitle = feedSourceCategoryWrapper.feedSourceCategory?.title
    Column(
        modifier = Modifier
            .tagForTesting("${TestingTag.FEED_SOURCE_SELECTOR}_$categoryTitle"),
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
                .clip(MaterialTheme.shapes.medium)
                .clickable {
                    onCategoryExpand()
                }
                .fillMaxWidth()
                .padding(vertical = Spacing.regular),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val headerText = if (feedSourceCategoryWrapper.feedSourceCategory?.title != null) {
                requireNotNull(feedSourceCategoryWrapper.feedSourceCategory?.title)
            } else {
                LocalFeedFlowStrings.current.noCategory
            }

            Text(
                modifier = Modifier
                    .padding(start = Spacing.regular),
                text = headerText,
                style = MaterialTheme.typography.titleMedium,
            )

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.rotate(degrees),
            )
        }

        FeedSourcesListWithCategorySelector(
            isCategoryExpanded = isCategoryExpanded,
            drawerFeedSources = drawerFeedSources,
            currentFeedFilter = currentFeedFilter,
            onFeedFilterSelected = onFeedFilterSelected,
            onEditFeedClick = onEditFeedClick,
            onDeleteFeedSourceClick = onDeleteFeedSourceClick,
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
                feedSource = feedSourceWrapper.feedSource,
                unreadCount = feedSourceWrapper.unreadCount,
            )
        }
    }
}

@Composable
fun FeedSourceDrawerItem(
    feedSource: FeedSource,
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    unreadCount: Long,
) {
    var showFeedMenu by remember {
        mutableStateOf(
            false,
        )
    }

    Surface(
        selected = selected,
        onClick = onClick,
        modifier =
        modifier
            .semantics { role = Role.Tab }
            .heightIn(min = 56.0.dp)
            .fillMaxWidth(),
        shape = CircleShape,
        color = colors.containerColor(selected).value,
    ) {
        Row(
            Modifier
                .feedSourceMenuClickModifier(
                    onClick = {
                        onClick()
                    },
                    onLongClick = {
                        showFeedMenu = true
                    },
                )
                .padding(start = 16.dp, end = 24.dp),
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
        )
    }
}
