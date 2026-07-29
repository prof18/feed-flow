package com.prof18.feedflow.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContentDirectionDetectorTest {

    @Test
    fun `detects left to right for latin text`() {
        assertEquals(
            ContentDirection.LEFT_TO_RIGHT,
            ContentDirectionDetector.detect("Apple releases macOS Ventura 13.2.1"),
        )
    }

    @Test
    fun `detects right to left for persian text`() {
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            ContentDirectionDetector.detect("تماس عراقچی با وزیر خارجه عربستان"),
        )
    }

    @Test
    fun `detects right to left for arabic text`() {
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            ContentDirectionDetector.detect("سحب نارية تهدد فرنسا وإسبانيا"),
        )
    }

    @Test
    fun `detects right to left for hebrew text`() {
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            ContentDirectionDetector.detect("טראמפ לקראת הפגישה המתוכננת"),
        )
    }

    @Test
    fun `detects left to right for cjk text`() {
        assertEquals(
            ContentDirection.LEFT_TO_RIGHT,
            ContentDirectionDetector.detect("日本のニュース"),
        )
    }

    @Test
    fun `skips leading punctuation before the first strong character`() {
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            ContentDirectionDetector.detect("«۴۵ روز زندان» و دیپورت از بلاروس"),
        )
    }

    @Test
    fun `skips leading emoji before the first strong character`() {
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            ContentDirectionDetector.detect("🔥 حملات به تاسیسات نفتی"),
        )
    }

    @Test
    fun `skips leading latin digits before the first strong character`() {
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            ContentDirectionDetector.detect("2026 سال جدید"),
        )
    }

    @Test
    fun `arabic indic digits are weak and do not decide the direction`() {
        assertEquals(
            ContentDirection.LEFT_TO_RIGHT,
            ContentDirectionDetector.detect("۴۵ items in stock"),
        )
    }

    @Test
    fun `latin prefix wins over trailing rtl text`() {
        assertEquals(
            ContentDirection.LEFT_TO_RIGHT,
            ContentDirectionDetector.detect("BBC فارسی"),
        )
    }

    @Test
    fun `rtl prefix wins over trailing latin text`() {
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            ContentDirectionDetector.detect("فارسی BBC"),
        )
    }

    @Test
    fun `honours an explicit right to left mark`() {
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            ContentDirectionDetector.detect("‏BBC News"),
        )
    }

    @Test
    fun `honours an explicit left to right mark`() {
        assertEquals(
            ContentDirection.LEFT_TO_RIGHT,
            ContentDirectionDetector.detect("‎فارسی"),
        )
    }

    @Test
    fun `ignores characters wrapped in a directional isolate`() {
        // The isolated Latin run must not decide the direction of the surrounding Persian text.
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            ContentDirectionDetector.detect("⁦BBC⁩ خبر فوری"),
        )
    }

    @Test
    fun `leading arabic comma does not decide the direction`() {
        // U+060C is a common separator, not a strong character: the Latin text after it wins.
        assertEquals(
            ContentDirection.LEFT_TO_RIGHT,
            ContentDirectionDetector.detect("، BBC News"),
        )
    }

    @Test
    fun `leading arabic comma still yields rtl for arabic text`() {
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            ContentDirectionDetector.detect("، خبر فوری"),
        )
    }

    @Test
    fun `leading arabic percent sign does not decide the direction`() {
        assertEquals(
            ContentDirection.LEFT_TO_RIGHT,
            ContentDirectionDetector.detect("٪ 50 off everything"),
        )
    }

    @Test
    fun `leading arabic ornament does not decide the direction`() {
        assertEquals(
            ContentDirection.LEFT_TO_RIGHT,
            ContentDirectionDetector.detect("۞ Chapter one"),
        )
    }

    @Test
    fun `leading byte order mark does not decide the direction`() {
        assertEquals(
            ContentDirection.LEFT_TO_RIGHT,
            ContentDirectionDetector.detect("﻿Breaking news"),
        )
    }

    @Test
    fun `leading byte order mark still yields rtl for persian text`() {
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            ContentDirectionDetector.detect("﻿خبر فوری"),
        )
    }

    @Test
    fun `leading rumi numeral does not decide the direction`() {
        assertEquals(
            ContentDirection.LEFT_TO_RIGHT,
            ContentDirectionDetector.detect("𐹩 Roman era finds"),
        )
    }

    @Test
    fun `nko digits are strongly right to left`() {
        // Unlike Arabic-Indic digits, NKo digits carry a strong right-to-left direction.
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            ContentDirectionDetector.detect("߀߁ ߊߘߊ"),
        )
    }

    @Test
    fun `leading hanifi rohingya digit does not decide the direction`() {
        assertEquals(
            ContentDirection.LEFT_TO_RIGHT,
            ContentDirectionDetector.detect("𐴰 Weekly roundup"),
        )
    }

    @Test
    fun `leading rumi digit does not decide the direction`() {
        assertEquals(
            ContentDirection.LEFT_TO_RIGHT,
            ContentDirectionDetector.detect("𐹠 Roman era finds"),
        )
    }

    @Test
    fun `arabic strong punctuation still yields rtl`() {
        // The afghani sign and the Arabic date separator are strong, unlike the comma above.
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            ContentDirectionDetector.detect("؋ 250"),
        )
    }

    @Test
    fun `resolves supplementary plane rtl scripts`() {
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            ContentDirectionDetector.detect("𞤀𞤁"), // Adlam letters
        )
    }

    @Test
    fun `returns null when there is no strong character`() {
        assertNull(ContentDirectionDetector.detect("2026 — 12:45 (#1) 🔥"))
    }

    @Test
    fun `returns null for null and empty text`() {
        assertNull(ContentDirectionDetector.detect(null))
        assertNull(ContentDirectionDetector.detect(""))
    }

    @Test
    fun `falls back to the next entry when the first has no strong character`() {
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            ContentDirectionDetector.detect(listOf("07/12/2025", "حملات به تاسیسات نفتی")),
        )
    }

    @Test
    fun `uses the first entry that has a strong character`() {
        assertEquals(
            ContentDirection.LEFT_TO_RIGHT,
            ContentDirectionDetector.detect(listOf("", "Breaking news", "خبر فوری")),
        )
    }

    @Test
    fun `returns null when no entry has a strong character`() {
        assertNull(ContentDirectionDetector.detect(listOf("", "2026 (#1)")))
    }

    @Test
    fun `returns null for an empty list`() {
        assertNull(ContentDirectionDetector.detect(emptyList()))
    }
}
