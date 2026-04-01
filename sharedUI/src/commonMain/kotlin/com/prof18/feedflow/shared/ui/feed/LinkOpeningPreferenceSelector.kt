package com.prof18.feedflow.shared.ui.feed

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.LinkOpeningPreference.DEFAULT
import com.prof18.feedflow.core.model.LinkOpeningPreference.INTERNAL_BROWSER
import com.prof18.feedflow.core.model.LinkOpeningPreference.PREFERRED_BROWSER
import com.prof18.feedflow.core.model.LinkOpeningPreference.READER_MODE
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.shared.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.shared.ui.settings.SettingDropdownOption
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.toImmutableList

@Composable
fun LinkOpeningPreferenceSelector(
    currentPreference: LinkOpeningPreference,
    onPreferenceSelected: (LinkOpeningPreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current

    CompactSettingDropdownRow(
        title = strings.linkOpeningPreference,
        currentValue = currentPreference,
        options = LinkOpeningPreference.entries
            .map { preference ->
                SettingDropdownOption(preference, preference.toLabel(strings))
            }
            .toImmutableList(),
        onOptionSelected = onPreferenceSelected,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier,
    )
}

private fun LinkOpeningPreference.toLabel(strings: FeedFlowStrings): String = when (this) {
    DEFAULT -> strings.linkOpeningPreferenceDefault
    READER_MODE -> strings.linkOpeningPreferenceReaderMode
    INTERNAL_BROWSER -> strings.linkOpeningPreferenceInternalBrowser
    PREFERRED_BROWSER -> strings.linkOpeningPreferencePreferredBrowser
}
