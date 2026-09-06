package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.utils.AppEnvironment
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsRepositoryKleadParserTest {

    @Test
    fun `Klead parser setting is read from storage only once`() {
        val settings = CountingSettings().apply {
            putBoolean(KLEAD_PARSER_KEY, true)
        }
        val repository = SettingsRepository(settings, AppEnvironment.Release)

        assertTrue(repository.isKleadParserEnabled())
        assertTrue(repository.isKleadParserEnabled())

        assertEquals(1, settings.booleanReadCount)
    }

    @Test
    fun `setting Klead parser updates the cached value`() {
        val settings = CountingSettings()
        val repository = SettingsRepository(settings, AppEnvironment.Release)

        repository.setKleadParserEnabled(true)
        assertTrue(repository.isKleadParserEnabled())
        repository.setKleadParserEnabled(false)
        assertFalse(repository.isKleadParserEnabled())

        assertEquals(0, settings.booleanReadCount)
    }

    @Test
    fun `Klead parser defaults to enabled in debug environment`() {
        val repository = SettingsRepository(MapSettings(), AppEnvironment.Debug)

        assertTrue(repository.isKleadParserEnabled())
    }

    @Test
    fun `Klead parser defaults to disabled in release environment`() {
        val repository = SettingsRepository(MapSettings(), AppEnvironment.Release)

        assertFalse(repository.isKleadParserEnabled())
    }

    @Test
    fun `stored Klead parser setting overrides debug default`() {
        val settings = MapSettings().apply {
            putBoolean(KLEAD_PARSER_KEY, false)
        }
        val repository = SettingsRepository(settings, AppEnvironment.Debug)

        assertFalse(repository.isKleadParserEnabled())
    }

    private class CountingSettings(
        private val delegate: Settings = MapSettings(),
    ) : Settings by delegate {
        var booleanReadCount = 0
            private set

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
            if (key == KLEAD_PARSER_KEY) {
                booleanReadCount++
            }
            return delegate.getBoolean(key, defaultValue)
        }
    }

    private companion object {
        const val KLEAD_PARSER_KEY = "USE_KLEAD_READER_PARSER"
    }
}
