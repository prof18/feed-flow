package com.prof18.feedflow.ui.home.components

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.prof18.feedflow.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun Drawer(
    modifier: Modifier = Modifier,
    drawerItems: List<DrawerItem>,
    currentFeedFilter: FeedFilter,
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
            items(drawerItems) { drawerItem ->
                when (drawerItem) {
                    is DrawerItem.Timeline -> {
                        DrawerTimelineItem(
                            currentFeedFilter = currentFeedFilter,
                            onFeedFilterSelected = onFeedFilterSelected,
                        )

                        DrawerDivider()
                    }

                    is DrawerItem.CategorySectionTitle -> {
                        Text(
                            modifier = Modifier
                                .padding(start = Spacing.regular)
                                .padding(bottom = Spacing.regular),
                            text = stringResource(MR.strings.drawer_title_categories),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    is DrawerItem.DrawerCategory -> {
                        DrawerCategoryItem(
                            currentFeedFilter = currentFeedFilter,
                            drawerItem = drawerItem,
                            onFeedFilterSelected = onFeedFilterSelected,
                        )
                    }

                    is DrawerItem.CategorySourcesTitle -> {
                        DrawerDivider()

                        Text(
                            modifier = Modifier
                                .padding(start = Spacing.regular)
                                .padding(bottom = Spacing.regular),
                            text = stringResource(MR.strings.drawer_title_feed_sources),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    is DrawerItem.DrawerCategoryWrapper -> {
                        DrawerFeedSourceByCategoryItem(
                            drawerItem = drawerItem,
                            currentFeedFilter = currentFeedFilter,
                            onFeedFilterSelected = onFeedFilterSelected,
                        )
                    }
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
private fun DrawerCategoryItem(
    currentFeedFilter: FeedFilter,
    drawerItem: DrawerItem.DrawerCategory,
    onFeedFilterSelected: (FeedFilter) -> Unit,
) {
    NavigationDrawerItem(
        selected = currentFeedFilter is FeedFilter.Category &&
            drawerItem.category == currentFeedFilter.feedCategory,
        label = {
            Text(
                text = drawerItem.category.title,
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
                FeedFilter.Category(feedCategory = drawerItem.category),
            )
        },
    )
}

@Composable
private fun DrawerFeedSourceByCategoryItem(
    drawerItem: DrawerItem.DrawerCategoryWrapper,
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
) {
    Column {
        @Suppress("MagicNumber")
        val degrees by animateFloatAsState(
            targetValue = if (drawerItem.isExpanded) {
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
                    drawerItem.onExpandClick(drawerItem)
                }
                .fillMaxWidth()
                .padding(vertical = Spacing.regular),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val headerText = if (drawerItem.category?.title != null) {
                requireNotNull(drawerItem.category?.title)
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

        FeedSourcesList(
            drawerItem = drawerItem,
            currentFeedFilter = currentFeedFilter,
            onFeedFilterSelected = onFeedFilterSelected,
        )
    }
}

@Composable
private fun ColumnScope.FeedSourcesList(
    drawerItem: DrawerItem.DrawerCategoryWrapper,
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
) {
    AnimatedVisibility(
        visible = drawerItem.isExpanded,
        enter = expandVertically(
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntSize.VisibilityThreshold,
            ),
        ),
        exit = shrinkVertically(),
    ) {
        Column {
            drawerItem.feedSources.forEach { feedSourceWrapper ->
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
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                        )
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
}
