package com.prof18.feedflow.desktop

import com.prof18.feedflow.core.utils.DesktopOS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WindowsRenderApiWorkaroundTest {

    @Test
    fun `uses OpenGL on Windows x64 when no renderer override exists`() {
        val renderApi = windowsDefaultRenderApi(
            desktopOS = DesktopOS.WINDOWS,
            osArch = "amd64",
            skikoRenderApiEnv = null,
            skikoRenderApiProperty = null,
            windowsOpenGLRendererEnabled = true,
        )

        assertEquals(WINDOWS_DEFAULT_RENDER_API, renderApi)
    }

    @Test
    fun `does not override SKIKO_RENDER_API environment setting`() {
        val renderApi = windowsDefaultRenderApi(
            desktopOS = DesktopOS.WINDOWS,
            osArch = "amd64",
            skikoRenderApiEnv = "SOFTWARE_COMPAT",
            skikoRenderApiProperty = null,
            windowsOpenGLRendererEnabled = true,
        )

        assertNull(renderApi)
    }

    @Test
    fun `does not override skiko renderApi system property`() {
        val renderApi = windowsDefaultRenderApi(
            desktopOS = DesktopOS.WINDOWS,
            osArch = "amd64",
            skikoRenderApiEnv = null,
            skikoRenderApiProperty = "DIRECT3D",
            windowsOpenGLRendererEnabled = true,
        )

        assertNull(renderApi)
    }

    @Test
    fun `does not use OpenGL when persisted setting is disabled`() {
        val renderApi = windowsDefaultRenderApi(
            desktopOS = DesktopOS.WINDOWS,
            osArch = "amd64",
            skikoRenderApiEnv = null,
            skikoRenderApiProperty = null,
            windowsOpenGLRendererEnabled = false,
        )

        assertNull(renderApi)
    }

    @Test
    fun `does not use OpenGL on Windows arm64`() {
        val renderApi = windowsDefaultRenderApi(
            desktopOS = DesktopOS.WINDOWS,
            osArch = "aarch64",
            skikoRenderApiEnv = null,
            skikoRenderApiProperty = null,
            windowsOpenGLRendererEnabled = true,
        )

        assertNull(renderApi)
    }

    @Test
    fun `does not use OpenGL on non-Windows platforms`() {
        val macRenderApi = windowsDefaultRenderApi(
            desktopOS = DesktopOS.MAC,
            osArch = "aarch64",
            skikoRenderApiEnv = null,
            skikoRenderApiProperty = null,
            windowsOpenGLRendererEnabled = true,
        )
        val linuxRenderApi = windowsDefaultRenderApi(
            desktopOS = DesktopOS.LINUX,
            osArch = "amd64",
            skikoRenderApiEnv = null,
            skikoRenderApiProperty = null,
            windowsOpenGLRendererEnabled = true,
        )

        assertNull(macRenderApi)
        assertNull(linuxRenderApi)
    }

    @Test
    fun `sets Skiko render API only when workaround applies`() {
        val properties = mutableMapOf<String, String>()

        configureWindowsRenderApiWorkaround(
            desktopOS = DesktopOS.WINDOWS,
            osArch = "x86_64",
            skikoRenderApiEnv = null,
            skikoRenderApiProperty = null,
            windowsOpenGLRendererEnabled = true,
            setProperty = { key, value -> properties[key] = value },
        )

        assertEquals(WINDOWS_DEFAULT_RENDER_API, properties[SKIKO_RENDER_API_PROPERTY])
    }
}
