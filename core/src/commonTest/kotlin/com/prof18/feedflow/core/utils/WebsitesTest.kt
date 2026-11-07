package com.prof18.feedflow.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class WebsitesTest {
    @Test
    fun englishFaqLivesAtTheSiteRoot() {
        assertEquals("https://www.feedflow.dev/faq/", Websites.faqUrl("en"))
        assertEquals("https://www.feedflow.dev/faq/", Websites.faqUrl("en-GB"))
    }

    @Test
    fun otherLanguagesUseTheirFolder() {
        assertEquals("https://www.feedflow.dev/it/faq/", Websites.faqUrl("it"))
        assertEquals("https://www.feedflow.dev/es/faq/", Websites.faqUrl("es"))
    }

    @Test
    fun languageIgnoresCaseAndRegion() {
        assertEquals("https://www.feedflow.dev/it/faq/", Websites.faqUrl("IT"))
        assertEquals("https://www.feedflow.dev/de/faq/", Websites.faqUrl("de-CH"))
        assertEquals("https://www.feedflow.dev/pt/faq/", Websites.faqUrl("pt_BR"))
    }

    @Test
    fun missingLanguageUsesEnglish() {
        assertEquals("https://www.feedflow.dev/faq/", Websites.faqUrl(""))
        assertEquals("https://www.feedflow.dev/faq/", Websites.faqUrl(null))
    }
}
