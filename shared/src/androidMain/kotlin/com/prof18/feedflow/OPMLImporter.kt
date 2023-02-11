package com.prof18.feedflow

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.withContext
import java.io.BufferedReader

class OPMLImporter(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) {

    suspend fun getOPML(uri: Uri): String = withContext(dispatcherProvider.io) {

        val inputStream = context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(inputStream?.reader())
        var content: String
        reader.use {
            content = it.readText()
        }
        return@withContext content
    }
}