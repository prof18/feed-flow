package com.prof18.feedflow.domain.opml

import android.content.Context
import com.prof18.feedflow.utils.DispatcherProvider
import kotlinx.coroutines.withContext
import java.io.BufferedReader

actual class OPMLImporter(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) {

    actual suspend fun getOPML(opmlInput: OPMLInput): String = withContext(dispatcherProvider.io) {
        context.contentResolver.openInputStream(opmlInput.uri).use { inputStream ->
            val reader = BufferedReader(inputStream?.reader())
            var content: String
            reader.use {
                content = it.readText()
            }
            return@use content
        }
    }
}