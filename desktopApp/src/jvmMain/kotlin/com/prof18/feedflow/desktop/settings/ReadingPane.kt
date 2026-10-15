package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.ArticleOpenMode.FEED_CONTENT
import com.prof18.feedflow.core.model.ArticleOpenMode.FULL_ARTICLE
import com.prof18.feedflow.core.model.ArticleOpenMode.INTERNAL_BROWSER
import com.prof18.feedflow.core.model.ArticleOpenMode.PREFERRED_BROWSER
import com.prof18.feedflow.core.model.globalArticleOpenModes
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.ConfirmationDialog
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ReadingPane(
    articleOpenMode: ArticleOpenMode,
    isSaveReaderModeContentEnabled: Boolean,
    isPrefetchArticleContentEnabled: Boolean,
    isMarkReadWhenScrollingEnabled: Boolean,
    isShowReadItemsEnabled: Boolean,
    isHideReadItemsEnabled: Boolean,
    onArticleOpenModeSelected: (ArticleOpenMode) -> Unit,
    onSaveReaderModeContentToggled: (Boolean) -> Unit,
    onPrefetchToggled: (Boolean) -> Unit,
    onMarkReadWhenScrollingToggled: (Boolean) -> Unit,
    onShowReadItemsToggled: (Boolean) -> Unit,
    onHideReadItemsToggled: (Boolean) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    var showPrefetchConfirmation by remember { mutableStateOf(false) }
    SettingsScrollPane(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SettingsGroup(strings.settingsReadingBehavior)
        val displayedMode = if (articleOpenMode == INTERNAL_BROWSER) PREFERRED_BROWSER else articleOpenMode
        JewelSettingsChoice(
            title = strings.articleOpenMode,
            choices = globalArticleOpenModes.filter { it != INTERNAL_BROWSER }.map { mode ->
                mode to when (mode) {
                    FULL_ARTICLE -> strings.readerContentSourceWeb
                    FEED_CONTENT -> strings.readerContentSourceFeed
                    PREFERRED_BROWSER -> strings.linkOpeningPreferenceSystemBrowser
                    else -> strings.linkOpeningPreferenceSystemBrowser
                }
            }.toPersistentList(),
            selected = displayedMode,
            onSelected = onArticleOpenModeSelected,
        )
        SettingsCheckbox(
            strings.settingsSaveReaderModeContent,
            isSaveReaderModeContentEnabled,
            onSaveReaderModeContentToggled,
        )
        SettingsCheckbox(
            title = strings.settingsPrefetchArticleContent,
            checked = isPrefetchArticleContentEnabled,
            onCheckedChange = { enabled ->
                if (enabled) showPrefetchConfirmation = true else onPrefetchToggled(false)
            },
        )
        SettingsCheckbox(
            strings.toggleMarkReadWhenScrolling,
            isMarkReadWhenScrollingEnabled,
            onMarkReadWhenScrollingToggled,
        )
        SettingsCheckbox(
            strings.settingsToggleShowReadArticles,
            isShowReadItemsEnabled,
            onShowReadItemsToggled,
        )
        SettingsCheckbox(
            title = strings.settingsHideReadItems,
            checked = isHideReadItemsEnabled,
            onCheckedChange = onHideReadItemsToggled,
            enabled = !isMarkReadWhenScrollingEnabled,
            supportingText = if (isMarkReadWhenScrollingEnabled) {
                strings.settingsHideReadItemsDisabledWhileScrolling
            } else {
                null
            },
        )
    }

    if (showPrefetchConfirmation) {
        ConfirmationDialog(
            title = strings.settingsPrefetchArticleContent,
            message = strings.settingsPrefetchArticleContentWarning,
            onConfirm = {
                showPrefetchConfirmation = false
                onPrefetchToggled(true)
            },
            onDismiss = { showPrefetchConfirmation = false },
        )
    }
}
