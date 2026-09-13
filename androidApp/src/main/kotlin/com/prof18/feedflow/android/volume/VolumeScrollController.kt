package com.prof18.feedflow.android.volume

enum class VolumeScrollDirection {
    UP,
    DOWN,
}

/**
 * Active handler for volume-button scrolling. Screens register while visible via
 * [RememberVolumeScrollHandler]; [com.prof18.feedflow.android.MainActivity] dispatches keys here.
 */
fun interface VolumeScrollHandler {
    /** @return true if the event was consumed (system volume should not change). */
    fun onVolumeScroll(direction: VolumeScrollDirection): Boolean
}

object VolumeScrollController {
    private val handlers = ArrayDeque<VolumeScrollHandler>()

    @Synchronized
    fun push(handler: VolumeScrollHandler) {
        handlers.addLast(handler)
    }

    @Synchronized
    fun remove(handler: VolumeScrollHandler) {
        handlers.remove(handler)
    }

    @Synchronized
    fun dispatch(direction: VolumeScrollDirection): Boolean =
        handlers.lastOrNull()?.onVolumeScroll(direction) == true
}
