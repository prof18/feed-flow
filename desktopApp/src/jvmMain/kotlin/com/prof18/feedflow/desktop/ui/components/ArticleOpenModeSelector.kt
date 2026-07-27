package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.ArticleOpenMode.DEFAULT
import com.prof18.feedflow.core.model.ArticleOpenMode.FEED_CONTENT
import com.prof18.feedflow.core.model.ArticleOpenMode.FULL_ARTICLE
import com.prof18.feedflow.core.model.ArticleOpenMode.INTERNAL_BROWSER
import com.prof18.feedflow.core.model.ArticleOpenMode.PREFERRED_BROWSER
import com.prof18.feedflow.core.model.globalArticleOpenModes
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.shared.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.shared.ui.settings.SettingDropdownOption
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.toImmutableList

/**
 * Desktop has no in-app browser and no favourite-browser picker: every link goes to the system
 * default. So [INTERNAL_BROWSER] is never offered, and the browser section holds a single option.
 *
 * @param allowDefault true for the per-feed selector, where [ArticleOpenMode.DEFAULT] means
 * "follow the app setting". The global setting has no such option.
 */
@Composable
internal fun ArticleOpenModeSelector(
    currentMode: ArticleOpenMode,
    onModeSelected: (ArticleOpenMode) -> Unit,
    modifier: Modifier = Modifier,
    allowDefault: Boolean = false,
) {
    val strings = LocalFeedFlowStrings.current
    val options = remember(strings, allowDefault) {
        val modes = if (allowDefault) ArticleOpenMode.entries else globalArticleOpenModes
        modes
            .filter { it != INTERNAL_BROWSER }
            .map { mode ->
                SettingDropdownOption(
                    value = mode,
                    label = mode.toLabel(strings),
                    subtitle = mode.toSubtitle(strings),
                    shortLabel = mode.toShortLabel(strings),
                    sectionHeader = mode.toSectionHeader(strings),
                )
            }
            .toImmutableList()
    }

    CompactSettingDropdownRow(
        title = strings.articleOpenMode,
        // A value synced from a phone can still be INTERNAL_BROWSER. Desktop opens it exactly like
        // PREFERRED_BROWSER, so show it as the one browser option instead of leaving the row blank.
        currentValue = if (currentMode == INTERNAL_BROWSER) PREFERRED_BROWSER else currentMode,
        options = options,
        onOptionSelected = onModeSelected,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier,
    )
}

private fun ArticleOpenMode.toLabel(strings: FeedFlowStrings): String = when (this) {
    DEFAULT -> strings.linkOpeningPreferenceDefault
    FULL_ARTICLE -> strings.readerContentSourceWeb
    FEED_CONTENT -> strings.readerContentSourceFeed
    INTERNAL_BROWSER, PREFERRED_BROWSER -> strings.linkOpeningPreferenceSystemBrowser
}

/** Only the reader modes need one: the browser label already says where the article opens. */
private fun ArticleOpenMode.toSubtitle(strings: FeedFlowStrings): String? = when (this) {
    FULL_ARTICLE -> strings.readerContentSourceWebSubtitle
    FEED_CONTENT -> strings.readerContentSourceFeedSubtitle
    else -> null
}

/** The collapsed row is narrow, so the reader modes trade their long label for a short one. */
private fun ArticleOpenMode.toShortLabel(strings: FeedFlowStrings): String? = when (this) {
    DEFAULT -> strings.linkOpeningPreferenceDefaultShort
    FULL_ARTICLE -> strings.articleOpenModeFullArticleShort
    FEED_CONTENT -> strings.articleOpenModeFeedShort
    else -> null
}

/** Headers split the list along the axis the labels can't show: reader mode vs plain browser. */
private fun ArticleOpenMode.toSectionHeader(strings: FeedFlowStrings): String? = when (this) {
    FULL_ARTICLE -> strings.articleOpenModeSectionReader
    PREFERRED_BROWSER -> strings.articleOpenModeSectionBrowser
    else -> null
}
