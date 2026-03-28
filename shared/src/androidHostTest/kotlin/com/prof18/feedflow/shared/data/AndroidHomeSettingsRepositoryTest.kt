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
}
