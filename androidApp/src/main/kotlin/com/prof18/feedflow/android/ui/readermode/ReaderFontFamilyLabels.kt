package com.prof18.feedflow.android.ui.readermode

import com.prof18.feedflow.core.model.ReaderFontFamily
import com.prof18.feedflow.i18n.FeedFlowStrings

fun ReaderFontFamily.displayName(strings: FeedFlowStrings): String = when (this) {
    ReaderFontFamily.SYSTEM -> strings.readerFontSystem
    ReaderFontFamily.LITERATA -> strings.readerFontLiterata
    ReaderFontFamily.SOURCE_SERIF_4 -> strings.readerFontSourceSerif4
    ReaderFontFamily.LORA -> strings.readerFontLora
    ReaderFontFamily.ATKINSON_HYPERLEGIBLE_NEXT -> strings.readerFontAtkinsonHyperlegibleNext
}
