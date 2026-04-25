package com.prof18.feedflow.shared.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidHomeSettingsRepositoryTest {

    private val repository = AndroidHomeSettingsRepository(MapSettings())

    @Test
    fun `pane expansion index is persisted`() {
        repository.setPaneExpansionIndex(7)

        assertEquals(7, repository.getPaneExpansionIndex())
    }

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
