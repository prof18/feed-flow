package com.prof18.feedflow.core.model

enum class ReaderFontFamily {
    SYSTEM,
    OUTFIT,
    INTER,
    ATKINSON_HYPERLEGIBLE,
    LITERATA,
    SOURCE_SERIF_4,
    LIBRE_BASKERVILLE,
    LORA,
    ;

    val isBundled: Boolean
        get() = this != SYSTEM

    /** CSS / Compose font-family name for bundled faces. */
    val cssFamilyName: String
        get() = when (this) {
            SYSTEM -> SYSTEM_FONT_STACK
            OUTFIT -> "FeedFlow Outfit"
            INTER -> "FeedFlow Inter"
            ATKINSON_HYPERLEGIBLE -> "FeedFlow Atkinson Hyperlegible"
            LITERATA -> "FeedFlow Literata"
            SOURCE_SERIF_4 -> "FeedFlow Source Serif 4"
            LIBRE_BASKERVILLE -> "FeedFlow Libre Baskerville"
            LORA -> "FeedFlow Lora"
        }

    /** Value suitable for a CSS `font-family` property. */
    val cssFontFamilyValue: String
        get() = if (this == SYSTEM) {
            SYSTEM_FONT_STACK
        } else {
            "'$cssFamilyName'"
        }

    /** Bundled Regular face file name under `reader-fonts/`, or null for SYSTEM. */
    fun resourceFileName(): String? = when (this) {
        SYSTEM -> null
        OUTFIT -> "Outfit-Regular.ttf"
        INTER -> "Inter-Regular.ttf"
        ATKINSON_HYPERLEGIBLE -> "AtkinsonHyperlegible-Regular.ttf"
        LITERATA -> "Literata-Regular.ttf"
        SOURCE_SERIF_4 -> "SourceSerif4-Regular.ttf"
        LIBRE_BASKERVILLE -> "LibreBaskerville-Regular.ttf"
        LORA -> "Lora-Regular.ttf"
    }

    /** Optional separate Bold face (non-variable fonts that ship a Bold file). */
    fun boldResourceFileName(): String? = when (this) {
        ATKINSON_HYPERLEGIBLE -> "AtkinsonHyperlegible-Bold.ttf"
        else -> null
    }

    companion object {
        const val SYSTEM_FONT_STACK: String =
            "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, " +
                "'Open Sans', 'Helvetica Neue', sans-serif"

        fun fromStorageName(name: String?): ReaderFontFamily =
            name?.let { runCatching { valueOf(it) }.getOrNull() } ?: SYSTEM
    }
}
