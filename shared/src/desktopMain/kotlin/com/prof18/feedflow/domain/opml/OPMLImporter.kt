package com.prof18.feedflow.domain.opml

import com.prof18.feedflow.utils.DispatcherProvider
import kotlinx.coroutines.withContext

actual class OPMLImporter(
    private val dispatcherProvider: DispatcherProvider
) {

    actual suspend fun getOPML(opmlInput: OPMLInput): String = withContext(dispatcherProvider.io) {
        opmlInput.file.readText()
    }
}
