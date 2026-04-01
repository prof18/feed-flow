package com.prof18.feedflow.shared.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf

@Composable
fun FeedLayoutSelector(
    feedLayout: FeedLayout,
    onFeedLayoutSelected: (FeedLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    CompactSettingDropdownRow(
        title = strings.feedLayoutTitle,
        currentValue = feedLayout,
        options = persistentListOf(
            SettingDropdownOption(FeedLayout.LIST, strings.settingsFeedLayoutList),
            SettingDropdownOption(FeedLayout.CARD, strings.settingsFeedLayoutCard),
        ),
        onOptionSelected = onFeedLayoutSelected,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun FeedLayoutSelectorPreview() {
    FeedLayoutSelector(
        feedLayout = FeedLayout.CARD,
        onFeedLayoutSelected = {},
    )
}
