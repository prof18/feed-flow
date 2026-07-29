package com.prof18.feedflow.core.utils

/**
 * Resolves the reading direction of feed content, so a single article can be laid out
 * right-to-left even when the app locale is left-to-right (and the other way around).
 *
 * Follows the "first strong character" rule of the Unicode bidirectional algorithm (UAX #9, P2):
 * the direction comes from the first strongly directional character, skipping digits, punctuation,
 * symbols and anything nested inside a directional isolate. Text without any strong character
 * (only digits, emoji, punctuation) resolves to `null`, meaning the caller should keep whatever
 * direction the app locale already provides.
 *
 * Right-to-left detection is range based rather than a full Unicode character database lookup,
 * which is not available in common code. Script blocks are not the same thing as bidi classes, so
 * [NON_STRONG_RANGES] carves out the punctuation, symbols and number signs that live inside those
 * blocks without being strong themselves: without it a title such as "، BBC News" would mirror on
 * its leading Arabic comma instead of on its first real letter. Digits are excluded the same way.
 * Combining marks are deliberately left in: they cannot realistically open a title, and when they
 * do they belong to the right-to-left script around them anyway.
 */
object ContentDirectionDetector {

    fun detect(text: String?): ContentDirection? {
        if (text.isNullOrEmpty()) {
            return null
        }

        var isolateDepth = 0
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAtIndex(index)
            index += if (codePoint > MAX_BMP_CODE_POINT) 2 else 1

            when {
                codePoint in FIRST_ISOLATE_INITIATOR..LAST_ISOLATE_INITIATOR -> isolateDepth++
                codePoint == POP_DIRECTIONAL_ISOLATE -> isolateDepth = (isolateDepth - 1).coerceAtLeast(0)
                isolateDepth > 0 -> Unit
                else -> strongDirectionOf(codePoint)?.let { return it }
            }
        }
        return null
    }

    /** Returns the direction of the first entry that has a strong character, or null if none has. */
    fun detect(texts: List<String>): ContentDirection? = texts.firstNotNullOfOrNull { detect(it) }

    private fun strongDirectionOf(codePoint: Int): ContentDirection? = when {
        codePoint == LEFT_TO_RIGHT_MARK -> ContentDirection.LEFT_TO_RIGHT
        codePoint == RIGHT_TO_LEFT_MARK -> ContentDirection.RIGHT_TO_LEFT
        STRONG_RTL_DIGIT_RANGES.any { range -> codePoint in range } -> ContentDirection.RIGHT_TO_LEFT
        codePoint.isDigitCodePoint() -> null
        NON_STRONG_RANGES.any { range -> codePoint in range } -> null
        RTL_RANGES.any { range -> codePoint in range } -> ContentDirection.RIGHT_TO_LEFT
        codePoint.isBmpLetter() -> ContentDirection.LEFT_TO_RIGHT
        else -> null
    }

    private fun Int.isDigitCodePoint(): Boolean = this <= MAX_BMP_CODE_POINT && toChar().isDigit()

    private fun Int.isBmpLetter(): Boolean = this <= MAX_BMP_CODE_POINT && toChar().isLetter()

    private fun String.codePointAtIndex(index: Int): Int {
        val high = this[index]
        if (high.isHighSurrogate() && index + 1 < length) {
            val low = this[index + 1]
            if (low.isLowSurrogate()) {
                return SUPPLEMENTARY_PLANE_OFFSET +
                    ((high.code - MIN_HIGH_SURROGATE) shl SURROGATE_SHIFT) +
                    (low.code - MIN_LOW_SURROGATE)
            }
        }
        return high.code
    }

    /**
     * Weak and neutral code points (bidi classes AN, CS, ES, ET, ON and BN) that sit inside the
     * right-to-left blocks below. They must not decide the direction, so scanning continues past
     * them to the first character that really is strong.
     */
    /**
     * Digits that are strongly right-to-left, unlike the Arabic-Indic ones. They have to be
     * checked before the generic digit test, which would otherwise discard them as weak.
     */
    private val STRONG_RTL_DIGIT_RANGES = listOf(
        0x07C0..0x07C9, // NKo digits
        0x1E950..0x1E959, // Adlam digits
    )

    private val NON_STRONG_RANGES = listOf(
        0x0600..0x0607, // Arabic number signs, Arabic-Indic roots
        0x0609..0x060A, // Arabic-Indic per mille and per ten thousand signs
        0x060C..0x060C, // Arabic comma
        0x060E..0x060F, // Arabic poetic verse sign, Arabic sign misra
        0x066A..0x066C, // Arabic percent, decimal and thousands separators
        0x06DD..0x06DE, // Arabic end of ayah, start of rub el hizb
        0x06E9..0x06E9, // Arabic place of sajdah
        0x07F6..0x07F9, // NKo symbols and punctuation
        0x0890..0x0891, // Arabic pound and piastre marks
        0x08E2..0x08E2, // Arabic disputed end of ayah
        0xFB29..0xFB29, // Hebrew alternative plus sign
        0xFD3E..0xFD4F, // Ornate parentheses and Arabic ligature symbols
        0xFDCF..0xFDCF, // Arabic ligature salaamuhu alaynaa
        0xFDFD..0xFDFF, // Arabic ligature bismillah and friends
        0xFEFF..0xFEFF, // Byte order mark, which can easily open a badly trimmed title
        0x1091F..0x1091F, // Phoenician word separator
        0x10B39..0x10B3F, // Avestan punctuation
        0x10D30..0x10D39, // Hanifi Rohingya digits, which are weak unlike the NKo ones
        0x10D40..0x10D49, // Garay digits
        0x10D6E..0x10D6E, // Garay hyphen
        0x10E60..0x10E7E, // Rumi digits and number symbols
        0x1EEF0..0x1EEF1, // Arabic mathematical operators
    )

    private val RTL_RANGES = listOf(
        // Hebrew, Arabic, Syriac, Thaana, NKo, Samaritan, Mandaic and their extensions
        0x0590..0x08FF,
        0xFB1D..0xFB4F, // Hebrew presentation forms
        0xFB50..0xFDFF, // Arabic presentation forms-A
        0xFE70..0xFEFF, // Arabic presentation forms-B
        0x10800..0x10FFF, // Cypriot, Phoenician, Kharoshthi and other historic RTL scripts
        0x1E800..0x1EFFF, // Mende Kikakui, Adlam, Arabic mathematical symbols
    )

    private const val LEFT_TO_RIGHT_MARK = 0x200E
    private const val RIGHT_TO_LEFT_MARK = 0x200F
    private const val FIRST_ISOLATE_INITIATOR = 0x2066
    private const val LAST_ISOLATE_INITIATOR = 0x2068
    private const val POP_DIRECTIONAL_ISOLATE = 0x2069
    private const val MAX_BMP_CODE_POINT = 0xFFFF
    private const val SUPPLEMENTARY_PLANE_OFFSET = 0x10000
    private const val MIN_HIGH_SURROGATE = 0xD800
    private const val MIN_LOW_SURROGATE = 0xDC00
    private const val SURROGATE_SHIFT = 10
}

enum class ContentDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
}
