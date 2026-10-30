package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.globalArticleOpenModes
import com.prof18.feedflow.desktop.settings.SettingsChoice
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ArticleOpenModeSelector(
    currentMode: ArticleOpenMode,
    onModeSelected: (ArticleOpenMode) -> Unit,
    modifier: Modifier = Modifier,
    allowDefault: Boolean = false,
    enabled: Boolean = true,
) {
    val strings = LocalFeedFlowStrings.current
    val modes = if (allowDefault) ArticleOpenMode.entries else globalArticleOpenModes
    SettingsChoice(
        title = strings.articleOpenMode,
        selected = if (currentMode == ArticleOpenMode.INTERNAL_BROWSER) {
            ArticleOpenMode.PREFERRED_BROWSER
        } else {
            currentMode
        },
        choices = modes.filter { it != ArticleOpenMode.INTERNAL_BROWSER }
            .map { mode ->
                mode to when (mode) {
                    ArticleOpenMode.DEFAULT -> strings.linkOpeningPreferenceDefault
                    ArticleOpenMode.FULL_ARTICLE -> strings.readerContentSourceWeb
                    ArticleOpenMode.FEED_CONTENT -> strings.readerContentSourceFeed
                    ArticleOpenMode.PREFERRED_BROWSER, ArticleOpenMode.INTERNAL_BROWSER -> {
                        strings.linkOpeningPreferenceSystemBrowser
                    }
                }
            }.toPersistentList(),
        onSelected = onModeSelected,
        modifier = modifier,
        enabled = enabled,
    )
}
