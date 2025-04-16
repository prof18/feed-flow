package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedSourceNotificationPreference
import com.prof18.feedflow.core.model.NotificationSettingState
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationsViewModel internal constructor(
    private val databaseHelper: DatabaseHelper,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val notificationSettingsMutableStateFlow = MutableStateFlow(
        NotificationSettingState(
            feedSources = persistentListOf<FeedSourceNotificationPreference>().toImmutableList(),
            isEnabledForAll = false,
        ),
    )
    val notificationSettingState = notificationSettingsMutableStateFlow.asStateFlow()

    val syncPeriodFlow: StateFlow<SyncPeriod> = settingsRepository.syncPeriodFlow

    init {
        viewModelScope.launch {
            databaseHelper.getFeedSourcesFlow().collect { feedSources ->
                notificationSettingsMutableStateFlow.update {
                    NotificationSettingState(
                        feedSources = feedSources.map { feedSource ->
                            FeedSourceNotificationPreference(
                                feedSourceId = feedSource.id,
                                feedSourceTitle = feedSource.title,
                                isEnabled = feedSource.isNotificationEnabled,
                            )
                        }.toPersistentList(),
                        isEnabledForAll = if (feedSources.isEmpty()) {
                            false
                        } else {
                            feedSources.all { it.isNotificationEnabled }
                        },
                    )
                }
            }
        }
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
                forceUpdateSyncPeriod()
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
                forceUpdateSyncPeriod()
            }
        }
    }

    private fun forceUpdateSyncPeriod() {
        if (syncPeriodFlow.value == SyncPeriod.NEVER) {
            settingsRepository.setSyncPeriod(SyncPeriod.ONE_HOUR)
        }
    }

    fun updateSyncPeriod(period: SyncPeriod) {
        viewModelScope.launch {
            settingsRepository.setSyncPeriod(period)
        }
    }
}
