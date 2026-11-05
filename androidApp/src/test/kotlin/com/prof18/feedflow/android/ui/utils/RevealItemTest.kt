package com.prof18.feedflow.android.ui.utils

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems

@RunWith(RobolectricTestRunner::class)
class RevealItemTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var scope: CoroutineScope
    private val rows = (0 until ROW_COUNT).toList()

    @Test
    fun `list scrolls to an off-screen item`() {
        val state = LazyListState()
        setList(state)

        reveal { state.revealItemConditionally(TARGET, reduceMotionEnabled = true) }

        compose.runOnIdle { assertEquals(TARGET, state.firstVisibleItemIndex) }
    }

    @Test
    fun `list stays put for a fully visible item`() {
        val state = LazyListState()
        setList(state)

        reveal { state.revealItemConditionally(1, reduceMotionEnabled = true) }

        compose.runOnIdle { assertEquals(0, state.firstVisibleItemIndex) }
    }

    @Test
    fun `a user scroll interrupting the animation still completes the reveal`() {
        val state = LazyListState()
        setList(state)

        lateinit var revealJob: Job
        compose.runOnIdle {
            revealJob = scope.launch { state.revealItemConditionally(TARGET, reduceMotionEnabled = false) }
            scope.launch { state.scroll(MutatePriority.UserInput) { scrollBy(USER_SCROLL) } }
        }
        compose.waitForIdle()

        compose.runOnIdle {
            assertTrue(revealJob.isCompleted)
            assertFalse(revealJob.isCancelled)
        }
    }

    @Test
    fun `staggered grid scrolls to an off-screen item`() {
        val state = LazyStaggeredGridState()
        setStaggeredGrid(state)

        reveal { state.revealItemConditionally(TARGET, reduceMotionEnabled = true) }

        compose.runOnIdle { assertEquals(TARGET, state.firstVisibleItemIndex) }
    }

    @Test
    fun `staggered grid stays put for a fully visible item`() {
        val state = LazyStaggeredGridState()
        setStaggeredGrid(state)

        reveal { state.revealItemConditionally(1, reduceMotionEnabled = true) }

        compose.runOnIdle { assertEquals(0, state.firstVisibleItemIndex) }
    }

    @Test
    fun `grid scrolls to an off-screen item`() {
        val state = LazyGridState()
        setGrid(state)

        reveal { state.revealItemConditionally(TARGET, reduceMotionEnabled = true) }

        compose.runOnIdle { assertEquals(TARGET, state.firstVisibleItemIndex) }
    }

    @Test
    fun `grid stays put for a fully visible item`() {
        val state = LazyGridState()
        setGrid(state)

        reveal { state.revealItemConditionally(1, reduceMotionEnabled = true) }

        compose.runOnIdle { assertEquals(0, state.firstVisibleItemIndex) }
    }

    private fun reveal(block: suspend () -> Unit) {
        compose.runOnIdle { scope.launch { block() } }
        compose.waitForIdle()
    }

    private fun setList(state: LazyListState) {
        compose.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.size(VIEWPORT.dp)) {
                LazyColumn(state = state) {
                    listItems(rows) { Row() }
                }
            }
        }
    }

    private fun setStaggeredGrid(state: LazyStaggeredGridState) {
        compose.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.size(VIEWPORT.dp)) {
                LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Fixed(1), state = state) {
                    staggeredItems(rows) { Row() }
                }
            }
        }
    }

    private fun setGrid(state: LazyGridState) {
        compose.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.size(VIEWPORT.dp)) {
                LazyVerticalGrid(columns = GridCells.Fixed(1), state = state) {
                    gridItems(rows) { Row() }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Row() {
        Box(Modifier.height(ROW_HEIGHT.dp).fillMaxWidth())
    }

    private companion object {
        const val ROW_COUNT = 50
        const val ROW_HEIGHT = 50
        const val VIEWPORT = 200
        const val TARGET = 30
        const val USER_SCROLL = 10f
    }
}
