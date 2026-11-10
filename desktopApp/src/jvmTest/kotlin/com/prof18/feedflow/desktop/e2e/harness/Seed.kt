package com.prof18.feedflow.desktop.e2e.harness

import com.prof18.feedflow.shared.e2e.E2eSeedAccount
import com.prof18.feedflow.shared.e2e.E2eSeedProfile

/** A startup seed passed to the app as `feedflow.e2e.seed` (see E2eSeedRunner). */
data class Seed(
    val action: String,
    val profile: String,
    val account: String? = null,
    val audioUrl: String? = null,
) {
    val propertyValue: String
        get() = listOfNotNull(action, profile, account).joinToString(":")

    companion object {
        private const val RESET_AND_SEED = "reset-and-seed"

        val Empty = of(E2eSeedProfile.EMPTY)
        val AudioEpisode = of(E2eSeedProfile.AUDIO_EPISODE).copy(audioUrl = SEED_AUDIO_URL)
        val ContentRich = of(E2eSeedProfile.CONTENT_RICH)
        val CardLayout = of(E2eSeedProfile.CARD_LAYOUT)
        val BigImageLayout = of(E2eSeedProfile.BIG_IMAGE_LAYOUT)
        val GridLayout = of(E2eSeedProfile.GRID_LAYOUT)
        val CompactList = of(E2eSeedProfile.COMPACT_LIST)
        val ReaderMode = of(E2eSeedProfile.READER_MODE)
        val ExternalBrowser = of(E2eSeedProfile.EXTERNAL_BROWSER)
        val ReadBehavior = of(E2eSeedProfile.READ_BEHAVIOR)
        val OldestFirst = of(E2eSeedProfile.OLDEST_FIRST)
        val SwipeActions = of(E2eSeedProfile.SWIPE_ACTIONS)
        val SwipeLeftOnly = of(E2eSeedProfile.SWIPE_LEFT_ONLY)
        val SwipeDisabled = of(E2eSeedProfile.SWIPE_DISABLED)
        val Notifications = of(E2eSeedProfile.NOTIFICATIONS)
        val AndroidWidget = of(E2eSeedProfile.ANDROID_WIDGET)
        val SyncLinkedMock = of(E2eSeedProfile.SYNC_LINKED_MOCK)
        val SyncUploadRequired = of(E2eSeedProfile.SYNC_UPLOAD_REQUIRED)
        val LargeContent = of(E2eSeedProfile.LARGE_CONTENT)
        val PaginationScrollRead = of(E2eSeedProfile.PAGINATION_SCROLL_READ)
        val ReorderDrag = of(E2eSeedProfile.REORDER_DRAG)
        val FeedContent = of(E2eSeedProfile.FEED_CONTENT)
        val RtlContent = of(E2eSeedProfile.RTL_CONTENT)

        fun of(profile: E2eSeedProfile, account: E2eSeedAccount? = null): Seed =
            Seed(action = RESET_AND_SEED, profile = profile.queryValue, account = account?.queryValue)

        /** The report-<name> actions seed CONTENT_RICH plus the report article; the profile is ignored. */
        fun report(responseName: String): Seed =
            Seed(action = "report-$responseName", profile = E2eSeedProfile.CONTENT_RICH.queryValue)
    }
}
