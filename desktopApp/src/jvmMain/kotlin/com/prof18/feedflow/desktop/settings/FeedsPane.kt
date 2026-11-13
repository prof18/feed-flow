package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.Composable
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings

@Composable
internal fun FeedsPane(
    onImportExport: () -> Unit,
    onBlockedWords: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    SettingsPage(title = strings.feedsTitle) {
        SettingsSection {
            SettingsActionRow(
                title = strings.importExportLabel,
                onClick = onImportExport,
                showChevron = true,
            )
            SettingsActionRow(
                title = strings.settingsBlockedWords,
                onClick = onBlockedWords,
                showChevron = true,
            )
        }
    }
}
