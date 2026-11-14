package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedSourceNotificationPreference
import com.prof18.feedflow.core.model.NotificationFeedGroup
import com.prof18.feedflow.core.model.NotificationMode
import com.prof18.feedflow.core.model.NotificationSettingState
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.BackgroundSyncScheduler
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.domain.notification.NotificationRefreshEnabler
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationsViewModel internal constructor(
    private val databaseHelper: DatabaseHelper,
    private val settingsRepository: SettingsRepository,
    private val notificationRefreshEnabler: NotificationRefreshEnabler,
    private val backgroundSyncScheduler: BackgroundSyncScheduler,
) : ViewModel() {

    private val notificationSettingsMutableStateFlow = MutableStateFlow(
        NotificationSettingState(
            feedSources = persistentListOf(),
            feedGroups = persistentListOf(),
            searchQuery = "",
            isEnabledForAll = false,
            notificationMode = NotificationMode.FEED_SOURCE,
        ),
    )
    val notificationSettingState = notificationSettingsMutableStateFlow.asStateFlow()

    private val backgroundRefreshTurnedOnMutable = MutableSharedFlow<SyncPeriod>()
    val backgroundRefreshTurnedOn: SharedFlow<SyncPeriod> = backgroundRefreshTurnedOnMutable.asSharedFlow()

    val syncPeriodFlow: StateFlow<SyncPeriod> = settingsRepository.syncPeriodFlow

    private val searchQueryMutableState = MutableStateFlow("")

    init {
        viewModelScope.launch {
            combine(
                databaseHelper.getFeedSourcesFlow(),
                searchQueryMutableState,
            ) { feedSources, searchQuery ->
                val preferences = feedSources
                    .map { feedSource ->
                        FeedSourceNotificationPreference(
                            feedSourceId = feedSource.id,
                            feedSourceTitle = feedSource.title,
                            categoryTitle = feedSource.category?.title,
                            isEnabled = feedSource.isNotificationEnabled,
                        )
                    }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.feedSourceTitle })
                NotificationSettingState(
                    feedSources = preferences.toPersistentList(),
                    feedGroups = preferences.toFeedGroups(searchQuery),
                    searchQuery = searchQuery,
                    isEnabledForAll = if (feedSources.isEmpty()) {
                        false
                    } else {
                        feedSources.all { it.isNotificationEnabled }
                    },
                    notificationMode = settingsRepository.getNotificationMode(),
                )
            }.collect { newState ->
                notificationSettingsMutableStateFlow.update { newState }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQueryMutableState.value = query
    }

    fun updateAllNotificationStatus(status: Boolean) {
        viewModelScope.launch {
            databaseHelper.updateAllNotificationsEnabledStatus(enabled = status)
            notificationSettingsMutableStateFlow.update { oldValue ->
                oldValue.copy(
                    isEnabledForAll = status,
                )
            }
            if (status) {
                enableRefreshForNotifications()
            }
        }
    }

    fun updateNotificationStatus(status: Boolean, feedSourceId: String) {
        viewModelScope.launch {
            databaseHelper.updateNotificationEnabledStatus(
                feedSourceId = feedSourceId,
                enabled = status,
            )
            if (status) {
                enableRefreshForNotifications()
            }
        }
    }

    fun updateSyncPeriod(period: SyncPeriod) {
        viewModelScope.launch {
            settingsRepository.setSyncPeriod(period)
            backgroundSyncScheduler.updateSyncPeriod(period)
        }
    }

    private suspend fun enableRefreshForNotifications() {
        notificationRefreshEnabler.enableIfOff()?.let { period ->
            backgroundRefreshTurnedOnMutable.emit(period)
        }
    }

    fun updateNotificationMode(mode: NotificationMode) {
        viewModelScope.launch {
            settingsRepository.setNotificationMode(mode)
            notificationSettingsMutableStateFlow.update { oldValue ->
                oldValue.copy(
                    notificationMode = mode,
                )
            }
        }
    }
}

private fun List<FeedSourceNotificationPreference>.toFeedGroups(
    searchQuery: String,
): PersistentList<NotificationFeedGroup> {
    val query = searchQuery.trim()
    return filter { query.isEmpty() || it.feedSourceTitle.contains(query, ignoreCase = true) }
        .groupBy { it.categoryTitle }
        .entries
        .sortedWith(
            compareBy<Map.Entry<String?, List<FeedSourceNotificationPreference>>> { it.key == null }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.key.orEmpty() },
        )
        .map { (categoryTitle, feedSources) ->
            NotificationFeedGroup(
                categoryTitle = categoryTitle,
                feedSources = feedSources.toPersistentList(),
            )
        }
        .toPersistentList()
}
