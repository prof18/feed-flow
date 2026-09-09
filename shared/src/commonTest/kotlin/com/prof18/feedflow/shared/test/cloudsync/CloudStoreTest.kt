package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.core.model.CloudBackupNotFoundException
import com.prof18.feedflow.feedsync.dropbox.DropboxUploadConflictException
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CloudStoreTest : KoinTestBase() {

    @Test
    fun `upload and download deep copy bytes`() {
        val store = CloudStore()
        val uploaded = byteArrayOf(1, 2, 3)

        store.upload(CloudProvider.DROPBOX, "account", "file", uploaded, "device")
        uploaded[0] = 9

        val downloaded = store.download(CloudProvider.DROPBOX, "account", "file")
        downloaded[1] = 8

        assertContentEquals(byteArrayOf(1, 2, 3), store.download(CloudProvider.DROPBOX, "account", "file"))
        assertFailsWith<CloudBackupNotFoundException> {
            store.download(CloudProvider.DROPBOX, "account", "missing")
        }
    }

    @Test
    fun `provider and account data are isolated`() {
        val store = CloudStore()
        val bytes = byteArrayOf(7)

        store.upload(CloudProvider.DROPBOX, "one", "file", bytes, "device")
        store.upload(CloudProvider.DROPBOX, "two", "file", bytes, "device")
        store.upload(CloudProvider.GOOGLE_DRIVE, "one", "file", bytes, "device")

        assertContentEquals(byteArrayOf(7), store.download(CloudProvider.DROPBOX, "one", "file"))
        assertContentEquals(byteArrayOf(7), store.download(CloudProvider.DROPBOX, "two", "file"))
        assertContentEquals(byteArrayOf(7), store.download(CloudProvider.GOOGLE_DRIVE, "one", "file"))
        assertEquals(2, store.fileCount(CloudProvider.DROPBOX))
        assertEquals(1, store.fileCount(CloudProvider.DROPBOX, "one"))
    }

    @Test
    fun `drive update keeps stable opaque identity`() {
        val store = CloudStore()
        val firstId = store.upload(CloudProvider.GOOGLE_DRIVE, "account", "file", byteArrayOf(1), "device")
        val secondId = store.upload(CloudProvider.GOOGLE_DRIVE, "account", "file", byteArrayOf(2), "device")

        assertEquals(firstId, secondId)
        assertContentEquals(byteArrayOf(2), store.download(CloudProvider.GOOGLE_DRIVE, "account", "file"))
        assertEquals(1, store.fileCount(CloudProvider.GOOGLE_DRIVE, "account"))
    }

    @Test
    fun `icloud upload is staged until explicit propagation`() {
        val store = CloudStore()
        val bytes = byteArrayOf(4, 5)

        store.upload(CloudProvider.ICLOUD, "account", "file", bytes, "phone")
        assertEquals(0, store.fileCount(CloudProvider.ICLOUD))
        assertFailsWith<CloudBackupNotFoundException> {
            store.download(CloudProvider.ICLOUD, "account", "file")
        }

        store.propagate("phone")

        assertEquals(1, store.fileCount(CloudProvider.ICLOUD))
        assertContentEquals(bytes, store.download(CloudProvider.ICLOUD, "account", "file"))
        assertEquals(2, store.events.size)
        assertEquals(CloudStoreOperation.PROPAGATE, store.events.last().operation)
    }

    @Test
    fun `dropbox conditional upload is create only when revision is null`() {
        val store = CloudStore()
        val first = store.uploadDropbox("account", "file", byteArrayOf(1), "device1", null)

        assertFailsWith<DropboxUploadConflictException> {
            store.uploadDropbox("account", "file", byteArrayOf(2), "device2", null)
        }
        assertContentEquals(byteArrayOf(1), store.downloadDropbox("account", "file").bytes)
        assertEquals("1", first.revision)
    }

    @Test
    fun `dropbox conditional upload rejects stale revision atomically`() {
        val store = CloudStore()
        val first = store.uploadDropbox("account", "file", byteArrayOf(1), "device1", null)
        val second = store.uploadDropbox("account", "file", byteArrayOf(2), "device2", first.revision)

        assertFailsWith<DropboxUploadConflictException> {
            store.uploadDropbox("account", "file", byteArrayOf(3), "device1", first.revision)
        }
        assertContentEquals(byteArrayOf(2), store.downloadDropbox("account", "file").bytes)
        assertEquals("2", second.revision)
    }

    @Test
    fun `dropbox lost acknowledgement leaves accepted revision visible`() {
        val store = CloudStore()
        store.afterUploadFailure = IllegalStateException("lost response")

        assertFailsWith<IllegalStateException> {
            store.uploadDropbox("account", "file", byteArrayOf(1), "device1", null)
        }
        val accepted = store.downloadDropbox("account", "file")
        assertContentEquals(byteArrayOf(1), accepted.bytes)
        assertEquals("1", accepted.revision)
    }

    @Test
    fun `dropbox stale upload cannot recreate a remotely deleted file`() {
        val store = CloudStore()
        val first = store.uploadDropbox("account", "file", byteArrayOf(1), "device1", null)
        store.deleteDropbox("account", "file")

        assertFailsWith<DropboxUploadConflictException> {
            store.uploadDropbox("account", "file", byteArrayOf(2), "device1", first.revision)
        }
        assertFailsWith<CloudBackupNotFoundException> {
            store.downloadDropbox("account", "file")
        }
    }
}
