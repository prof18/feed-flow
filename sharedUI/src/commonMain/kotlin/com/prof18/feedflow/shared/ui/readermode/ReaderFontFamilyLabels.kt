package com.prof18.feedflow.shared.ui.readermode

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.ReaderFontFamily
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.shared.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.shared.ui.settings.SettingDropdownOption
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

fun ReaderFontFamily.displayName(strings: FeedFlowStrings): String = when (this) {
    ReaderFontFamily.SYSTEM -> strings.readerFontSystem
    ReaderFontFamily.OUTFIT -> strings.readerFontOutfit
    ReaderFontFamily.INTER -> strings.readerFontInter
    ReaderFontFamily.ATKINSON_HYPERLEGIBLE -> strings.readerFontAtkinsonHyperlegible
    ReaderFontFamily.LITERATA -> strings.readerFontLiterata
    ReaderFontFamily.SOURCE_SERIF_4 -> strings.readerFontSourceSerif4
    ReaderFontFamily.LIBRE_BASKERVILLE -> strings.readerFontLibreBaskerville
    ReaderFontFamily.LORA -> strings.readerFontLora
}

@Composable
fun readerFontFamilyOptions(
    strings: FeedFlowStrings = LocalFeedFlowStrings.current,
): ImmutableList<SettingDropdownOption<ReaderFontFamily>> =
    ReaderFontFamily.entries
        .map { SettingDropdownOption(it, it.displayName(strings)) }
        .toImmutableList()

@Composable
fun ReaderFontFamilyDropdownRow(
    title: String,
    currentFont: ReaderFontFamily,
    onFontSelected: (ReaderFontFamily) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Spacing.regular,
        vertical = Spacing.small,
    ),
) {
    CompactSettingDropdownRow(
        title = title,
        currentValue = currentFont,
        options = readerFontFamilyOptions(),
        onOptionSelected = onFontSelected,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}
