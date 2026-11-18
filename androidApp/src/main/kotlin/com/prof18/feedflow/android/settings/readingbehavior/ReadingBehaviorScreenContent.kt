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
import com.prof18.feedflow.android.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.android.ui.settings.SettingDropdownOption
import com.prof18.feedflow.android.ui.settings.SettingSwitchItem
import com.prof18.feedflow.android.ui.style.Spacing
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.Browser
import com.prof18.feedflow.core.model.ReadArticlesVisibility
import com.prof18.feedflow.core.model.ReadingBehaviorState
import com.prof18.feedflow.shared.presentation.preview.browsersForPreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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
    setReadArticlesVisibility: (ReadArticlesVisibility) -> Unit,
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
                ReadArticlesVisibilityRow(
                    readArticlesVisibility = state.readArticlesVisibility,
                    isMarkReadWhenScrollingEnabled = state.isMarkReadWhenScrollingEnabled,
                    onReadArticlesVisibilitySelected = setReadArticlesVisibility,
                )
            }

            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}

@Composable
private fun ReadArticlesVisibilityRow(
    readArticlesVisibility: ReadArticlesVisibility,
    isMarkReadWhenScrollingEnabled: Boolean,
    onReadArticlesVisibilitySelected: (ReadArticlesVisibility) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    CompactSettingDropdownRow(
        modifier = Modifier.testTag(SettingsE2eIds.READING_BEHAVIOR_READ_ARTICLES),
        title = strings.settingsReadArticlesTitle,
        currentValue = readArticlesVisibility,
        options = persistentListOf(
            SettingDropdownOption(
                value = ReadArticlesVisibility.KEEP_IN_LIST,
                label = strings.settingsReadArticlesKeep,
            ),
            SettingDropdownOption(
                value = ReadArticlesVisibility.HIDE_ON_REFRESH,
                label = strings.settingsReadArticlesHideOnRefresh,
            ),
            SettingDropdownOption(
                value = ReadArticlesVisibility.HIDE_RIGHT_AWAY,
                label = strings.settingsReadArticlesHideRightAway,
                e2eId = SettingsE2eIds.READING_BEHAVIOR_READ_ARTICLES_HIDE_RIGHT_AWAY,
                subtitle = strings.settingsReadArticlesRightAwayDisabled.takeIf { isMarkReadWhenScrollingEnabled },
                enabled = !isMarkReadWhenScrollingEnabled,
            ),
        ),
        onOptionSelected = onReadArticlesVisibilitySelected,
        supportingText = {
            Text(strings.settingsReadArticlesFooter)
        },
    )
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
                readArticlesVisibility = ReadArticlesVisibility.KEEP_IN_LIST,
            ),
            browsers = browsersForPreview,
            resolvedArticleBrowserId = browsersForPreview.first().id,
            onBrowserSelected = {},
            onArticleBrowserSelected = {},
            setArticleOpenMode = {},
            setMarkReadWhenScrolling = {},
            setReadArticlesVisibility = {},
        )
    }
}
