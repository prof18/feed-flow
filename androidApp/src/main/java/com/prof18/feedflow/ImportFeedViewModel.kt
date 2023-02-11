package com.prof18.feedflow

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ImportFeedViewModel(
    private val feedManagerRepository: FeedManagerRepository,
    private val opmlImporter: OPMLImporter,
) : ViewModel() {


    fun importFeed(uri: Uri) {
        viewModelScope.launch {
            val feed = opmlImporter.getOPML(uri)
            // todo: add a try/catch?
            feedManagerRepository.addFeedsFromFile(feed)
            // todo: send a result back to activity
        }
    }


}