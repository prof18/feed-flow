package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.shared.test.KoinTestBase
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CloudUploadAcknowledgmentTest : KoinTestBase() {
    @Test
    fun `later edits remain pending across repository recreation`() {
        val settings = MapSettings()
        val repository = SettingsRepository(settings, AppEnvironment.Debug)
        repository.setIsSyncUploadRequired(true)
        val firstUpload = repository.captureSyncUploadGeneration()
        repository.setIsSyncUploadRequired(true)
        val laterEdit = repository.captureSyncUploadGeneration()
        assertNotEquals(firstUpload, laterEdit)

        val restarted = SettingsRepository(settings, AppEnvironment.Debug)
        assertFalse(restarted.acknowledgeSyncUpload(firstUpload))
        assertTrue(restarted.getIsSyncUploadRequired())
        assertTrue(restarted.isSyncUploadRequired.value)
        assertTrue(restarted.acknowledgeSyncUpload(laterEdit))
        assertFalse(restarted.getIsSyncUploadRequired())
        assertFalse(SettingsRepository(settings, AppEnvironment.Debug).getIsSyncUploadRequired())
    }

    @Test
    fun `legacy pending flag can be acknowledged without a generation`() {
        val settings = MapSettings()
        settings["IS_SYNC_UPLOAD_REQUIRED"] = true
        val repository = SettingsRepository(settings, AppEnvironment.Debug)
        assertTrue(repository.getIsSyncUploadRequired())
        assertEquals(null, repository.captureSyncUploadGeneration())
        assertTrue(repository.acknowledgeSyncUpload(null))
        assertFalse(repository.getIsSyncUploadRequired())
    }

    @Test
    fun `interrupted acknowledgment leaves a durable pending generation`() {
        val settings = InterruptedAcknowledgmentSettings(MapSettings())
        val repository = SettingsRepository(settings, AppEnvironment.Debug)
        repository.setIsSyncUploadRequired(true)
        val captured = repository.captureSyncUploadGeneration()
        settings.interruptRemoval = true
        assertFailsWith<IllegalStateException> { repository.acknowledgeSyncUpload(captured) }

        val restarted = SettingsRepository(settings, AppEnvironment.Debug)
        assertTrue(restarted.getIsSyncUploadRequired())
        assertTrue(restarted.isSyncUploadRequired.value)
        settings.interruptRemoval = false
        assertTrue(restarted.acknowledgeSyncUpload(captured))
        assertFalse(restarted.getIsSyncUploadRequired())
    }
}

private class InterruptedAcknowledgmentSettings(private val delegate: Settings) : Settings by delegate {
    var interruptRemoval = false

    override fun remove(key: String) {
        check(!interruptRemoval) { "Injected interruption before generation removal" }
        delegate.remove(key)
    }
}
