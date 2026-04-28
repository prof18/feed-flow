package com.prof18.feedflow.shared.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class IosHomeSettingsRepositoryTest {

    private val repository = IosHomeSettingsRepository(MapSettings())

    @Test
    fun `multi pane layout is disabled by default`() {
        assertEquals(false, repository.isMultiPaneLayoutEnabled())
    }

    @Test
    fun `multi pane layout setting is persisted`() {
        repository.setMultiPaneLayoutEnabled(true)

        assertEquals(true, repository.isMultiPaneLayoutEnabled())
    }
}
