package com.prof18.feedflow.android.audio

import android.content.Context
import android.os.Build
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextLanguage
import androidx.annotation.RequiresApi

internal fun interface SpeechLanguageDetector {
    fun detectLanguage(text: String): String?
}

internal class AndroidSpeechLanguageDetector(context: Context) : SpeechLanguageDetector {
    private val appContext = context.applicationContext

    override fun detectLanguage(text: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return detectLanguageApi29(text)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun detectLanguageApi29(text: String): String? {
        val manager = appContext.getSystemService(TextClassificationManager::class.java) ?: return null
        val language = manager.textClassifier.detectLanguage(
            TextLanguage.Request.Builder(text).build(),
        )
        if (language.localeHypothesisCount == 0) return null
        val locale = language.getLocale(0)
        return locale.toLanguageTag().takeIf { language.getConfidenceScore(locale) >= LANGUAGE_CONFIDENCE_THRESHOLD }
    }

    private companion object {
        const val LANGUAGE_CONFIDENCE_THRESHOLD = 0.8f
    }
}
