package com.prof18.feedflow.android.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.prof18.feedflow.shared.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.shared.ui.settings.SettingDropdownOption
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun WidgetTextColorSelector(
    currentMode: WidgetTextColorMode,
    onModeSelected: (WidgetTextColorMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current

    CompactSettingDropdownRow(
        title = strings.widgetTextColorTitle,
        currentValue = currentMode,
        options = WidgetTextColorMode.entries
            .map { mode -> SettingDropdownOption(mode, mode.toLabel(strings)) }
            .toImmutableList(),
        onOptionSelected = onModeSelected,
        modifier = modifier,
    )
}

private fun WidgetTextColorMode.toLabel(strings: com.prof18.feedflow.i18n.FeedFlowStrings): String {
    return when (this) {
        WidgetTextColorMode.AUTOMATIC -> strings.widgetTextColorAutomatic
        WidgetTextColorMode.LIGHT -> strings.widgetTextColorLight
        WidgetTextColorMode.DARK -> strings.widgetTextColorDark
    }
}

@Preview
@Composable
private fun WidgetTextColorSelectorPreview() {
    FeedFlowTheme {
        WidgetTextColorSelector(
            currentMode = WidgetTextColorMode.AUTOMATIC,
            onModeSelected = {},
        )
    }
}
