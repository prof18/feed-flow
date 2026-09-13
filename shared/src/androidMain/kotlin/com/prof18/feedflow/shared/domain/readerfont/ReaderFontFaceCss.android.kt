package com.prof18.feedflow.shared.domain.readerfont

import android.content.Context
import org.koin.core.context.GlobalContext

actual fun readerFontFaceSrc(fileName: String): String? {
    val context = ReaderFontAndroidContext.appContext
        ?: runCatching { GlobalContext.get().get<Context>() }.getOrNull()
        ?: return null
    return runCatching {
        context.assets.open("reader-fonts/$fileName").use { stream ->
            stream.readBytes().toFontDataUri()
        }
    }.getOrNull()
}

/**
 * Optional Application context for reader font assets when Koin is not yet available.
 */
object ReaderFontAndroidContext {
    @Volatile
    var appContext: Context? = null
}
