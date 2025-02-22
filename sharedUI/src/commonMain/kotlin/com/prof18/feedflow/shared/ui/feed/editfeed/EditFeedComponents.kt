package com.prof18.feedflow.shared.ui.feed.editfeed

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedSourceSettings
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.ui.feed.CategoriesSelector
import com.prof18.feedflow.shared.ui.feed.FeedNameTextField
import com.prof18.feedflow.shared.ui.feed.FeedUrlTextField
import com.prof18.feedflow.shared.ui.feed.LinkOpeningPreferenceSelector
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.tagForTesting

@Composable
fun EditFeedContent(
    feedUrl: String,
    feedName: String,
    showError: Boolean,
    showLoading: Boolean,
    errorMessage: String,
    canEditUrl: Boolean,
    categoriesState: CategoriesState,
    feedSourceSettings: FeedSourceSettings,
    onFeedUrlUpdated: (String) -> Unit,
    onFeedNameUpdated: (String) -> Unit,
    onLinkOpeningPreferenceSelected: (LinkOpeningPreference) -> Unit,
    onHiddenToggled: (Boolean) -> Unit,
    onPinnedToggled: (Boolean) -> Unit,
    editFeed: () -> Unit,
    onExpandClick: () -> Unit,
    onAddCategoryClick: (CategoryName) -> Unit,
    onDeleteCategoryClick: (CategoryId) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
    topAppBar: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = topAppBar,
        snackbarHost = snackbarHost,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = Spacing.regular),
        ) {
            item {
                FeedNameTextField(
                    modifier = Modifier
                        .padding(top = Spacing.regular)
                        .fillMaxWidth()
                        .tagForTesting(TestingTag.FEED_URL_INPUT),
                    feedName = feedName,
                    onFeedNameUpdated = onFeedNameUpdated,
                )
            }

            if (canEditUrl) {
                item {
                    FeedUrlTextField(
                        modifier = Modifier
                            .padding(top = Spacing.regular)
                            .fillMaxWidth()
                            .tagForTesting(TestingTag.FEED_URL_INPUT),
                        feedUrl = feedUrl,
                        showError = showError,
                        errorMessage = errorMessage,
                        onFeedUrlUpdated = onFeedUrlUpdated,
                    )
                }
            }

            item {
                LinkOpeningPreferenceSelector(
                    modifier = Modifier
                        .padding(top = Spacing.regular)
                        .fillMaxWidth(),
                    currentPreference = feedSourceSettings.linkOpeningPreference,
                    onPreferenceSelected = onLinkOpeningPreferenceSelected,
                )
            }

            item {
                Row(
                    modifier = modifier
                        .padding(top = Spacing.regular),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = LocalFeedFlowStrings.current.hideFeedFromTimelineDescription,
                        modifier = Modifier.padding(top = Spacing.xsmall)
                            .weight(1f),
                    )
                    Switch(
                        checked = feedSourceSettings.isHiddenFromTimeline,
                        onCheckedChange = onHiddenToggled,
                        modifier = Modifier.padding(start = Spacing.regular),
                    )
                }
            }

            item {
                Row(
                    modifier = modifier
                        .padding(top = Spacing.regular),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = LocalFeedFlowStrings.current.pinFeedSourceDescription,
                        modifier = Modifier.padding(top = Spacing.xsmall)
                            .weight(1f),
                    )
                    Switch(
                        checked = feedSourceSettings.isPinned,
                        onCheckedChange = onPinnedToggled,
                        modifier = Modifier.padding(start = Spacing.regular),
                    )
                }
            }

            item {
                CategoriesSelector(
                    modifier = Modifier
                        .padding(vertical = Spacing.regular)
                        .tagForTesting(TestingTag.CATEGORY_SELECTOR),
                    categoriesState = categoriesState,
                    onExpandClick = onExpandClick,
                    onAddCategoryClick = onAddCategoryClick,
                    onDeleteCategoryClick = onDeleteCategoryClick,
                )
            }

            item {
                Button(
                    modifier = Modifier
                        .padding(top = Spacing.small)
                        .padding(bottom = Spacing.regular)
                        .fillMaxWidth(),
                    enabled = feedUrl.isNotBlank() && !showLoading,
                    onClick = editFeed,
                ) {
                    if (showLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    } else {
                        Text(LocalFeedFlowStrings.current.editFeed)
                    }
                }
            }
        }
    }
}
