package com.prof18.feedflow.desktop.home.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun FeedContentToolbar(
    unReadCount: Long,
    showDrawerMenu: Boolean,
    isDrawerOpen: Boolean,
    currentFeedFilter: FeedFilter,
    onDrawerMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
) {
    TopAppBar(
        navigationIcon = if (showDrawerMenu) {
            {
                IconButton(
                    onClick = {
                        onDrawerMenuClick()
                    },
                ) {
                    Icon(
                        imageVector = if (isDrawerOpen) {
                            Icons.AutoMirrored.Filled.MenuOpen
                        } else {
                            Icons.Default.Menu
                        },
                        contentDescription = null,
                    )
                }
            }
        } else {
            { }
        },
        title = {
            Row {
                Text(
                    modifier = Modifier
                        .weight(1f, fill = false),
                    text = currentFeedFilter.getTitle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (currentFeedFilter !is FeedFilter.Read && currentFeedFilter !is FeedFilter.Bookmarks) {
                    Spacer(modifier = Modifier.width(4.dp))

                    Text(text = "($unReadCount)")
                }
            }
        },
        actions = {
            IconButton(
                onClick = onSearchClick,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            }
        }
    )
}

@Composable
private fun FeedFilter.getTitle(): String =
    when (this) {
        is FeedFilter.Category -> this.feedCategory.title
        is FeedFilter.Source -> this.feedSource.title
        FeedFilter.Timeline -> LocalFeedFlowStrings.current.appName
        FeedFilter.Read -> LocalFeedFlowStrings.current.drawerTitleRead
        FeedFilter.Bookmarks -> LocalFeedFlowStrings.current.drawerTitleBookmarks
    }

@Preview
@Composable
private fun FeedContentToolbarPreview() {
    FeedFlowTheme {
        FeedContentToolbar(
            unReadCount = 10,
            showDrawerMenu = true,
            isDrawerOpen = false,
            currentFeedFilter = FeedFilter.Timeline,
            onDrawerMenuClick = { },
            onSearchClick = { },
        )
    }
}
