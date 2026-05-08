package com.prof18.feedflow.android.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.prof18.feedflow.core.model.Browser
import com.prof18.feedflow.shared.presentation.preview.browsersForPreview
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.shared.ui.settings.SettingDropdownOption
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun BrowserSelector(
    browsers: ImmutableList<Browser>,
    onBrowserSelected: (Browser) -> Unit,
) {
    val currentBrowserId = browsers.firstOrNull { it.isFavourite }?.id.orEmpty()

    val options = remember(browsers) {
        browsers
            .map { browser -> SettingDropdownOption(browser.id, browser.name) }
            .toImmutableList()
    }

    CompactSettingDropdownRow(
        title = LocalFeedFlowStrings.current.browserSelectionButton,
        currentValue = currentBrowserId,
        options = options,
        onOptionSelected = { browserId ->
            browsers.firstOrNull { it.id == browserId }?.let(onBrowserSelected)
        },
    )
}

@PreviewPhone
@Composable
private fun BrowserSelectorPreview() {
    FeedFlowTheme {
        BrowserSelector(
            browsers = browsersForPreview,
            onBrowserSelected = {},
        )
    }
}
