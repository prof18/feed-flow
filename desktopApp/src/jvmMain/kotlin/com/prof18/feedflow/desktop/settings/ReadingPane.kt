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
import com.prof18.feedflow.core.model.ReadArticlesVisibility
import com.prof18.feedflow.core.model.ReaderFontFamily
import com.prof18.feedflow.core.model.ReaderModeDefaults
import com.prof18.feedflow.core.model.globalArticleOpenModes
import com.prof18.feedflow.desktop.reaadermode.ReaderTextSettingsControls
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.ConfirmationDialog
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
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
    readArticlesVisibility: ReadArticlesVisibility = ReadArticlesVisibility.HIDE_ON_REFRESH,
    onReadArticlesVisibilitySelected: (ReadArticlesVisibility) -> Unit = {},
    readerFontSize: Int = ReaderModeDefaults.FONT_SIZE,
    readerLineHeight: Int = ReaderModeDefaults.LINE_HEIGHT,
    onReaderFontSizeChanged: (Int) -> Unit = {},
    onReaderLineHeightChanged: (Int) -> Unit = {},
    readerHeadlineFont: ReaderFontFamily = ReaderModeDefaults.HEADLINE_FONT,
    readerBodyFont: ReaderFontFamily = ReaderModeDefaults.BODY_FONT,
    onReaderHeadlineFontChanged: (ReaderFontFamily) -> Unit = {},
    onReaderBodyFontChanged: (ReaderFontFamily) -> Unit = {},
    readerTitleFontSize: Int = ReaderModeDefaults.TITLE_FONT_SIZE,
    onReaderTitleFontSizeChanged: (Int) -> Unit = {},
) {
    val strings = LocalFeedFlowStrings.current
    var showPrefetchConfirmation by remember { mutableStateOf(false) }
    SettingsPage(title = strings.settingsReadingTitle) {
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
                supportingText = strings.settingsSaveReaderModeContentDescription,
                checked = isSaveReaderModeContentEnabled,
                onCheckedChange = onSaveReaderModeContentToggled,
            )
            SettingsSwitch(
                title = strings.settingsPrefetchArticleContent,
                supportingText = strings.settingsPrefetchArticleContentDescription,
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
            SettingsChoice(
                title = strings.settingsReadArticlesTitle,
                choices = persistentListOf(
                    ReadArticlesVisibility.KEEP_IN_LIST to strings.settingsReadArticlesKeep,
                    ReadArticlesVisibility.HIDE_ON_REFRESH to strings.settingsReadArticlesHideOnRefresh,
                    ReadArticlesVisibility.HIDE_RIGHT_AWAY to strings.settingsReadArticlesHideRightAway,
                ),
                selected = readArticlesVisibility,
                onSelected = onReadArticlesVisibilitySelected,
                supportingText = if (isMarkReadWhenScrollingEnabled) {
                    strings.settingsReadArticlesRightAwayDisabled
                } else {
                    strings.settingsReadArticlesFooter
                },
                disabledChoices = if (isMarkReadWhenScrollingEnabled) {
                    persistentSetOf(ReadArticlesVisibility.HIDE_RIGHT_AWAY)
                } else {
                    persistentSetOf()
                },
            )
        }
        SettingsSection(title = strings.readerModeTextSettings) {
            ReaderTextSettingsControls(
                fontSize = readerFontSize,
                lineHeight = readerLineHeight,
                onFontSizeChange = onReaderFontSizeChanged,
                onLineHeightChange = onReaderLineHeightChanged,
                modifier = Modifier.fillMaxWidth(),
                headlineFont = readerHeadlineFont,
                bodyFont = readerBodyFont,
                onHeadlineFontChange = onReaderHeadlineFontChanged,
                onBodyFontChange = onReaderBodyFontChanged,
                titleFontSize = readerTitleFontSize,
                onTitleFontSizeChange = onReaderTitleFontSizeChanged,
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
