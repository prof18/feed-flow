package com.prof18.feedflow.core.utils

object Websites {
    const val MG_WEBSITE = "https://www.marcogomiero.com"
    const val FEED_FLOW_WEBSITE = "https://www.feedflow.dev"
    const val TRANSLATION_WEBSITE = "https://hosted.weblate.org/projects/feedflow/"

    // English lives at the site root. Any other language links to its folder; the website's
    // 404 page sends languages without a translated FAQ to the English one.
    fun faqUrl(languageCode: String?): String {
        val language = languageCode
            ?.substringBefore('-')
            ?.substringBefore('_')
            ?.trim()
            ?.lowercase()
        return if (language.isNullOrEmpty() || language == "en") {
            "$FEED_FLOW_WEBSITE/faq/"
        } else {
            "$FEED_FLOW_WEBSITE/$language/faq/"
        }
    }
}
