package com.prof18.feedflow.android.settings.readingbehavior

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.feed.ArticleOpenModeE2eIds
import com.prof18.feedflow.android.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.android.ui.settings.SettingDropdownOption
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.Browser
import com.prof18.feedflow.core.utils.BrowserIds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * The global "Open articles in" setting: the reader modes, then every browser. Picking a browser
 * stores it as the article browser, which can differ from the browser used for other links.
 */
@Composable
internal fun ArticleDestinationSelector(
    articleOpenMode: ArticleOpenMode,
    resolvedArticleBrowserId: String,
    browsers: ImmutableList<Browser>,
    onReaderModeSelected: (ArticleOpenMode) -> Unit,
    onBrowserSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val options = remember(strings, browsers) {
        val readerOptions = listOf(
            SettingDropdownOption<ArticleDestination>(
                value = ArticleDestination.Reader(ArticleOpenMode.FULL_ARTICLE),
                label = strings.readerContentSourceWeb,
                e2eId = ArticleOpenModeE2eIds.option(ArticleOpenMode.FULL_ARTICLE),
                subtitle = strings.readerContentSourceWebSubtitle,
                shortLabel = strings.articleOpenModeFullArticleShort,
                sectionHeader = strings.articleOpenModeSectionReader,
            ),
            SettingDropdownOption<ArticleDestination>(
                value = ArticleDestination.Reader(ArticleOpenMode.FEED_CONTENT),
                label = strings.readerContentSourceFeed,
                e2eId = ArticleOpenModeE2eIds.option(ArticleOpenMode.FEED_CONTENT),
                subtitle = strings.readerContentSourceFeedSubtitle,
                shortLabel = strings.articleOpenModeFeedShort,
            ),
        )
        val browserOptions = browsers.mapIndexed { index, browser ->
            SettingDropdownOption<ArticleDestination>(
                value = ArticleDestination.Browser(browser.id),
                label = browser.name,
                e2eId = if (browser.id == BrowserIds.IN_APP_BROWSER) {
                    ArticleOpenModeE2eIds.option(ArticleOpenMode.INTERNAL_BROWSER)
                } else {
                    null
                },
                sectionHeader = if (index == 0) strings.articleOpenModeSectionBrowser else null,
            )
        }
        (readerOptions + browserOptions).toImmutableList()
    }

    val currentValue: ArticleDestination = when (articleOpenMode) {
        ArticleOpenMode.INTERNAL_BROWSER -> ArticleDestination.Browser(BrowserIds.IN_APP_BROWSER)
        ArticleOpenMode.PREFERRED_BROWSER -> ArticleDestination.Browser(resolvedArticleBrowserId)
        else -> ArticleDestination.Reader(articleOpenMode)
    }

    CompactSettingDropdownRow(
        title = strings.articleOpenMode,
        currentValue = currentValue,
        options = options,
        onOptionSelected = { destination ->
            when (destination) {
                is ArticleDestination.Reader -> onReaderModeSelected(destination.mode)
                is ArticleDestination.Browser -> onBrowserSelected(destination.id)
            }
        },
        contentPadding = PaddingValues(0.dp),
        modifier = modifier,
        supportingText = {
            Text(
                text = strings.articleOpenModeSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

private sealed interface ArticleDestination {
    data class Reader(val mode: ArticleOpenMode) : ArticleDestination
    data class Browser(val id: String) : ArticleDestination
}
