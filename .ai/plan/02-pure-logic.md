# Phase 1: Pure Logic Tests (Quick Wins)

**Goal**: Test pure functions with no external dependencies - just input → output.

These tests are fast, easy to write, and provide immediate value.

---

## Task 1.1: Utils Tests

### UrlUtils Tests

**File**: `shared/src/commonTest/kotlin/.../utils/UrlUtilsTest.kt`

```kotlin
package com.prof18.feedflow.shared.utils

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.domain
import io.kotest.property.checkAll
import kotlin.test.Test

class UrlUtilsTest {

    @Test
    fun `should normalize HTTP to HTTPS`() {
        val url = "http://example.com/feed.xml"
        val normalized = UrlUtils.normalizeUrl(url)
        normalized shouldBe "https://example.com/feed.xml"
    }

    @Test
    fun `should preserve HTTPS URLs`() = runTest {
        checkAll(50, Arb.domain()) { domain ->
            val url = "https://$domain/feed.xml"
            val normalized = UrlUtils.normalizeUrl(url)
            normalized shouldBe url
        }
    }

    @Test
    fun `should handle invalid URLs gracefully`() {
        val invalidUrls = listOf(
            "not a url",
            "ftp://example.com",
            "",
            "   ",
        )
        invalidUrls.forEach { url ->
            val result = UrlUtils.isValidUrl(url)
            result shouldBe false
        }
    }

    @Test
    fun `should extract domain from URL`() {
        val testCases = mapOf(
            "https://example.com/feed.xml" to "example.com",
            "https://blog.example.com/rss" to "blog.example.com",
            "https://example.com:8080/feed" to "example.com",
        )
        testCases.forEach { (url, expected) ->
            UrlUtils.extractDomain(url) shouldBe expected
        }
    }
}
```

**Acceptance Criteria**:
- ✅ All UrlUtils functions tested
- ✅ Property-based tests for valid URLs
- ✅ Edge cases covered (empty, invalid, special chars)

---

### RetryHelpers Tests

**File**: `shared/src/commonTest/kotlin/.../utils/RetryHelpersTest.kt`

```kotlin
package com.prof18.feedflow.shared.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import java.io.IOException

class RetryHelpersTest {

    @Test
    fun `should retry specified number of times`() = runTest {
        checkAll(Arb.int(1..5)) { maxRetries ->
            var attempts = 0

            shouldThrow<IOException> {
                retryWithExponentialBackoff(maxRetries = maxRetries) {
                    attempts++
                    throw IOException("Network error")
                }
            }

            attempts shouldBe maxRetries + 1  // Initial attempt + retries
        }
    }

    @Test
    fun `should succeed on first attempt if no error`() = runTest {
        var attempts = 0

        val result = retryWithExponentialBackoff(maxRetries = 3) {
            attempts++
            "success"
        }

        result shouldBe "success"
        attempts shouldBe 1
    }

    @Test
    fun `should succeed after some retries`() = runTest {
        var attempts = 0

        val result = retryWithExponentialBackoff(maxRetries = 5) {
            attempts++
            if (attempts < 3) {
                throw IOException("Temporary error")
            }
            "success"
        }

        result shouldBe "success"
        attempts shouldBe 3
    }
}
```

**Acceptance Criteria**:
- ✅ Retry logic tested with property-based inputs
- ✅ Success and failure paths covered
- ✅ Exponential backoff behavior verified

---

### ArchiveIS Tests

**File**: `shared/src/commonTest/kotlin/.../utils/ArchiveISTest.kt`

```kotlin
package com.prof18.feedflow.shared.utils

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.domain
import io.kotest.property.checkAll
import kotlin.test.Test

class ArchiveISTest {

    @Test
    fun `should generate archive URL`() = runTest {
        checkAll(50, Arb.domain()) { domain ->
            val originalUrl = "https://$domain/article/123"
            val archiveUrl = ArchiveIS.getArchiveUrl(originalUrl)

            archiveUrl.shouldContain("archive.is")
            archiveUrl.shouldContain(originalUrl)
        }
    }

    @Test
    fun `should handle empty URL`() {
        val result = ArchiveIS.getArchiveUrl("")
        result shouldBe ""
    }
}
```

**Acceptance Criteria**:
- ✅ Archive URL generation tested
- ✅ Edge cases handled

---

## Task 1.2: Mapper Tests

### RssChannelMapper Tests

**File**: `shared/src/commonTest/kotlin/.../domain/mappers/RssChannelMapperTest.kt`

```kotlin
package com.prof18.feedflow.shared.domain.mappers

import com.prof18.feedflow.core.model.RssChannel
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.domain
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlin.test.Test

class RssChannelMapperTest {

    private val validRssChannelArb = arbitrary {
        RssChannel(
            title = Arb.string(10..100).bind(),
            url = Arb.domain().map { "https://$it/article" }.bind(),
            subtitle = Arb.string(50..500).orNull().bind(),
            imageUrl = Arb.domain().map { "https://$it/image.jpg" }.orNull().bind(),
            pubDate = System.currentTimeMillis(),
        )
    }

    @Test
    fun `should skip items with null URL`() = runTest {
        checkAll(100, arbitrary {
            RssChannel(
                title = Arb.string().bind(),
                url = null,  // Force null URL
                subtitle = Arb.string().orNull().bind(),
                imageUrl = null,
                pubDate = System.currentTimeMillis(),
            )
        }) { channel ->
            val result = channel.toFeedItem(feedSourceId = "test-source")
            result.shouldBeNull()
        }
    }

    @Test
    fun `should map valid data correctly`() = runTest {
        checkAll(50, validRssChannelArb) { channel ->
            val result = channel.toFeedItem(feedSourceId = "test-source")

            result.shouldNotBeNull()
            result.title shouldBe channel.title
            result.url shouldBe channel.url
            result.subtitle shouldBe channel.subtitle
            result.feedSourceId shouldBe "test-source"
        }
    }

    @Test
    fun `should convert HTTP image URLs to HTTPS`() {
        val channel = RssChannel(
            title = "Article",
            url = "https://example.com/article",
            subtitle = null,
            imageUrl = "http://example.com/image.jpg",
            pubDate = System.currentTimeMillis(),
        )

        val result = channel.toFeedItem(feedSourceId = "test")
        result?.imageUrl shouldStartWith "https://"
    }

    @Test
    fun `should extract YouTube thumbnail`() {
        val channel = RssChannel(
            title = "Video",
            url = "https://youtube.com/watch?v=ABC123",
            subtitle = null,
            imageUrl = null,
            pubDate = System.currentTimeMillis(),
        )

        val result = channel.toFeedItem(feedSourceId = "test")
        result?.imageUrl.shouldNotBeNull()
        result?.imageUrl shouldStartWith "https://img.youtube.com/vi/ABC123/"
    }

    @Test
    fun `should use GUID as URL fallback`() {
        val guid = "https://example.com/unique-article"
        val channel = RssChannel(
            title = "Article",
            url = null,
            guid = guid,
            subtitle = null,
            imageUrl = null,
            pubDate = System.currentTimeMillis(),
        )

        val result = channel.toFeedItem(feedSourceId = "test")
        result?.url shouldBe guid
    }
}
```

**Acceptance Criteria**:
- ✅ All mapping logic tested
- ✅ Null handling verified
- ✅ YouTube thumbnail extraction works
- ✅ HTTP→HTTPS conversion works
- ✅ GUID fallback tested

---

### GReader DTO Mapper Tests

**File**: `feedSync/greader/src/commonTest/kotlin/.../domain/mapping/ItemContentDTOMapperTest.kt`

```kotlin
package com.prof18.feedflow.feedsync.greader.domain.mapping

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlin.test.Test

class ItemContentDTOMapperTest {

    @Test
    fun `should map valid DTO`() {
        val dto = ItemContentDTO(
            id = "tag:google.com,2005:reader/item/abc123",
            title = "Test Article",
            canonical = listOf(
                CanonicalDTO(href = "https://example.com/article")
            ),
            summary = SummaryDTO(content = "Article content"),
            published = 1234567890,
            categories = listOf("user/-/state/com.google/read"),
        )

        val result = dto.toFeedItem(feedSourceId = "source-1")

        result.id shouldBe "abc123"
        result.title shouldBe "Test Article"
        result.url shouldBe "https://example.com/article"
        result.isRead shouldBe true
    }

    @Test
    fun `should parse read status from categories`() {
        val readDto = ItemContentDTO(
            id = "item/123",
            categories = listOf("user/-/state/com.google/read"),
        )
        readDto.toFeedItem("src").isRead shouldBe true

        val unreadDto = ItemContentDTO(
            id = "item/124",
            categories = emptyList(),
        )
        unreadDto.toFeedItem("src").isRead shouldBe false
    }

    @Test
    fun `should extract image from content HTML`() {
        val dto = ItemContentDTO(
            id = "item/125",
            summary = SummaryDTO(
                content = """<p>Article text</p><img src="https://example.com/image.jpg" />"""
            ),
        )

        val result = dto.toFeedItem("src")
        result.imageUrl shouldBe "https://example.com/image.jpg"
    }
}
```

**Acceptance Criteria**:
- ✅ DTO mapping tested
- ✅ Read/starred status parsing works
- ✅ Image extraction from HTML content tested

---

## Task 1.3: HtmlParser Tests

**File**: `shared/src/jvmTest/kotlin/.../utils/JvmHtmlParserTest.kt`

```kotlin
package com.prof18.feedflow.shared.utils

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class JvmHtmlParserTest {

    private val parser = JvmHtmlParser()

    @Test
    fun `should extract text from HTML`() {
        val html = """
            <html>
                <body>
                    <p>First paragraph</p>
                    <p>Second paragraph</p>
                </body>
            </html>
        """.trimIndent()

        val text = parser.parseHtmlText(html)
        text shouldBe "First paragraph Second paragraph"
    }

    @Test
    fun `should find RSS feed link`() {
        val html = """
            <html>
                <head>
                    <link rel="alternate" type="application/rss+xml" href="/feed.xml" />
                </head>
            </html>
        """.trimIndent()

        val feedUrl = parser.findFeedUrl(html, baseUrl = "https://example.com")
        feedUrl shouldBe "https://example.com/feed.xml"
    }

    @Test
    fun `should find Atom feed link`() {
        val html = """
            <html>
                <head>
                    <link rel="alternate" type="application/atom+xml" href="/atom.xml" />
                </head>
            </html>
        """.trimIndent()

        val feedUrl = parser.findFeedUrl(html, baseUrl = "https://example.com")
        feedUrl shouldBe "https://example.com/atom.xml"
    }

    @Test
    fun `should extract favicon URL`() {
        val html = """
            <html>
                <head>
                    <link rel="icon" href="/favicon.ico" />
                </head>
            </html>
        """.trimIndent()

        val faviconUrl = parser.findFaviconUrl(html, baseUrl = "https://example.com")
        faviconUrl shouldBe "https://example.com/favicon.ico"
    }

    @Test
    fun `should handle malformed HTML`() {
        val html = "<html><p>Unclosed paragraph<div>Unclosed div"

        val text = parser.parseHtmlText(html)
        text.shouldNotBeNull()  // Should not crash
    }

    @Test
    fun `should strip script and style tags`() {
        val html = """
            <html>
                <body>
                    <p>Visible text</p>
                    <script>console.log('hidden');</script>
                    <style>body { color: red; }</style>
                </body>
            </html>
        """.trimIndent()

        val text = parser.parseHtmlText(html)
        text shouldBe "Visible text"
    }
}
```

**Acceptance Criteria**:
- ✅ Text extraction works
- ✅ RSS/Atom feed detection works
- ✅ Favicon extraction works
- ✅ Malformed HTML handled gracefully
- ✅ Script/style tags stripped

---

## Task 1.4: CSV and OPML Edge Cases

### CSV Tests

**File**: `shared/src/commonTest/kotlin/.../domain/csv/FeedItemImportExportTest.kt`

```kotlin
package com.prof18.feedflow.shared.domain.csv

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FeedItemImportExportTest {

    @Test
    fun `should handle empty CSV`() {
        val csv = ""
        val result = CsvParser.parseBookmarks(csv)
        result.shouldBeEmpty()
    }

    @Test
    fun `should skip items with unknown feed sources`() {
        val csv = """
            title,url,feedSourceId
            "Article 1","https://example.com/1","unknown-source"
        """.trimIndent()

        val availableSources = emptySet<String>()
        val result = CsvParser.parseBookmarks(csv, availableSources)
        result.shouldBeEmpty()
    }

    @Test
    fun `should handle CSV with special characters`() {
        val csv = """
            title,url,feedSourceId
            "Article with ""quotes""","https://example.com/1","source-1"
            "Article with, comma","https://example.com/2","source-1"
        """.trimIndent()

        val result = CsvParser.parseBookmarks(csv, setOf("source-1"))
        result.size shouldBe 2
        result[0].title shouldBe """Article with "quotes""""
        result[1].title shouldBe "Article with, comma"
    }

    @Test
    fun `should round-trip export and import`() {
        val originalItems = listOf(
            FeedItem(id = "1", title = "Article 1", url = "https://example.com/1"),
            FeedItem(id = "2", title = "Article 2", url = "https://example.com/2"),
        )

        val csv = CsvExporter.exportBookmarks(originalItems)
        val importedItems = CsvParser.parseBookmarks(csv)

        importedItems.size shouldBe originalItems.size
        importedItems.zip(originalItems).forEach { (imported, original) ->
            imported.title shouldBe original.title
            imported.url shouldBe original.url
        }
    }
}
```

**Acceptance Criteria**:
- ✅ Empty CSV handled
- ✅ Unknown sources skipped
- ✅ Special characters handled
- ✅ Round-trip works

---

### OPML Tests

**File**: `shared/src/commonTest/kotlin/.../domain/opml/OpmlParserTest.kt`

Add additional test cases to existing OPML tests:

```kotlin
@Test
fun `should handle deeply nested categories`() {
    val opml = """
        <?xml version="1.0"?>
        <opml version="2.0">
            <body>
                <outline text="Level 1">
                    <outline text="Level 2">
                        <outline text="Level 3">
                            <outline text="Feed" xmlUrl="https://example.com/feed.xml" />
                        </outline>
                    </outline>
                </outline>
            </body>
        </opml>
    """.trimIndent()

    val result = OpmlParser.parse(opml)
    // Should flatten or handle nested categories
    result.shouldNotBeEmpty()
}

@Test
fun `should handle duplicate feed URLs`() {
    val opml = """
        <?xml version="1.0"?>
        <opml version="2.0">
            <body>
                <outline text="Feed 1" xmlUrl="https://example.com/feed.xml" />
                <outline text="Feed 2" xmlUrl="https://example.com/feed.xml" />
            </body>
        </opml>
    """.trimIndent()

    val result = OpmlParser.parse(opml)
    // Should deduplicate or handle gracefully
}

@Test
fun `should handle BOM in different encodings`() {
    val bomUtf8 = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    val opmlBytes = bomUtf8 + """
        <?xml version="1.0"?>
        <opml version="2.0">
            <body>
                <outline text="Feed" xmlUrl="https://example.com/feed.xml" />
            </body>
        </opml>
    """.trimIndent().toByteArray()

    val result = OpmlParser.parse(opmlBytes.decodeToString())
    result.shouldNotBeEmpty()
}
```

**Acceptance Criteria**:
- ✅ Nested categories handled
- ✅ Duplicate URLs handled
- ✅ BOM handling works

---

## Summary

After completing Phase 1, you will have:

- ✅ All utility functions tested (UrlUtils, RetryHelpers, ArchiveIS)
- ✅ All mappers tested (RSS, GReader DTOs, Feedbin DTOs)
- ✅ HTML parser tested
- ✅ CSV/OPML edge cases covered

**Next Phase**: Phase 2 - Database Tests → See `03-database.md`
