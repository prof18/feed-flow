package com.prof18.feedflow.desktop.startup

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenDataFolderTest {

    private val folder = File("/tmp/FeedFlow-dev")

    @Test
    fun `opening the folder does not copy anything`() {
        val copied = mutableListOf<String>()

        openDataFolder(folder, open = {}, copy = { copied += it })

        assertTrue(copied.isEmpty())
    }

    @Test
    fun `a failed open falls back to copying the path`() {
        val copied = mutableListOf<String>()

        openDataFolder(folder, open = { error("no file manager") }, copy = { copied += it })

        assertEquals(listOf(folder.absolutePath), copied)
    }

    @Test
    fun `unsupported open falls back to copying the path`() {
        val copied = mutableListOf<String>()

        openDataFolder(folder, open = { error("not called") }, copy = { copied += it }, canOpen = { false })

        assertEquals(listOf(folder.absolutePath), copied)
    }

    @Test
    fun `no exception escapes when both open and copy fail`() {
        openDataFolder(
            folder,
            open = { error("no file manager") },
            copy = { throw IllegalStateException("clipboard busy") },
        )
    }
}
