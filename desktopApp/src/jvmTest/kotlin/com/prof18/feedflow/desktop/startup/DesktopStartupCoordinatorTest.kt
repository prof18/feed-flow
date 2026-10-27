package com.prof18.feedflow.desktop.startup

import app.cash.turbine.test
import com.prof18.feedflow.database.DesktopDatabaseFailure
import com.prof18.feedflow.database.DesktopDatabaseFailureKind
import com.prof18.feedflow.database.DesktopDatabaseStartupException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.koin.core.error.InstanceCreationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DesktopStartupCoordinatorTest {

    private var afterReadyCalls = 0
    private val reported = mutableListOf<DesktopDatabaseStartupException>()

    private fun coordinator(initDependencies: suspend () -> Unit) = DesktopStartupCoordinator(
        initDependencies = initDependencies,
        afterDatabaseReady = { afterReadyCalls++ },
        reportStartupFailure = { reported += it },
    )

    @Test
    fun `a successful start becomes Ready`() = runTest {
        val coordinator = coordinator(initDependencies = {})

        coordinator.state.test {
            assertEquals(StartupState.Loading, awaitItem())
            coordinator.start()
            assertEquals(StartupState.Ready, awaitItem())
        }
        assertEquals(1, afterReadyCalls)
        assertEquals(0, reported.size)
    }

    @Test
    fun `a database startup failure wrapped by Koin becomes DatabaseFailed`() = runTest {
        val failure = DesktopDatabaseFailure(DesktopDatabaseFailureKind.Locked, "SQLITE_BUSY")
        val startupException = DesktopDatabaseStartupException(failure, IllegalStateException("busy"))
        val coordinator = coordinator(
            initDependencies = { throw InstanceCreationException("Could not create SqlDriver", startupException) },
        )

        coordinator.start()

        assertEquals(StartupState.DatabaseFailed(failure), coordinator.state.value)
        assertEquals(0, afterReadyCalls)
        assertEquals(listOf(startupException), reported)
    }

    @Test
    fun `other failures propagate`() = runTest {
        val coordinator = coordinator(initDependencies = { throw IllegalStateException("boom") })

        assertFailsWith<IllegalStateException> { coordinator.start() }
        assertEquals(0, afterReadyCalls)
    }

    @Test
    fun `cancellation propagates`() = runTest {
        val coordinator = coordinator(initDependencies = { throw CancellationException("stopped") })

        assertFailsWith<CancellationException> { coordinator.start() }
        assertEquals(StartupState.Loading, coordinator.state.value)
    }
}
