package com.prof18.feedflow.shared.domain.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import java.io.File
import kotlin.test.Test

/**
 * Manual debugging harness for the desktop reader pipeline (GraalJS + linkedom).
 * Not a regression test. Reads the article HTML from DEBUG_HTML_PATH and prints the
 * resulting markdown. Run with:
 * ./gradlew --quiet --console=plain :shared:jvmTest --tests "*.ReaderModeDebugHarness"
 */
class ReaderModeDebugHarness {

    private fun loadResource(name: String): String =
        ReaderModeDebugHarness::class.java
            .getResourceAsStream("/$name")
            ?.bufferedReader()
            ?.readText()
            ?: error("Could not load $name")

    private fun jsSource(name: String): Source =
        Source.newBuilder("js", loadResource(name), name).buildLiteral()

    @Test
    fun `debug reader parser on a real article`() {
        val htmlPath = System.getenv("DEBUG_HTML_PATH") ?: DEBUG_HTML_PATH
        val articleUrl = System.getenv("DEBUG_ARTICLE_URL") ?: DEFAULT_ARTICLE_URL
        val htmlFile = File(htmlPath)
        if (!htmlFile.exists()) {
            println("No fixture at $htmlPath, skipping")
            return
        }
        val articleHtml = htmlFile.readText()

        Context.newBuilder("js")
            .allowExperimentalOptions(true)
            .allowAllAccess(true)
            .option("engine.WarnInterpreterOnly", "false")
            .build()
            .use { context ->
                context.eval(jsSource("reader-mode-parser.js"))

                val resultJson = context.getBindings("js")
                    .getMember("parseReaderContent")
                    .execute(articleHtml, articleUrl, null)
                    .asString()

                val result = Json.parseToJsonElement(resultJson).jsonObject
                val error = result["error"]?.jsonPrimitive?.takeIf { it.isString }?.content
                if (error != null) {
                    println("===== ERROR =====")
                    println(error)
                    return
                }

                val content = result["content"]?.jsonPrimitive?.content.orEmpty()
                println("===== TITLE: ${result["title"]?.jsonPrimitive?.content} =====")
                println("===== CONTENT HEAD =====")
                println(content.take(1500))
                println("===== CONTENT TAIL =====")
                println(content.takeLast(1500))
                println("===== CONTENT LENGTH: ${content.length} =====")
            }
    }

    private companion object {
        private const val DEBUG_HTML_PATH = "/tmp/ilpost-article.html"
        private const val DEFAULT_ARTICLE_URL =
            "https://www.ilpost.it/2026/06/11/jose-mourinho-real-madrid-ritorno/"
    }
}
