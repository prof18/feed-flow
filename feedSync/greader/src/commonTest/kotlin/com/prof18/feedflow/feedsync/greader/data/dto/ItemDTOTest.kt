package com.prof18.feedflow.feedsync.greader.data.dto

import kotlin.test.Test
import kotlin.test.assertEquals

class ItemDTOTest {

    @Test
    fun `getHexID matches the legacy conversion for positive decimal ids`() {
        val ids = listOf("1", "2", "1755276083123456", "9223372036854775807")

        for (id in ids) {
            val legacyHexID = id.toLong().toString(16).padStart(16, '0')
            assertEquals(legacyHexID, ItemDTO(id = id).getHexID())
        }
    }

    @Test
    fun `getHexID pads short values to 16 characters`() {
        assertEquals("0000000000000001", ItemDTO(id = "1").getHexID())
    }

    @Test
    fun `getHexID converts negative signed ids to the unsigned hex form`() {
        // Example from the Google Reader API documentation:
        // short form -355401917359550817 <-> long form hex fb115bd6d34a8e9f
        assertEquals("fb115bd6d34a8e9f", ItemDTO(id = "-355401917359550817").getHexID())
    }

    @Test
    fun `getHexID handles unsigned decimal ids above Long MAX_VALUE`() {
        // Same item id as the signed example, expressed as an unsigned 64-bit decimal
        assertEquals("fb115bd6d34a8e9f", ItemDTO(id = "18091342156350000799").getHexID())
    }

    @Test
    fun `getHexID converts long form ids with a decimal last segment`() {
        assertEquals(
            "0000000000000001",
            ItemDTO(id = "tag:google.com,2005:reader/item/1").getHexID(),
        )
    }

    @Test
    fun `getHexID passes through long form ids with a hex last segment`() {
        assertEquals(
            "fb115bd6d34a8e9f",
            ItemDTO(id = "tag:google.com,2005:reader/item/fb115bd6d34a8e9f").getHexID(),
        )
    }

    @Test
    fun `getHexID returns non numeric ids unchanged instead of throwing`() {
        assertEquals("not-a-number", ItemDTO(id = "not-a-number").getHexID())
    }
}
