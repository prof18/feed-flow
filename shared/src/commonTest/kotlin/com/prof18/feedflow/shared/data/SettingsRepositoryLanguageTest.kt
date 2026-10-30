package com.prof18.feedflow.shared.data

import app.cash.turbine.test
import com.prof18.feedflow.shared.test.KoinTestBase
import com.russhwolf.settings.Settings
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsRepositoryLanguageTest : KoinTestBase() {
    private val repository: SettingsRepository by inject()
    private val settings: Settings by inject()

    @Test
    fun `force English preference is disabled by default`() {
        assertFalse(repository.getForceEnglishEnabled())
        assertFalse(repository.forceEnglishEnabledFlow.value)
    }

    @Test
    fun `force English preference emits changes and updates the stored value`() = runTest {
        repository.forceEnglishEnabledFlow.test {
            assertFalse(awaitItem())

            repository.setForceEnglishEnabled(true)
            assertTrue(awaitItem())
            assertTrue(repository.getForceEnglishEnabled())

            repository.setForceEnglishEnabled(false)
            assertFalse(awaitItem())
            assertFalse(repository.getForceEnglishEnabled())
        }
    }

    @Test
    fun `force English preference survives repository recreation`() {
        repository.setForceEnglishEnabled(true)
        val recreatedRepository = SettingsRepository(settings)
        assertTrue(recreatedRepository.forceEnglishEnabledFlow.value)
        assertTrue(recreatedRepository.getForceEnglishEnabled())

        recreatedRepository.setForceEnglishEnabled(false)
        val restoredRepository = SettingsRepository(settings)
        assertFalse(restoredRepository.forceEnglishEnabledFlow.value)
        assertFalse(restoredRepository.getForceEnglishEnabled())
    }
}
