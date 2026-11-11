package com.prof18.feedflow.desktop.ui

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopE2eProperties
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageCacheDirectoryTest {

    private val e2eDataDir = mapOf(DesktopE2eProperties.DATA_DIR to "/tmp/ffe2e/abc/data")

    @Test
    fun `an E2E run keeps its image cache inside its data folder`() {
        assertEquals(
            "/tmp/ffe2e/abc/data".toPath() / "image_cache",
            imageCacheDirectory(AppEnvironment.Debug) { e2eDataDir[it] },
        )
    }

    @Test
    fun `a normal debug run uses the shared temporary folder`() {
        assertEquals(
            FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache",
            imageCacheDirectory(AppEnvironment.Debug) { null },
        )
    }

    @Test
    fun `a release build ignores the E2E data folder`() {
        assertEquals(
            FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache",
            imageCacheDirectory(AppEnvironment.Release) { e2eDataDir[it] },
        )
    }
}
