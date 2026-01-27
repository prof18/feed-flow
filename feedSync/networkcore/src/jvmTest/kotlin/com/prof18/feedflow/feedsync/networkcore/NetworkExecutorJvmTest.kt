package com.prof18.feedflow.feedsync.networkcore

import com.prof18.feedflow.core.model.DataResult
import com.prof18.feedflow.core.model.NetworkFailure
import kotlinx.coroutines.test.runTest
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkExecutorJvmTest {

    @Test
    fun `executeNetwork maps missing connection errors to NoConnection`() = runTest {
        val result = executeNetwork<String> { throw UnknownHostException("no connection") }

        assertEquals(DataResult.Error(NetworkFailure.NoConnection), result)
    }
}
