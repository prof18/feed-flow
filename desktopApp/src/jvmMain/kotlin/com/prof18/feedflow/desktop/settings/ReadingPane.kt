package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.ArticleOpenMode.FEED_CONTENT
import com.prof18.feedflow.core.model.ArticleOpenMode.FULL_ARTICLE
import com.prof18.feedflow.core.model.ArticleOpenMode.INTERNAL_BROWSER
import com.prof18.feedflow.core.model.ArticleOpenMode.PREFERRED_BROWSER
import com.prof18.feedflow.core.model.ReaderModeDefaults
import com.prof18.feedflow.core.model.globalArticleOpenModes
import com.prof18.feedflow.desktop.reaadermode.ReaderTextSettingsControls
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.ConfirmationDialog
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ReadingPane(
    articleOpenMode: ArticleOpenMode,
    isSaveReaderModeContentEnabled: Boolean,
    isPrefetchArticleContentEnabled: Boolean,
    isMarkReadWhenScrollingEnabled: Boolean,
    onArticleOpenModeSelected: (ArticleOpenMode) -> Unit,
    onSaveReaderModeContentToggled: (Boolean) -> Unit,
    onPrefetchToggled: (Boolean) -> Unit,
    onMarkReadWhenScrollingToggled: (Boolean) -> Unit,
    readerFontSize: Int = ReaderModeDefaults.FONT_SIZE,
    readerLineHeight: Int = ReaderModeDefaults.LINE_HEIGHT,
    onReaderFontSizeChanged: (Int) -> Unit = {},
    onReaderLineHeightChanged: (Int) -> Unit = {},
) {
    val strings = LocalFeedFlowStrings.current
    var showPrefetchConfirmation by remember { mutableStateOf(false) }
    SettingsPage(title = strings.settingsReadingBehavior) {
        SettingsSection {
            val displayedMode = if (articleOpenMode == INTERNAL_BROWSER) PREFERRED_BROWSER else articleOpenMode
            SettingsChoice(
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
            SettingsSwitch(
                title = strings.settingsSaveReaderModeContent,
                checked = isSaveReaderModeContentEnabled,
                onCheckedChange = onSaveReaderModeContentToggled,
            )
            SettingsSwitch(
                title = strings.settingsPrefetchArticleContent,
                checked = isPrefetchArticleContentEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) showPrefetchConfirmation = true else onPrefetchToggled(false)
                },
            )
            SettingsSwitch(
                title = strings.toggleMarkReadWhenScrolling,
                checked = isMarkReadWhenScrollingEnabled,
                onCheckedChange = onMarkReadWhenScrollingToggled,
            )
        }
        SettingsSection(title = strings.readerModeTextSettings) {
            ReaderTextSettingsControls(
                fontSize = readerFontSize,
                lineHeight = readerLineHeight,
                onFontSizeChange = onReaderFontSizeChanged,
                onLineHeightChange = onReaderLineHeightChanged,
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
