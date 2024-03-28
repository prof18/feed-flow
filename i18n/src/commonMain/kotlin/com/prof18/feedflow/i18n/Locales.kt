package com.prof18.feedflow.i18n

val feedFlowStrings: Map<String, FeedFlowStrings> = mapOf(
    Locales.It to ItFeedFlowStrings,
    Locales.En to EnFeedFlowStrings,
    Locales.Pl to PlFeedFlowStrings,
    Locales.Sk to SkFeedFlowStrings,
    Locales.NbNo to NbNoFeedFlowStrings,
    Locales.Fr to FrFeedFlowStrings,
    Locales.Hu to HuFeedFlowStrings,
    Locales.De to DeFeedFlowStrings,
)

expect fun String.format(vararg args: Any): String
