package com.prof18.feedflow.desktop.reaadermode

import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.shared.domain.ReaderModeExtractor
import com.prof18.feedflow.shared.domain.getReaderModeStyledHtml
import com.prof18.feedflow.shared.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReaderModeViewModel internal constructor(
    private val readerModeExtractor: ReaderModeExtractor,
    private val markdownToHtmlConverter: MarkdownToHtmlConverter,
) : BaseViewModel() {

    private val readerModeMutableState: MutableStateFlow<ReaderModeState> = MutableStateFlow(
        ReaderModeState.Loading,
    )
    val readerModeState = readerModeMutableState.asStateFlow()

    fun getReaderModeHtml(urlInfo: FeedItemUrlInfo) {
        scope.launch {
            readerModeMutableState.value = ReaderModeState.Loading
            val readerModeData = readerModeExtractor.extractReaderContent(urlInfo)
            if (readerModeData != null) {
                val tmpTitle = readerModeData.title
                val title = if (tmpTitle != null && readerModeData.content.contains(tmpTitle)) {
                    null
                } else {
                    tmpTitle
                }
                val html = getReaderModeStyledHtml(
                    colors = null,
                    content = readerModeData.content,
                    title = title,
                )
                val markdown = markdownToHtmlConverter.convertToMarkdown(html)
                readerModeMutableState.value = ReaderModeState.Success(
                    readerModeData.copy(
                        content = markdown,
                    ),
                )
            } else {
                readerModeMutableState.value = ReaderModeState.HtmlNotAvailable(urlInfo.url)
            }
        }
    }
}
