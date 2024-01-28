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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.tagForTesting
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun Drawer(
    modifier: Modifier = Modifier,
    navDrawerState: NavDrawerState,
    currentFeedFilter: FeedFilter,
    feedSourceImage: @Composable (String) -> Unit,
    onFeedFilterSelected: (FeedFilter) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.regular),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LazyColumn {
            item {
                DrawerTimelineItem(
                    currentFeedFilter = currentFeedFilter,
                    onFeedFilterSelected = onFeedFilterSelected,
                )
            }

            item {
                DrawerReadItem(
                    currentFeedFilter = currentFeedFilter,
                    onFeedFilterSelected = onFeedFilterSelected,
                )
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
                        feedSourceImage = feedSourceImage,
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerDivider() {
    Divider(
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
) {
    NavigationDrawerItem(
        selected = currentFeedFilter is FeedFilter.Timeline,
        label = {
            Text(
                text = stringResource(MR.strings.drawer_title_timeline),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Feed,
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
                text = stringResource(MR.strings.drawer_title_read),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Default.PlaylistAddCheck,
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
            text = stringResource(MR.strings.drawer_title_categories),
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
                modifier = Modifier.padding(horizontal = Spacing.regular),
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Label,
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
    feedSourceImage: @Composable (String) -> Unit,
) {
    Column {
        Column {
            DrawerDivider()

            Text(
                modifier = Modifier
                    .padding(start = Spacing.regular)
                    .padding(bottom = Spacing.regular),
                text = stringResource(MR.strings.drawer_title_feed_sources),
                style = MaterialTheme.typography.labelLarge,
            )

            FeedSourcesList(
                drawerFeedSources = navDrawerState.feedSourcesWithoutCategory
                    .filterIsInstance<DrawerItem.DrawerFeedSource>(),
                currentFeedFilter = currentFeedFilter,
                feedSourceImage = feedSourceImage,
                onFeedFilterSelected = onFeedFilterSelected,
            )

            for ((categoryWrapper, drawerFeedSources) in navDrawerState.feedSourcesByCategory) {
                var isCategoryExpanded by remember {
                    mutableStateOf(false)
                }

                DrawerFeedSourceByCategoryItem(
                    feedSourceCategoryWrapper = categoryWrapper,
                    drawerFeedSources = drawerFeedSources
                        .filterIsInstance<DrawerItem.DrawerFeedSource>(),
                    currentFeedFilter = currentFeedFilter,
                    isCategoryExpanded = isCategoryExpanded,
                    onCategoryExpand = {
                        isCategoryExpanded = !isCategoryExpanded
                    },
                    onFeedFilterSelected = onFeedFilterSelected,
                    feedSourceImage = feedSourceImage,
                )
            }
        }
    }
}

@Composable
private fun DrawerFeedSourceByCategoryItem(
    feedSourceCategoryWrapper: DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper,
    drawerFeedSources: List<DrawerItem.DrawerFeedSource>,
    currentFeedFilter: FeedFilter,
    isCategoryExpanded: Boolean,
    onCategoryExpand: () -> Unit,
    feedSourceImage: @Composable (String) -> Unit,
    onFeedFilterSelected: (FeedFilter) -> Unit,
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
                stringResource(resource = MR.strings.no_category)
            }

            Text(
                modifier = Modifier
                    .padding(start = Spacing.regular),
                text = headerText,
                style = MaterialTheme.typography.titleMedium,
            )

            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.rotate(degrees),
            )
        }

        FeedSourcesListWithCategorySelector(
            isCategoryExpanded = isCategoryExpanded,
            drawerFeedSources = drawerFeedSources,
            currentFeedFilter = currentFeedFilter,
            onFeedFilterSelected = onFeedFilterSelected,
            feedSourceImage = feedSourceImage,
        )
    }
}

@Composable
private fun ColumnScope.FeedSourcesListWithCategorySelector(
    isCategoryExpanded: Boolean,
    drawerFeedSources: List<DrawerItem.DrawerFeedSource>,
    currentFeedFilter: FeedFilter,
    feedSourceImage: @Composable (String) -> Unit,
    onFeedFilterSelected: (FeedFilter) -> Unit,
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
            feedSourceImage = feedSourceImage,
            onFeedFilterSelected = onFeedFilterSelected,
        )
    }
}

@Composable
private fun FeedSourcesList(
    drawerFeedSources: List<DrawerItem.DrawerFeedSource>,
    currentFeedFilter: FeedFilter,
    feedSourceImage: @Composable (String) -> Unit,
    onFeedFilterSelected: (FeedFilter) -> Unit,
) {
    Column {
        drawerFeedSources.forEach { feedSourceWrapper ->
            NavigationDrawerItem(
                selected = currentFeedFilter is FeedFilter.Source &&
                    currentFeedFilter.feedSource == feedSourceWrapper.feedSource,
                label = {
                    Text(
                        text = feedSourceWrapper.feedSource.title,
                        modifier = Modifier
                            .padding(horizontal = Spacing.small),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                icon = {
                    val imageUrl = feedSourceWrapper.feedSource.logoUrl
                    if (imageUrl != null) {
                        feedSourceImage(imageUrl)
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
                onClick = {
                    onFeedFilterSelected(
                        FeedFilter.Source(
                            feedSource = feedSourceWrapper.feedSource,
                        ),
                    )
                },
            )
        }
    }
}
