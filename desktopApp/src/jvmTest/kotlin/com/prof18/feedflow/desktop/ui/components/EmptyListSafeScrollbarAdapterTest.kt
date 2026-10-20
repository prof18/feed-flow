package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFailsWith

class EmptyListSafeScrollbarAdapterTest {
    @get:Rule
    val rule = createComposeRule()

    private val listState = LazyListState()
    private lateinit var adapter: ScrollbarAdapter

    private fun setEmptyList() {
        rule.setContent {
            adapter = rememberScrollbarAdapter(listState)
            LazyColumn(state = listState, modifier = Modifier.size(width = 120.dp, height = 200.dp)) {}
        }
        rule.waitForIdle()
    }

    @Test
    fun `Compose adapter still crashes when snapping on an empty list`() {
        setEmptyList()
        rule.runOnUiThread {
            assertFailsWith<IllegalArgumentException> { runBlocking { adapter.scrollTo(LONG_DRAG_OFFSET) } }
        }
    }

    @Test
    fun `guarded adapter ignores snapping on an empty list`() {
        setEmptyList()
        rule.runOnUiThread {
            runBlocking { EmptyListSafeScrollbarAdapter(adapter, listState).scrollTo(LONG_DRAG_OFFSET) }
        }
    }

    private companion object {
        const val LONG_DRAG_OFFSET = 10_000.0
    }
}
