package com.prof18.feedflow.desktop.home

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopSinglePaneHomeScaffoldTest {

    @Test
    fun `negative width uses modal drawer`() {
        assertFalse(usesDockedDrawerForWidth((-1).dp))
    }

    @Test
    fun `compact width uses modal drawer`() {
        assertFalse(usesDockedDrawerForWidth(599.dp))
    }

    @Test
    fun `medium width uses docked drawer`() {
        assertTrue(usesDockedDrawerForWidth(600.dp))
    }
}
