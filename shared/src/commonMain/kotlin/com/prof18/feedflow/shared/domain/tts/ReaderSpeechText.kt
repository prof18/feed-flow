package com.prof18.feedflow.shared.domain.tts

import com.prof18.feedflow.core.utils.DispatcherProvider
import kotlinx.coroutines.withContext

class ReaderSpeechText(
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend fun segments(title: String?, content: String): List<String> = withContext(dispatcherProvider.default) {
        TtsTextExtractor.extract(title, content)
    }
}
