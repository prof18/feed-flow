package com.prof18.feedflow.shared.domain.parser

import com.prof18.feedflow.shared.test.testLogger
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.time.Duration
import kotlin.time.measureTimedValue

class ReaderModeJsBenchmarkTest {

    @Test
    fun benchmarkReaderModeJsRuntime() {
        if (System.getenv("FEEDFLOW_READER_BENCHMARK") != "true") {
            return
        }

        val runtime = ReaderModeJsRuntime(testLogger)
        try {
            val warmUpTime = measureTimedValue {
                runtime.warmUp()
            }.duration

            benchmarkCases.forEach { benchmarkCase ->
                repeat(WARM_UP_RUNS) {
                    assertNotNull(
                        runtime.parse(benchmarkCase.html, benchmarkCase.url, benchmarkCase.imageUrl),
                        "${benchmarkCase.name} warm-up parse failed",
                    )
                }

                val timings = buildList {
                    repeat(MEASURED_RUNS) {
                        val timedValue = measureTimedValue {
                            runtime.parse(benchmarkCase.html, benchmarkCase.url, benchmarkCase.imageUrl)
                        }
                        assertNotNull(timedValue.value, "${benchmarkCase.name} parse failed")
                        add(timedValue.duration)
                    }
                }

                println(
                    buildString {
                        append("Reader benchmark | ")
                        append(benchmarkCase.name)
                        append(" | warm-up=")
                        append(warmUpTime.inWholeMilliseconds)
                        append("ms")
                        append(" | min=")
                        append(timings.minOf { it.inWholeMilliseconds })
                        append("ms")
                        append(" | p50=")
                        append(timings.percentile(0.50).inWholeMilliseconds)
                        append("ms")
                        append(" | p95=")
                        append(timings.percentile(0.95).inWholeMilliseconds)
                        append("ms")
                        append(" | max=")
                        append(timings.maxOf { it.inWholeMilliseconds })
                        append("ms")
                    },
                )
            }
        } finally {
            runtime.close()
        }
    }

    private fun List<Duration>.percentile(percentile: Double): Duration {
        val sorted = sorted()
        val index = ((sorted.size - 1) * percentile).roundToLong().toInt()
        return sorted[index.coerceIn(sorted.indices)]
    }

    private data class BenchmarkCase(
        val name: String,
        val url: String,
        val imageUrl: String?,
        val html: String,
    )

    private companion object {
        private const val WARM_UP_RUNS = 1
        private const val MEASURED_RUNS = 7

        private val benchmarkCases = listOf(
            BenchmarkCase(
                name = "plain-article",
                url = "https://example.com/plain-article",
                imageUrl = null,
                html = articleHtml(
                    title = "Plain Article",
                    paragraphs = 18,
                    includeMedia = false,
                    includeNoise = false,
                ),
            ),
            BenchmarkCase(
                name = "media-article",
                url = "https://example.com/media-article",
                imageUrl = "https://example.com/images/hero.jpg",
                html = articleHtml(
                    title = "Media Article",
                    paragraphs = 24,
                    includeMedia = true,
                    includeNoise = false,
                ),
            ),
            BenchmarkCase(
                name = "noisy-article",
                url = "https://example.com/noisy-article",
                imageUrl = "https://example.com/images/hero.jpg",
                html = articleHtml(
                    title = "Noisy Article",
                    paragraphs = 30,
                    includeMedia = true,
                    includeNoise = true,
                ),
            ),
        )

        private fun articleHtml(
            title: String,
            paragraphs: Int,
            includeMedia: Boolean,
            includeNoise: Boolean,
        ): String = buildString {
            append("<html><head><title>$title</title>")
            append("<meta property=\"og:site_name\" content=\"Benchmark Site\">")
            if (includeNoise) {
                repeat(30) { index ->
                    append("<script>window.__noise$index='")
                    append("x".repeat(2_000))
                    append("';</script>")
                    append("<style>.noise$index{display:none}</style>")
                }
            }
            append("</head><body>")
            append("<header><nav><a href=\"/\">Home</a><a href=\"/latest\">Latest</a></nav></header>")
            append("<main><article><h1>$title</h1>")
            repeat(paragraphs) { index ->
                append("<p>This is paragraph number $index of a benchmark article. ")
                append("It has enough meaningful prose for the extraction algorithm to keep it, ")
                append("including a <a href=\"/related-$index\">related reference</a> and ")
                append("<strong>important text</strong> that should survive Markdown conversion.</p>")
                if (includeMedia && index == paragraphs / 2) {
                    append("<figure>")
                    append("<img src=\"/images/mid-article.jpg\" alt=\"A benchmark image\">")
                    append("<figcaption>A concise benchmark caption</figcaption>")
                    append("</figure>")
                }
            }
            append("</article></main>")
            append("<aside>Recommended links and subscription prompts that should not be selected.</aside>")
            append("<footer>Copyright footer junk</footer>")
            append("</body></html>")
        }
    }
}
