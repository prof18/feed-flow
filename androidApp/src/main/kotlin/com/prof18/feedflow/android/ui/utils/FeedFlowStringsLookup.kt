package com.prof18.feedflow.android.ui.utils

import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.i18n.feedFlowStrings
import java.util.Locale

/**
 * Strings for code outside composition (notifications, custom tabs). Tries the most specific tag
 * first and falls back to the bare language, like Lyricist does for the Compose UI.
 */
fun feedFlowStringsFor(
    locale: Locale = Locale.getDefault(),
    forceEnglish: Boolean = false,
): FeedFlowStrings {
    if (forceEnglish) return EnFeedFlowStrings
    val language = locale.language
    val script = locale.script.takeIf { it.isNotEmpty() }
    val country = locale.country.takeIf { it.isNotEmpty() }
    val candidates = listOfNotNull(
        locale.toLanguageTag(),
        if (script != null && country != null) "$language-$script-$country" else null,
        country?.let { "$language-$it" },
        script?.let { "$language-$it" },
        language,
    )
    return candidates.firstNotNullOfOrNull { feedFlowStrings[it] } ?: EnFeedFlowStrings
}
