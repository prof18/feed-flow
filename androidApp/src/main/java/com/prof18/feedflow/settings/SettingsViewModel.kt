package com.prof18.feedflow.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.FeedManagerRepository
import com.prof18.feedflow.FeedRetrieverRepository
import com.prof18.feedflow.OPMLImporter
import com.prof18.feedflow.workmanager.WorkManagerHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val feedManagerRepository: FeedManagerRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
    private val opmlImporter: OPMLImporter,
    private val workManagerHandler: WorkManagerHandler,
) : ViewModel() {

    private val isImportDoneMutableState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isImportDoneState = isImportDoneMutableState.asStateFlow()

    fun importFeed(uri: Uri) {
        viewModelScope.launch {
            isImportDoneMutableState.update { false }
            val feed = opmlImporter.getOPML(uri)
            // todo: add a try/catch?
            feedManagerRepository.addFeedsFromFile(feed)
            isImportDoneMutableState.update { true }
            feedRetrieverRepository.fetchFeeds(updateLoadingInfo = false)
        }
    }

    fun scheduleCleaning(isCleaningEnabled: Boolean) {
        if (isCleaningEnabled) {
            workManagerHandler.enqueueCleanupWork()
        } else {
            // TODO: delete worker
        }
    }
}