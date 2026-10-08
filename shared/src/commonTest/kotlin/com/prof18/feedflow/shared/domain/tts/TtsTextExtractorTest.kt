package com.prof18.feedflow.shared.domain.tts

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TtsTextExtractorTest {
    @Test
    fun `literal title is prepended and body entities are decoded`() = runTest {
        assertEquals(
            listOf("How &amp; works", "A & B"),
            TtsTextExtractor.extract(" How  &amp; works ", "<p>A &amp; B</p>"),
        )
        assertEquals(listOf("© 😀"), TtsTextExtractor.extract(null, "<p>&#169; &#x1F600;</p>"))
    }

    @Test
    fun `hidden and media subtrees are skipped while figure narrative remains`() = runTest {
        val html = """
            <p>Keep</p><script>script</script><div hidden>hidden</div>
            <p aria-hidden="true">aria</p><img alt="photo"><figure><img src="x">
            <figcaption>caption</figcaption><blockquote>Quoted words</blockquote></figure>
        """.trimIndent()
        assertEquals(listOf("Keep", "Quoted words"), TtsTextExtractor.extract(null, html))
    }

    @Test
    fun `all configured non narrative subtrees are omitted`() = runTest {
        val html = """
            <p>Before</p><style>style marker</style><iframe>iframe marker</iframe>
            <svg><text>svg marker</text></svg><img alt="image marker"><picture>picture marker</picture>
            <video>video marker</video><audio>audio marker</audio><object>object marker</object>
            <embed src="embed marker"><canvas>canvas marker</canvas><p>After</p>
        """.trimIndent()
        assertEquals(listOf("Before", "After"), TtsTextExtractor.extract(null, html))
    }

    @Test
    fun `inline text keeps natural spacing and block table separators`() = runTest {
        assertEquals(
            listOf("A bold word", "left right"),
            TtsTextExtractor.extract(
                null,
                "<p>A <b>bold</b> word</p><table><tr><td>left</td><td>right</td></tr></table>",
            ),
        )
    }

    @Test
    fun `unicode whitespace is normalized within a block`() = runTest {
        assertEquals(listOf("A B C D"), TtsTextExtractor.extract(null, "<p>A\u2003B\u2028C&nbsp;D</p>"))
    }

    @Test
    fun `only renderer duplicate headings are omitted`() = runTest {
        assertEquals(
            listOf("Title", "Different", "Same", "Same"),
            TtsTextExtractor.extract("Title", "<h1>title</h1><h2>Title</h2><h2>Different</h2><p>Same</p><p>Same</p>"),
        )
        assertEquals(
            listOf("First", "Body"),
            TtsTextExtractor.extract(null, "<h1>First</h1><h2> first </h2><p>Body</p>"),
        )
        assertEquals(
            listOf("Repeat", "Repeat"),
            TtsTextExtractor.extract(null, "<h2>Repeat</h2><h2>Repeat</h2>"),
        )
        assertEquals(
            listOf("TitleCaption", "Body"),
            TtsTextExtractor.extract(
                "TitleCaption",
                "<h1>Title<span aria-hidden='true'>Caption</span></h1><p>Body</p>",
            ),
        )
        assertEquals(
            listOf("Words"),
            TtsTextExtractor.extract("", "<h1></h1><h2> </h2><h2>Words</h2>"),
        )
        assertEquals(
            listOf("İ", "i"),
            TtsTextExtractor.extract("İ", "<h1>i</h1>"),
        )
    }

    @Test
    fun `empty content emits title only and image only emits nothing`() = runTest {
        assertEquals(emptyList(), TtsTextExtractor.extract("  ", "<img src='x'>"))
        assertEquals(emptyList(), TtsTextExtractor.extract(null, "<img src='x'>"))
        assertEquals(listOf("Only title"), TtsTextExtractor.extract("Only title", "<img src='x'>"))
    }

    @Test
    fun `missing and blank titles retain distinct duplicate heading rules`() = runTest {
        val html = "<h1>Article</h1><h2>Article</h2><p>Body</p>"
        assertEquals(listOf("Article", "Body"), TtsTextExtractor.extract(null, html))
        assertEquals(listOf("Article", "Article", "Body"), TtsTextExtractor.extract("", html))
    }

    @Test
    fun `inline display and visibility omit hidden text but allow visible descendants`() = runTest {
        val html = """
            <p>Before</p><div style='display:none'>Absent</div>
            <div style='visibility:hidden'>Hidden <span style='visibility:visible'>Restored</span></div>
            <p>After</p>
        """.trimIndent()
        assertEquals(listOf("Before", "Restored", "After"), TtsTextExtractor.extract(null, html))
        assertEquals(
            listOf("Restored", "After"),
            TtsTextExtractor.extract(
                null,
                "<div style='visibility:hidden'>" +
                    "<span style='visibility:visible'>Restored</span></div><span>After</span>",
            ),
        )
    }

    @Test
    fun `inline declarations follow last value and important precedence`() = runTest {
        val html = """
            <p style='display:none; display:block'>Shown</p>
            <p style='display:block; display:none'>Gone</p>
            <p style='display:none !important; display:block'>Still gone</p>
            <p style='display:none; display:block !important'>Important shown</p>
            <p style='visibility:hidden !important; visibility:visible'>Invisible</p>
            <p style='visibility:hidden; visibility:visible !important'>Visible</p>
            <p style='background:url("a;b"); display:block'>URL shown</p>
            <p style='background:url("a;b;display:none"); color:red'>URL still shown</p>
            <p style='/* display:none */ color:red'>Comment shown</p>
            <p style='display:/* comment */none'><span style='visibility:visible'>Display subtree gone</span></p>
            <p style='display:none; display:bogus'>Invalid display hidden</p>
            <p style='visibility:hidden; visibility:bogus'>Invalid visibility hidden</p>
            <p style='visibility:hidden; visibility:initial'>Initial visible</p>
            <p style='display:none; display:inline flow'>Compound shown</p>
        """.trimIndent()
        assertEquals(
            listOf(
                "Shown",
                "Important shown",
                "Visible",
                "URL shown",
                "URL still shown",
                "Comment shown",
                "Initial visible",
                "Compound shown",
            ),
            TtsTextExtractor.extract(null, html),
        )
    }

    @Test
    fun `long text is bounded and preserves all content without splitting surrogate pairs`() = runTest {
        val source = "a".repeat(499) + "😀" + "b".repeat(501)
        val chunks = TtsTextExtractor.extract(null, "<p>$source</p>")
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.all { it.length <= TtsLimits.MAX_SEGMENT_LENGTH })
        assertEquals(source, chunks.joinToString(""))
        assertTrue(chunks.none { it.firstOrNull()?.isLowSurrogate() == true })
        assertTrue(chunks.none { it.lastOrNull()?.isHighSurrogate() == true })
    }

    @Test
    fun `long literal title is split within the limit`() = runTest {
        val title = "A".repeat(1_101)
        val chunks = TtsTextExtractor.extract(title, "")
        assertEquals(title, chunks.joinToString(""))
        assertTrue(chunks.all { it.length <= TtsLimits.MAX_SEGMENT_LENGTH })
        assertEquals(listOf(500, 500, 101), chunks.map(String::length))
    }

    @Test
    fun `whitespace is used when no sentence boundary fits`() = runTest {
        val first = "x".repeat(490)
        val second = "y".repeat(30)
        val chunks = TtsTextExtractor.extract(null, "<p>$first $second</p>")
        assertEquals(listOf(first, second), chunks)
    }

    @Test
    fun `cjk sentence punctuation is a preferred boundary`() = runTest {
        val first = "中".repeat(480) + "。"
        val second = "乙".repeat(30)
        val chunks = TtsTextExtractor.extract(null, "<p>$first$second</p>")
        assertEquals(listOf(first, second), chunks)
        assertTrue(chunks.all { it.length <= TtsLimits.MAX_SEGMENT_LENGTH })
    }

    @Test
    fun `literal angle brackets and entity sequences in title remain unchanged`() = runTest {
        val title = "Use <tag> &amp; literally"
        assertEquals(listOf(title), TtsTextExtractor.extract(title, ""))
    }

    @Test
    fun `sentence boundaries are preferred when splitting`() = runTest {
        val first = "a".repeat(480) + "."
        val second = "b".repeat(30)
        val chunks = TtsTextExtractor.extract(null, "<p>$first $second</p>")
        assertEquals(first, chunks.first())
        assertEquals(second, chunks.last())
    }

    @Test
    fun `block elements and line breaks separate speech blocks`() = runTest {
        assertEquals(
            listOf("one", "two", "three", "four", "five"),
            TtsTextExtractor.extract(null, "<div>one<p>two</p>three</div><hr>four<br>five"),
        )
    }

    @Test
    fun `already cancelled extraction throws cancellation`() = runTest {
        var extractorFailure: Throwable? = null
        val propagated = assertFailsWith<CancellationException> {
            withContext(Job()) {
                currentCoroutineContext()[Job]?.cancel()
                try {
                    TtsTextExtractor.extract(null, "<p>small input</p>")
                } catch (expectedCancellation: CancellationException) {
                    extractorFailure = expectedCancellation
                    throw expectedCancellation
                }
            }
        }
        assertIs<CancellationException>(assertNotNull(extractorFailure))
        assertIs<CancellationException>(propagated)
    }
}

private fun runTest(block: suspend () -> Unit) = kotlinx.coroutines.test.runTest { block() }
