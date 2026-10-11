package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.shared.test.KoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReaderContentLanguageTest : KoinTestBase() {

    @Test
    fun `normalizes common language tags and rejects unsafe or non language values`() {
        assertEquals("it", ReaderContentLanguage.normalize("IT"))
        assertEquals("it-IT", ReaderContentLanguage.normalize("it-it"))
        assertEquals("zh-Hant-TW", ReaderContentLanguage.normalize("ZH-hant-tw"))
        assertEquals("en-US", ReaderContentLanguage.normalize("en-us"))
        listOf("", "und", "mul", "zxx", "en,fr", "en-US,fr-FR", "en\nlang=fr", "<script>", "x", "en--US")
            .forEach { assertNull(ReaderContentLanguage.normalize(it), it) }
    }

    @Test
    fun `reads only persisted Klead metadata instead of detecting source declarations`() {
        listOf(
            "<html lang='it-it'><body>ciao</body></html>",
            "<div lang='de'><p>hallo</p></div>",
            "<article xml:lang='fr'><p>bonjour</p></article>",
            "<meta http-equiv='Content-Language' content='it-IT'>",
            "<meta property='og:locale' content='zh_Hant_TW'>",
        ).forEach { assertNull(ReaderContentLanguage.fromCachedContent(it), it) }
    }

    @Test
    fun `preserved wrapper round trips language without adding spoken text`() {
        val preserved = ReaderContentLanguage.preserve("<p>Ciao</p>", "it")
        assertEquals("it", ReaderContentLanguage.fromCachedContent(preserved))
        assertEquals("<div data-feedflow-language lang=\"it\"><p>Ciao</p></div>", preserved)
    }

    @Test
    fun `persisted language is retained when article markup declares another language`() {
        val content = "<article lang='fr'><p>Bonjour</p></article>"
        val preserved = ReaderContentLanguage.preserve(content, "it-IT")
        assertEquals("it-IT", ReaderContentLanguage.fromCachedContent(preserved))
    }

    @Test
    fun `rejects malformed cache metadata and markers inside article content`() {
        listOf(
            "<div data-feedflow-language lang=\"it",
            "<div data-feedflow-language lang=\"en,fr\"><p>Text</p></div>",
            "<div data-feedflow-language lang=\"\"><p>Text</p></div>",
            "<p>${ReaderContentLanguage.preserve("Ciao", "it")}</p>",
        ).forEach { assertNull(ReaderContentLanguage.fromCachedContent(it), it) }
    }

    @Test
    fun `content without reliable language stays byte for byte unchanged`() {
        val content = "<p>Text &amp; more</p>"
        assertEquals(content, ReaderContentLanguage.preserve(content, null))
        assertEquals(content, ReaderContentLanguage.preserve(content, "en,fr"))
    }
}
