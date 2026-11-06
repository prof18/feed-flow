package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import org.jetbrains.jewel.foundation.code.MimeType
import org.jetbrains.jewel.foundation.code.highlighting.CodeHighlighter
import org.jetbrains.jewel.intui.standalone.code.highlighting.SimpleCodeHighlighter
import org.jetbrains.jewel.intui.standalone.code.highlighting.SyntaxHighlightColors

internal fun readerCodeHighlighter(dark: Boolean): CodeHighlighter = PlainTextOnOverflowCodeHighlighter(
    SimpleCodeHighlighter(if (dark) SyntaxHighlightColors.dark() else SyntaxHighlightColors.light()),
)

// Jewel's grammars match string literals and comments with backtracking regexes that recurse once per
// character, so one long token in an article's code block overflows the stack and, rethrown on the UI
// thread, kills the app. That block is shown as plain text instead.
private class PlainTextOnOverflowCodeHighlighter(
    private val delegate: CodeHighlighter,
) : CodeHighlighter {
    override fun highlight(code: String, mimeType: MimeType?): Flow<AnnotatedString> =
        delegate.highlight(code, mimeType).plainTextOnOverflow(code)

    override fun highlight(code: String, language: String): Flow<AnnotatedString> =
        delegate.highlight(code, language).plainTextOnOverflow(code)
}

private fun Flow<AnnotatedString>.plainTextOnOverflow(code: String): Flow<AnnotatedString> = catch { error ->
    if (error !is StackOverflowError) throw error
    emit(AnnotatedString(code))
}
