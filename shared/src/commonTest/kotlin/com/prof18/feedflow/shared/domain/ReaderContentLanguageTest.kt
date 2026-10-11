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
    fun `reads html and xml language declarations`() {
        assertEquals("it-IT", ReaderContentLanguage.fromHtml("<html lang='it-it'><body>ciao</body></html>"))
        assertEquals("zh-Hant-TW", ReaderContentLanguage.fromHtml("<html xml:lang='zh-hant-tw'>文字</html>"))
    }

    @Test
    fun `article language overrides html default when unambiguous`() {
        val html = "<html lang='en'><body><article lang='it-IT'>Ciao</article></body></html>"
        assertEquals("it-IT", ReaderContentLanguage.fromHtml(html))
        val conflicting = "<html lang='en'><article lang='it'>A</article><main lang='fr'>B</main></html>"
        assertEquals("en", ReaderContentLanguage.fromHtml(conflicting))
    }

    @Test
    fun `uses body and feed fragment root declarations`() {
        assertEquals("fr", ReaderContentLanguage.fromHtml("<body lang='fr'><p>bonjour</p></body>"))
        assertEquals("de", ReaderContentLanguage.fromHtml("<div lang='de'><p>hallo</p></div>"))
    }

    @Test
    fun `reads head metadata after source declarations`() {
        val contentLanguage = "<head><meta http-equiv='Content-Language' content='it-IT'></head><body>ciao</body>"
        val locale = "<head><meta property='og:locale' content='zh_Hant_TW'>" +
            "<meta property='og:locale:alternate' content='fr'></head>"
        assertEquals("it-IT", ReaderContentLanguage.fromHtml(contentLanguage))
        assertEquals("zh-Hant-TW", ReaderContentLanguage.fromHtml(locale))
        assertNull(ReaderContentLanguage.fromHtml("<meta property='og:locale:alternate' content='fr'>"))
    }

    @Test
    fun `ignores languages on quotes and code blocks`() {
        val html = "<html lang='en'><blockquote lang='fr'>bonjour</blockquote><code lang='it'>ciao</code></html>"
        assertEquals("en", ReaderContentLanguage.fromHtml(html))
    }

    @Test
    fun `preserved wrapper round trips language without adding spoken text`() {
        val preserved = ReaderContentLanguage.preserve("<p>Ciao</p>", "it")
        assertEquals("it", ReaderContentLanguage.fromHtml(preserved))
        assertEquals("<div data-feedflow-language lang=\"it\"><p>Ciao</p></div>", preserved)
    }

    @Test
    fun `content without reliable language stays byte for byte unchanged`() {
        val content = "<p>Text &amp; more</p>"
        assertEquals(content, ReaderContentLanguage.preserve(content, null))
        assertEquals(content, ReaderContentLanguage.preserve(content, "en,fr"))
    }
}
