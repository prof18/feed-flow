package com.prof18.feedflow.android.settings.readingbehavior

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.android.settings.SettingsE2eIds
import com.prof18.feedflow.android.settings.components.BrowserSelector
import com.prof18.feedflow.android.ui.settings.SettingSwitchItem
import com.prof18.feedflow.android.ui.style.Spacing
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.Browser
import com.prof18.feedflow.core.model.ReadingBehaviorState
import com.prof18.feedflow.shared.presentation.preview.browsersForPreview
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ReadingBehaviorScreenContent(
    navigateBack: () -> Unit,
    state: ReadingBehaviorState,
    browsers: ImmutableList<Browser>,
    resolvedArticleBrowserId: String,
    onBrowserSelected: (Browser) -> Unit,
    onArticleBrowserSelected: (String) -> Unit,
    setArticleOpenMode: (ArticleOpenMode) -> Unit,
    setMarkReadWhenScrolling: (Boolean) -> Unit,
    setShowReadItem: (Boolean) -> Unit,
    setHideReadItems: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalFeedFlowStrings.current.settingsReadingBehavior) },
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.testTag(SettingsE2eIds.BACK_BUTTON),
                        onClick = navigateBack,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val layoutDir = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .padding(start = paddingValues.calculateLeftPadding(layoutDir))
                .padding(end = paddingValues.calculateRightPadding(layoutDir)),
        ) {
            item {
                ArticleDestinationSelector(
                    modifier = Modifier
                        .padding(horizontal = Spacing.regular)
                        .testTag(SettingsE2eIds.READING_BEHAVIOR_ARTICLE_OPEN_MODE),
                    articleOpenMode = state.articleOpenMode,
                    resolvedArticleBrowserId = resolvedArticleBrowserId,
                    browsers = browsers,
                    onReaderModeSelected = setArticleOpenMode,
                    onBrowserSelected = onArticleBrowserSelected,
                )
            }

            item {
                BrowserSelector(
                    browsers = browsers,
                    onBrowserSelected = onBrowserSelected,
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.toggleMarkReadWhenScrolling,
                    isChecked = state.isMarkReadWhenScrollingEnabled,
                    onCheckedChange = setMarkReadWhenScrolling,
                )
            }

            item {
                SettingSwitchItem(
                    modifier = Modifier.testTag(SettingsE2eIds.READING_BEHAVIOR_SHOW_READ),
                    title = LocalFeedFlowStrings.current.settingsToggleShowReadArticles,
                    isChecked = state.isShowReadItemsEnabled,
                    onCheckedChange = setShowReadItem,
                )
            }

            item {
                SettingSwitchItem(
                    modifier = Modifier.testTag(SettingsE2eIds.READING_BEHAVIOR_HIDE_READ),
                    title = LocalFeedFlowStrings.current.settingsHideReadItems,
                    isChecked = state.isHideReadItemsEnabled,
                    onCheckedChange = setHideReadItems,
                    enabled = !state.isMarkReadWhenScrollingEnabled,
                    supportingText = if (state.isMarkReadWhenScrollingEnabled) {
                        LocalFeedFlowStrings.current.settingsHideReadItemsDisabledWhileScrolling
                    } else {
                        null
                    },
                )
            }

            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}

@Preview
@Composable
private fun ReadingBehaviorScreenContentPreview() {
    FeedFlowTheme {
        ReadingBehaviorScreenContent(
            navigateBack = {},
            state = ReadingBehaviorState(
                isSaveReaderModeContentEnabled = false,
                isPrefetchArticleContentEnabled = false,
                isMarkReadWhenScrollingEnabled = true,
                isShowReadItemsEnabled = true,
                isHideReadItemsEnabled = false,
            ),
            browsers = browsersForPreview,
            resolvedArticleBrowserId = browsersForPreview.first().id,
            onBrowserSelected = {},
            onArticleBrowserSelected = {},
            setArticleOpenMode = {},
            setMarkReadWhenScrolling = {},
            setShowReadItem = {},
            setHideReadItems = {},
        )
    }
}
