package com.prof18.feedflow.desktop.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class RefreshScrollEffectTest {
    @get:Rule
    val rule = createComposeRule()

    private var refreshTrigger by mutableIntStateOf(0)
    private var isHomeShown by mutableStateOf(true)
    private var scrollCount = 0

    @Test
    fun `a new refresh scrolls once`() {
        setHome()

        refreshTrigger = 1
        rule.waitForIdle()

        rule.runOnIdle { assertEquals(1, scrollCount) }
    }

    @Test
    fun `coming back to home does not replay an earlier refresh`() {
        setHome()
        refreshTrigger = 1
        rule.waitForIdle()

        isHomeShown = false
        rule.waitForIdle()
        isHomeShown = true
        rule.waitForIdle()

        rule.runOnIdle { assertEquals(1, scrollCount) }
    }

    @Test
    fun `a refresh while home is away scrolls when it comes back`() {
        setHome()

        isHomeShown = false
        rule.waitForIdle()
        refreshTrigger = 1
        isHomeShown = true
        rule.waitForIdle()

        rule.runOnIdle { assertEquals(1, scrollCount) }
    }

    private fun setHome() {
        rule.setContent {
            val holder = rememberSaveableStateHolder()
            if (isHomeShown) {
                holder.SaveableStateProvider("home") {
                    RefreshScrollEffect(refreshTrigger = refreshTrigger) { scrollCount++ }
                }
            }
        }
        rule.waitForIdle()
    }
}
