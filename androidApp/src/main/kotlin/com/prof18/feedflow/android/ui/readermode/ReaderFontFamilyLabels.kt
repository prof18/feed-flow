package com.prof18.feedflow.android.ui.readermode

import com.prof18.feedflow.core.model.ReaderFontFamily
import com.prof18.feedflow.i18n.FeedFlowStrings

fun ReaderFontFamily.displayName(strings: FeedFlowStrings): String = familyName ?: strings.readerFontSystem
