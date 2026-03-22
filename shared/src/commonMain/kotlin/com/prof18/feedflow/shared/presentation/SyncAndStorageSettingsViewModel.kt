package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.BackgroundSyncScheduler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.model.BackgroundSyncRestrictions
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.presentation.model.SyncAndStorageState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SyncAndStorageSettingsViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val backgroundSyncScheduler: BackgroundSyncScheduler,
) : ViewModel() {

    private val stateMutableFlow = MutableStateFlow(SyncAndStorageState())
    val state: StateFlow<SyncAndStorageState> = stateMutableFlow.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.syncPeriodFlow,
                settingsRepository.backgroundSyncRestrictionsFlow,
            ) { syncPeriod, backgroundSyncRestrictions ->
                syncPeriod to backgroundSyncRestrictions
            }.collect { (syncPeriod, backgroundSyncRestrictions) ->
                loadSettings(syncPeriod, backgroundSyncRestrictions)
            }
        }
    }

    private fun loadSettings(
        syncPeriod: SyncPeriod,
        backgroundSyncRestrictions: BackgroundSyncRestrictions,
    ) {
        val autoDeletePeriod = settingsRepository.getAutoDeletePeriod()
        val refreshFeedsOnLaunch = settingsRepository.getRefreshFeedsOnLaunch()
        val showRssParsingErrors = settingsRepository.getShowRssParsingErrors()

        stateMutableFlow.update {
            SyncAndStorageState(
                syncPeriod = syncPeriod,
                backgroundSyncRestrictions = backgroundSyncRestrictions,
                autoDeletePeriod = autoDeletePeriod,
                refreshFeedsOnLaunch = refreshFeedsOnLaunch,
                showRssParsingErrors = showRssParsingErrors,
            )
        }
    }

    fun updateSyncPeriod(period: SyncPeriod) {
        viewModelScope.launch {
            settingsRepository.setSyncPeriod(period)
            backgroundSyncScheduler.updateSyncPeriod(period)
            stateMutableFlow.update {
                it.copy(syncPeriod = period)
            }
        }
    }

    fun updateSyncOnlyOnWifi(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBackgroundSyncOnlyOnWifi(enabled)
            backgroundSyncScheduler.updateSyncPeriod(state.value.syncPeriod)
            stateMutableFlow.update {
                it.copy(
                    backgroundSyncRestrictions = it.backgroundSyncRestrictions.copy(
                        syncOnlyOnWifi = enabled,
                    ),
                )
            }
        }
    }

    fun updateSyncOnlyWhenCharging(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBackgroundSyncOnlyWhenCharging(enabled)
            backgroundSyncScheduler.updateSyncPeriod(state.value.syncPeriod)
            stateMutableFlow.update {
                it.copy(
                    backgroundSyncRestrictions = it.backgroundSyncRestrictions.copy(
                        syncOnlyWhenCharging = enabled,
                    ),
                )
            }
        }
    }

    fun updateAutoDeletePeriod(period: AutoDeletePeriod) {
        viewModelScope.launch {
            settingsRepository.setAutoDeletePeriod(period)
            stateMutableFlow.update {
                it.copy(autoDeletePeriod = period)
            }
        }
    }

    fun updateRefreshFeedsOnLaunch(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRefreshFeedsOnLaunch(enabled)
            stateMutableFlow.update {
                it.copy(refreshFeedsOnLaunch = enabled)
            }
        }
    }

    fun updateShowRssParsingErrors(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowRssParsingErrors(enabled)
            stateMutableFlow.update {
                it.copy(showRssParsingErrors = enabled)
            }
        }
    }

    fun clearDownloadedArticleContent() {
        viewModelScope.launch {
            feedItemContentFileHandler.clearAllContent()
        }
    }
}
