package com.prof18.feedflow.desktop.reaadermode

import com.prof18.feedflow.core.model.ReaderFontFamily
import com.prof18.feedflow.i18n.FeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

internal fun ReaderFontFamily.displayName(strings: FeedFlowStrings): String = when (this) {
    ReaderFontFamily.SYSTEM -> strings.readerFontSystem
    ReaderFontFamily.OUTFIT -> strings.readerFontOutfit
    ReaderFontFamily.INTER -> strings.readerFontInter
    ReaderFontFamily.ATKINSON_HYPERLEGIBLE -> strings.readerFontAtkinsonHyperlegible
    ReaderFontFamily.LITERATA -> strings.readerFontLiterata
    ReaderFontFamily.SOURCE_SERIF_4 -> strings.readerFontSourceSerif4
    ReaderFontFamily.LIBRE_BASKERVILLE -> strings.readerFontLibreBaskerville
    ReaderFontFamily.LORA -> strings.readerFontLora
}

internal fun readerFontFamilyChoices(
    strings: FeedFlowStrings,
): ImmutableList<Pair<ReaderFontFamily, String>> =
    ReaderFontFamily.entries
        .map { it to it.displayName(strings) }
        .toPersistentList()
