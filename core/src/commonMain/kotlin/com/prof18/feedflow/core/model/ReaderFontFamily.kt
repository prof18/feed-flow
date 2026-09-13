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

    companion object {
        const val SYSTEM_FONT_STACK: String =
            "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, " +
                "'Open Sans', 'Helvetica Neue', sans-serif"

        fun fromStorageName(name: String?): ReaderFontFamily =
            name?.let { runCatching { valueOf(it) }.getOrNull() } ?: SYSTEM
    }
}
