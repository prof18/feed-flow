package com.prof18.feedflow.desktop.home

import com.prof18.feedflow.core.model.FeedOperation
import com.prof18.feedflow.core.model.FeedUpdateStatus
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.shared.presentation.HomeViewModel

data class DesktopWorkspaceCommandAvailability(
    val refreshEnabled: Boolean = true,
    val bulkActionsEnabled: Boolean = true,
)

internal enum class DesktopWorkspaceCommand {
    Refresh,
    BulkAction,
}

internal fun desktopWorkspaceCommandAvailability(
    updateStatus: FeedUpdateStatus,
    operation: FeedOperation,
): DesktopWorkspaceCommandAvailability {
    val enabled = !updateStatus.isLoading() && updateStatus !is NoFeedSourcesStatus && operation == FeedOperation.None
    return DesktopWorkspaceCommandAvailability(refreshEnabled = enabled, bulkActionsEnabled = enabled)
}

internal fun HomeViewModel.desktopWorkspaceCommandAvailability(): DesktopWorkspaceCommandAvailability =
    desktopWorkspaceCommandAvailability(loadingState.value, feedOperationState.value)

internal fun desktopWorkspaceAction(
    command: DesktopWorkspaceCommand,
    availability: () -> DesktopWorkspaceCommandAvailability,
    action: () -> Unit,
): () -> Unit = {
    val current = availability()
    val enabled = when (command) {
        DesktopWorkspaceCommand.Refresh -> current.refreshEnabled
        DesktopWorkspaceCommand.BulkAction -> current.bulkActionsEnabled
    }
    if (enabled) action()
}
