package com.prof18.feedflow.android.feed

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
            .map { mode ->
                SettingDropdownOption(
                    value = mode,
                    label = mode.toLabel(strings),
                    e2eId = ArticleOpenModeE2eIds.option(mode),
                    subtitle = mode.toSubtitle(strings),
                    shortLabel = mode.toShortLabel(strings),
                    sectionHeader = mode.toSectionHeader(strings),
                )
            }
            .toImmutableList()
    }

    CompactSettingDropdownRow(
        title = strings.articleOpenMode,
        currentValue = currentMode,
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
    INTERNAL_BROWSER -> strings.linkOpeningPreferenceInternalBrowser
    PREFERRED_BROWSER -> strings.linkOpeningPreferencePreferredBrowser
}

/** Only the reader modes need one: the browser labels already say where the article opens. */
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
    INTERNAL_BROWSER -> strings.articleOpenModeSectionBrowser
    else -> null
}
