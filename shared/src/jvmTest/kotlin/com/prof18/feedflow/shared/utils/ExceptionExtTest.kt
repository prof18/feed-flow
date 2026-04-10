package com.prof18.feedflow.shared.utils

import com.dropbox.core.NetworkIOException
import com.prof18.feedflow.feedsync.dropbox.DropboxDownloadException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExceptionExtTest {

    @Test
    fun `isTemporaryNetworkError returns true for wrapped Dropbox network failures`() {
        val exception = DropboxDownloadException(
            exceptionCause = NetworkIOException(IOException("timeout")),
        )

        assertTrue(exception.isTemporaryNetworkError())
    }

    @Test
    fun `isTemporaryNetworkError returns true for nested socket timeout failures`() {
        val exception = IllegalStateException(
            "wrapper",
            RuntimeException(SocketTimeoutException("timeout")),
        )

        assertTrue(exception.isTemporaryNetworkError())
    }

    @Test
    fun `isTemporaryNetworkError returns false for non network failures`() {
        assertFalse(IllegalStateException("boom").isTemporaryNetworkError())
    }
}
