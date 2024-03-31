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
    Locales.PtBr to PtBrFeedFlowStrings,
)

expect fun String.format(vararg args: Any): String

@Suppress("UnusedPrivateProperty")
// This is a trick to be sure that KSP re-generates the strings when there's no code updates
private const val StringsVersion = 2
