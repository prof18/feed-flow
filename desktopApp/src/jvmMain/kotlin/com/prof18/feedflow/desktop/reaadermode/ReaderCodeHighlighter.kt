package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import org.jetbrains.jewel.foundation.code.MimeType
import org.jetbrains.jewel.foundation.code.highlighting.CodeHighlighter
import org.jetbrains.jewel.intui.standalone.code.highlighting.SimpleCodeHighlighter
import org.jetbrains.jewel.intui.standalone.code.highlighting.SyntaxHighlightColors

/**
 * Jewel's [SimpleCodeHighlighter] with a plain-text fallback. Its grammars match string literals and
 * comments with patterns like `"(?:[^"\\]|\\.)*"`, which java.util.regex evaluates with one stack frame
 * group per character, so a code block holding a long literal (minified JSON, inlined data) throws
 * StackOverflowError. Jewel rethrows it in the composition and the whole app goes down.
 */
internal class ReaderCodeHighlighter(dark: Boolean) : CodeHighlighter {
    private val delegate: CodeHighlighter =
        SimpleCodeHighlighter(if (dark) SyntaxHighlightColors.dark() else SyntaxHighlightColors.light())

    override fun highlight(code: String, mimeType: MimeType?): Flow<AnnotatedString> =
        delegate.highlight(code, mimeType).plainOnStackOverflow(code)

    override fun highlight(code: String, language: String): Flow<AnnotatedString> =
        delegate.highlight(code, language).plainOnStackOverflow(code)

    private fun Flow<AnnotatedString>.plainOnStackOverflow(code: String): Flow<AnnotatedString> =
        catch { error -> emitPlainOnStackOverflow(error, code) }

    private suspend fun FlowCollector<AnnotatedString>.emitPlainOnStackOverflow(error: Throwable, code: String) {
        if (error !is StackOverflowError) throw error
        emit(AnnotatedString(code))
    }
}
