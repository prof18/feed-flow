package com.prof18.feedflow.android.settings.notifications

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import com.prof18.feedflow.core.model.FeedSourceNotificationPreference
import com.prof18.feedflow.core.model.NotificationFeedGroup
import com.prof18.feedflow.core.model.NotificationMode
import com.prof18.feedflow.core.model.NotificationSettingState
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2000dp")
class NotificationSettingsScreenContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun refreshRowChangesThePeriodInPlace() {
        val picked = mutableListOf<SyncPeriod>()
        setContent(onSyncPeriodSelected = { picked += it })

        compose.onNode(
            hasText(EnFeedFlowStrings.settingsSyncPeriodOneHour) and hasAnyAncestor(hasTestTag(REFRESH_ROW)),
        ).performClick()
        compose.onNodeWithText(EnFeedFlowStrings.settingsSyncPeriodSixHours).performClick()

        assertEquals(listOf(SyncPeriod.SIX_HOURS), picked)
    }

    @Test
    fun refreshRowAtNeverExplainsWhyNotificationsCantArrive() {
        setContent(syncPeriod = SyncPeriod.NEVER)

        compose.onNodeWithText(EnFeedFlowStrings.settingsNotificationsRefreshOff).assertExists()
    }

    @Test
    fun noRefreshOffHintWhenRefreshIsOn() {
        setContent(syncPeriod = SyncPeriod.ONE_HOUR)

        compose.onNodeWithText(EnFeedFlowStrings.settingsNotificationsRefreshOff).assertDoesNotExist()
    }

    @Test
    fun permissionRowRequestsPermission() {
        var requests = 0
        setContent(hasNotificationPermission = false, onRequestPermission = { requests++ })

        compose.onNodeWithTag(PERMISSION_ROW).assertExists()
        compose.onNodeWithText(EnFeedFlowStrings.settingsNotificationsPermissionAllow).performClick()

        assertEquals(1, requests)
    }

    @Test
    fun noPermissionRowWithPermission() {
        setContent(hasNotificationPermission = true)

        compose.onNodeWithTag(PERMISSION_ROW).assertDoesNotExist()
    }

    @Test
    fun feedSwitchStaysUsableAtNeverWithoutPermission() {
        val toggles = mutableListOf<Pair<String, Boolean>>()
        setContent(
            syncPeriod = SyncPeriod.NEVER,
            hasNotificationPermission = false,
            onFeedSourceNotificationsToggle = { id, status -> toggles += id to status },
        )

        val feedSwitch = compose.onNode(isToggleable() and hasAnyAncestor(hasTestTag("notifications_feed_tech-1")))
        feedSwitch.assertIsEnabled()
        feedSwitch.performClick()

        assertEquals(listOf("tech-1" to true), toggles)
    }

    @Test
    fun feedsAreGroupedUnderCategoryHeaders() {
        setContent()

        compose.onNodeWithText("Tech").assertExists()
        compose.onNodeWithText(EnFeedFlowStrings.noCategory).assertExists()
    }

    @Test
    fun categoryNamedNullAndUncategorizedFeedsBothRender() {
        val named = FeedSourceNotificationPreference("named-1", "Named", "null", isEnabled = false)
        setContent(
            notificationState = state.copy(
                feedSources = persistentListOf(named, loose),
                feedGroups = persistentListOf(
                    NotificationFeedGroup("null", persistentListOf(named)),
                    NotificationFeedGroup(null, persistentListOf(loose)),
                ),
            ),
        )

        compose.onNodeWithText("null").assertExists()
        compose.onNodeWithText(EnFeedFlowStrings.noCategory).assertExists()
    }

    @Test
    fun refreshConditionsAreNotOnThisPage() {
        setContent()

        compose.onNodeWithText(EnFeedFlowStrings.settingsBackgroundSyncWifiOnly).assertDoesNotExist()
    }

    private fun setContent(
        notificationState: NotificationSettingState = state,
        syncPeriod: SyncPeriod = SyncPeriod.ONE_HOUR,
        hasNotificationPermission: Boolean = true,
        onSyncPeriodSelected: (SyncPeriod) -> Unit = {},
        onRequestPermission: () -> Unit = {},
        onFeedSourceNotificationsToggle: (String, Boolean) -> Unit = { _, _ -> },
    ) {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) {
                    NotificationSettingsScreenContent(
                        notificationState = notificationState,
                        syncPeriod = syncPeriod,
                        hasNotificationPermission = hasNotificationPermission,
                        onNavigateBack = {},
                        onSyncPeriodSelected = onSyncPeriodSelected,
                        onRequestPermission = onRequestPermission,
                        onAllNotificationsToggle = {},
                        onFeedSourceNotificationsToggle = onFeedSourceNotificationsToggle,
                        onNotificationModeSelected = {},
                    )
                }
            }
        }
    }

    private companion object {
        const val REFRESH_ROW = "notifications_background_refresh"
        const val PERMISSION_ROW = "notifications_permission_row"

        val techOne = FeedSourceNotificationPreference("tech-1", "Android Weekly", "Tech", isEnabled = false)
        val techTwo = FeedSourceNotificationPreference("tech-2", "Kotlin Blog", "Tech", isEnabled = false)
        val loose = FeedSourceNotificationPreference("loose-1", "Zeta", null, isEnabled = false)

        val state = NotificationSettingState(
            feedSources = persistentListOf(techOne, techTwo, loose),
            feedGroups = persistentListOf(
                NotificationFeedGroup("Tech", persistentListOf(techOne, techTwo)),
                NotificationFeedGroup(null, persistentListOf(loose)),
            ),
            searchQuery = "",
            isEnabledForAll = false,
            notificationMode = NotificationMode.FEED_SOURCE,
        )
    }
}
