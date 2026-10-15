package com.prof18.feedflow.desktop.home

import com.prof18.feedflow.core.model.FeedOperation
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.core.model.InProgressFeedUpdateStatus
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.core.model.StartedFeedUpdateStatus
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopWorkspaceCommandsTest {
    @Test
    fun `loading operations and absent subscriptions disable refresh and bulk commands`() {
        val unavailableStates = listOf(
            NoFeedSourcesStatus to FeedOperation.None,
            StartedFeedUpdateStatus to FeedOperation.None,
            InProgressFeedUpdateStatus(1, 4) to FeedOperation.None,
            FinishedFeedUpdateStatus to FeedOperation.MarkingAllRead,
            FinishedFeedUpdateStatus to FeedOperation.Deleting,
        )
        for ((status, operation) in unavailableStates) {
            val availability = desktopWorkspaceCommandAvailability(status, operation)
            assertFalse(availability.refreshEnabled, "$status / $operation refresh")
            assertFalse(availability.bulkActionsEnabled, "$status / $operation bulk")
        }
        val idle = desktopWorkspaceCommandAvailability(FinishedFeedUpdateStatus, FeedOperation.None)
        assertTrue(idle.refreshEnabled)
        assertTrue(idle.bulkActionsEnabled)
    }

    @Test
    fun `remembered menu and toolbar actions read current availability and dispatch once`() {
        var state = DesktopWorkspaceCommandAvailability()
        var refreshes = 0
        var bulkActions = 0
        val menuRefresh = desktopWorkspaceAction(DesktopWorkspaceCommand.Refresh, { state }) { refreshes++ }
        val toolbarRefresh = desktopWorkspaceAction(DesktopWorkspaceCommand.Refresh, { state }) { refreshes++ }
        val markAll = desktopWorkspaceAction(DesktopWorkspaceCommand.BulkAction, { state }) { bulkActions++ }
        menuRefresh()
        toolbarRefresh()
        markAll()
        assertEquals(2, refreshes)
        assertEquals(1, bulkActions)
        state = DesktopWorkspaceCommandAvailability(refreshEnabled = false, bulkActionsEnabled = false)
        menuRefresh()
        toolbarRefresh()
        markAll()
        assertEquals(2, refreshes)
        assertEquals(1, bulkActions)
        state = DesktopWorkspaceCommandAvailability()
        toolbarRefresh()
        assertEquals(3, refreshes)
    }
}
