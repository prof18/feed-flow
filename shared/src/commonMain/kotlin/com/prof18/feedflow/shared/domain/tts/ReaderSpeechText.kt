package com.prof18.feedflow.shared.domain.tts

import co.touchlab.skie.configuration.annotations.SuspendInterop
import com.prof18.feedflow.core.utils.DispatcherProvider
import kotlinx.coroutines.withContext

class ReaderSpeechText(
    private val dispatcherProvider: DispatcherProvider,
) {
    @SuspendInterop.Enabled
    @Throws(Exception::class)
    suspend fun segments(title: String?, content: String): List<String> = withContext(dispatcherProvider.default) {
        TtsTextExtractor.extract(title, content)
    }
}
