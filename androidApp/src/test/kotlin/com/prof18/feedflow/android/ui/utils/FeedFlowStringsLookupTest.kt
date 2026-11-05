package com.prof18.feedflow.android.ui.utils

import com.prof18.feedflow.i18n.DeFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.ItFeedFlowStrings
import com.prof18.feedflow.i18n.PtBrFeedFlowStrings
import com.prof18.feedflow.i18n.ZhCnFeedFlowStrings
import com.prof18.feedflow.i18n.ZhTwFeedFlowStrings
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.Locale

class FeedFlowStringsLookupTest {

    @Test
    fun `region tag falls back to the language strings`() {
        assertSame(ItFeedFlowStrings, feedFlowStringsFor(Locale.forLanguageTag("it-IT")))
        assertSame(DeFeedFlowStrings, feedFlowStringsFor(Locale.forLanguageTag("de-AT")))
    }

    @Test
    fun `region specific translations are kept`() {
        assertSame(PtBrFeedFlowStrings, feedFlowStringsFor(Locale.forLanguageTag("pt-BR")))
        assertSame(ZhCnFeedFlowStrings, feedFlowStringsFor(Locale.forLanguageTag("zh-Hans-CN")))
        assertSame(ZhTwFeedFlowStrings, feedFlowStringsFor(Locale.forLanguageTag("zh-Hant-TW")))
        assertSame(ZhTwFeedFlowStrings, feedFlowStringsFor(Locale.forLanguageTag("zh-TW")))
    }

    @Test
    fun `unicode extensions do not break the lookup`() {
        assertSame(PtBrFeedFlowStrings, feedFlowStringsFor(Locale.forLanguageTag("pt-BR-u-mu-celsius")))
        assertSame(ItFeedFlowStrings, feedFlowStringsFor(Locale.forLanguageTag("it-IT-u-fw-mon")))
    }

    @Test
    fun `unknown language falls back to English`() {
        assertSame(EnFeedFlowStrings, feedFlowStringsFor(Locale.forLanguageTag("ko-KR")))
    }

    @Test
    fun `force English wins over the locale`() {
        assertSame(EnFeedFlowStrings, feedFlowStringsFor(Locale.forLanguageTag("it-IT"), forceEnglish = true))
    }
}
