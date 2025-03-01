package com.prof18.feedflow.i18n

val feedFlowStrings: Map<String, FeedFlowStrings> = mapOf(
    Locales.It to ItFeedFlowStrings,
    Locales.En to EnFeedFlowStrings,
    Locales.Pl to PlFeedFlowStrings,
    Locales.Sk to SkFeedFlowStrings,
    Locales.Fr to FrFeedFlowStrings,
    Locales.Hu to HuFeedFlowStrings,
    Locales.De to DeFeedFlowStrings,
    Locales.Es to EsFeedFlowStrings,
    Locales.Et to EtFeedFlowStrings,
    Locales.Ru to RuFeedFlowStrings,

    Locales.NbNo to NbNoFeedFlowStrings,
    "nb-NO" to NbNoFeedFlowStrings,

    Locales.PtBr to PtBrFeedFlowStrings,
    "pt-BR" to PtBrFeedFlowStrings,

    Locales.ZhCn to ZhCnFeedFlowStrings,
    "zh-CN" to ZhCnFeedFlowStrings,
    "zh-Hans-CN" to ZhCnFeedFlowStrings,

    Locales.Gl to GlFeedFlowStrings,
    Locales.Bg to BgFeedFlowStrings,
    Locales.Vi to ViFeedFlowStrings,

    Locales.Ta to TaFeedFlowStrings,
    "ta-IN" to TaFeedFlowStrings,

    Locales.Iw to IwFeedFlowStrings,
    "he" to IwFeedFlowStrings,

    Locales.Uk to UkFeedFlowStrings,
)

expect fun String.format(vararg args: Any): String

@Suppress("UnusedPrivateProperty")
// This is a trick to be sure that KSP re-generates the strings when there's no code updates
private const val StringsVersion = 102
