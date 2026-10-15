package com.prof18.feedflow.desktop.ui.components

import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import kotlin.test.Test
import kotlin.test.assertNotNull

class IconResourcesTest {

    @Test
    fun `sidebar toolbar and expansion icon variants are packaged`() {
        listOf(
            AllIconsKeys.Actions.Refresh,
            AllIconsKeys.Actions.Search,
            AllIconsKeys.General.ArrowDown,
            AllIconsKeys.General.ArrowRight,
            AllIconsKeys.General.HideToolWindow,
            AllIconsKeys.General.OpenInToolWindow,
        ).forEach(::assertIconVariantsAvailable)
    }

    private fun assertIconVariantsAvailable(key: IconKey) {
        listOf(false, true).forEach { isNewUi ->
            val path = key.path(isNewUi)
            val resource = key.iconClass.classLoader.getResource(path)
            assertNotNull(resource, "Missing Jewel icon resource $path")
        }
    }
}
