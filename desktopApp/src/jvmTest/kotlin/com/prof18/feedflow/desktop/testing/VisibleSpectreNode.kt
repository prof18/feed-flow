package com.prof18.feedflow.desktop.testing

import dev.sebastiano.spectre.core.AutomatorNode
import dev.sebastiano.spectre.core.ComposeAutomator
import dev.sebastiano.spectre.core.InternalSpectreApi
import javax.swing.SwingUtilities

@OptIn(InternalSpectreApi::class)
internal suspend fun ComposeAutomator.awaitVisibleNode(tag: String): AutomatorNode {
    waitForNode(tag = tag)
    waitUntil(description = "Native surface for $tag is visible") {
        var ready = false
        SwingUtilities.invokeAndWait {
            ready = windows().any { surface ->
                val node = surface.findOneByTestTag(tag)
                node != null && surface.trackedWindow.window.isShowing &&
                    surface.trackedWindow.composeSurfaceBoundsOnScreen.contains(node.centerOnScreen)
            }
        }
        ready
    }
    val node = checkNotNull(findOneByTestTag(tag))
    val host = tree().windows().single { it.surfaceId == node.surfaceId }.trackedWindow
    println("Native gesture: tag=$tag node=${node.boundsOnScreen} host=${host.composeSurfaceBoundsOnScreen}")
    return node
}
