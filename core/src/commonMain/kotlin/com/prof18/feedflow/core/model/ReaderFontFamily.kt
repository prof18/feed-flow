package com.prof18.feedflow.core.model

enum class ReaderFontFamily(
    /** Brand name shown in the font pickers; not translated. Null for [SYSTEM], which uses a localized label. */
    val familyName: String?,
    val cssFamilyName: String?,
    val fileName: String?,
    val italicFileName: String?,
    val cssWeightRange: String?,
) {
    SYSTEM(null, null, null, null, null),
    LITERATA("Literata", "FeedFlow Literata", "Literata-VF.ttf", "Literata-Italic-VF.ttf", "200 900"),
    SOURCE_SERIF_4(
        "Source Serif 4",
        "FeedFlow Source Serif 4",
        "SourceSerif4-VF.ttf",
        "SourceSerif4-Italic-VF.ttf",
        "200 900",
    ),
    IBM_PLEX_SANS(
        "IBM Plex Sans",
        "FeedFlow IBM Plex Sans",
        "IBMPlexSans-VF.ttf",
        "IBMPlexSans-Italic-VF.ttf",
        "100 700",
    ),
    ATKINSON_HYPERLEGIBLE_NEXT(
        "Atkinson Hyperlegible Next",
        "FeedFlow Atkinson Hyperlegible Next",
        "AtkinsonHyperlegibleNext-VF.ttf",
        "AtkinsonHyperlegibleNext-Italic-VF.ttf",
        "200 800",
    ),
    ;

    val isBundled: Boolean
        get() = this != SYSTEM

    /** The bundled files lack Greek and Cyrillic, so those articles fall back to the system font per glyph. */
    val isLatinScriptOnly: Boolean
        get() = this == ATKINSON_HYPERLEGIBLE_NEXT

    /** Value for a CSS `font-family` property; bundled fonts fall back to the system stack per glyph. */
    val cssFontFamilyValue: String
        get() = cssFamilyName?.let { "'$it', $SYSTEM_FONT_STACK" } ?: SYSTEM_FONT_STACK

    companion object {
        const val SYSTEM_FONT_STACK: String =
            "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, " +
                "'Open Sans', 'Helvetica Neue', sans-serif"

        fun fromStorageName(name: String?): ReaderFontFamily =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
