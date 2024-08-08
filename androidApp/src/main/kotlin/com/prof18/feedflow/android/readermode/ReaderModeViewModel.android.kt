package com.prof18.feedflow.android.readermode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.shared.domain.ReaderModeExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReaderModeViewModel internal constructor(
    private val readerModeExtractor: ReaderModeExtractor,
) : ViewModel() {

    private val readerModeMutableState: MutableStateFlow<ReaderModeState> = MutableStateFlow(
        ReaderModeState.Loading,
    )
    val readerModeState = readerModeMutableState.asStateFlow()

    fun getReaderModeHtml(urlInfo: FeedItemUrlInfo) {
        viewModelScope.launch {
            readerModeMutableState.value = ReaderModeState.Loading
            val readerModeData = readerModeExtractor.extractReaderContent(urlInfo)
            if (readerModeData != null) {
                readerModeMutableState.value = ReaderModeState.Success(readerModeData)
            } else {
                readerModeMutableState.value = ReaderModeState.HtmlNotAvailable(urlInfo.url)
            }
        }
    }
}
