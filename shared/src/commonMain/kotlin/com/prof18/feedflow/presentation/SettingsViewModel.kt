package com.prof18.feedflow.presentation

import com.prof18.feedflow.domain.FeedRetrieverRepository
import com.prof18.feedflow.domain.feedmanager.FeedManagerRepository
import com.prof18.feedflow.domain.opml.OPMLImporter
import com.prof18.feedflow.domain.opml.OPMLInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val feedManagerRepository: FeedManagerRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
    private val opmlImporter: OPMLImporter,
) : BaseViewModel() {

    private val isImportDoneMutableState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isImportDoneState = isImportDoneMutableState.asStateFlow()

    fun importFeed(opmlInput: OPMLInput) {
        scope.launch {
            isImportDoneMutableState.update { false }
            val feed = opmlImporter.getOPML(opmlInput)
            // todo: add a try/catch?
            feedManagerRepository.addFeedsFromFile(feed)
            isImportDoneMutableState.update { true }
            feedRetrieverRepository.fetchFeeds(updateLoadingInfo = false)
        }
    }
}