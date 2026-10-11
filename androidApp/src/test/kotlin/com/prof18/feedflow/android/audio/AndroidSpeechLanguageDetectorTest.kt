package com.prof18.feedflow.android.audio

import android.app.Application
import android.icu.util.ULocale
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import android.view.textclassifier.TextLanguage
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class)
class AndroidSpeechLanguageDetectorTest {
    @Test
    @Config(sdk = [26])
    fun `API 26 skips platform language detection`() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        assertNull(AndroidSpeechLanguageDetector(context).detectLanguage("Ciao"))
    }

    @Test
    @Config(sdk = [29])
    fun `API 29 returns null when classifier has no hypotheses`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        setClassifierResult(context, TextLanguage.Builder().build())

        assertNull(AndroidSpeechLanguageDetector(context).detectLanguage("Ciao"))
    }

    @Test
    @Config(sdk = [29])
    fun `classifier language below confidence threshold is rejected`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        setClassifierResult(context, language(ULocale.ITALIAN, confidence = 0.79f))

        assertNull(AndroidSpeechLanguageDetector(context).detectLanguage("Ciao"))
    }

    @Test
    @Config(sdk = [29])
    fun `Italian at confidence threshold is accepted`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        setClassifierResult(context, language(ULocale.ITALIAN, confidence = 0.8f))

        assertEquals("it", AndroidSpeechLanguageDetector(context).detectLanguage("Ciao"))
    }

    private fun setClassifierResult(context: Application, result: TextLanguage) {
        val manager = checkNotNull(context.getSystemService(TextClassificationManager::class.java))
        manager.setTextClassifier(
            object : TextClassifier by TextClassifier.NO_OP {
                override fun detectLanguage(request: TextLanguage.Request): TextLanguage = result
            },
        )
    }

    private fun language(locale: ULocale, confidence: Float): TextLanguage =
        TextLanguage.Builder().putLocale(locale, confidence).build()
}
