package com.prof18.feedflow.presentation

import com.prof18.feedflow.MR
import com.prof18.feedflow.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.domain.model.AddFeedResponse
import com.prof18.feedflow.domain.model.FeedAddedState
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AddFeedViewModel internal constructor(
    private val feedRetrieverRepository: FeedRetrieverRepository,
) : BaseViewModel() {

    private var feedUrl: String = ""
    private val feedAddedMutableState: MutableSharedFlow<FeedAddedState> = MutableSharedFlow()

    @NativeCoroutines
    val feedAddedState = feedAddedMutableState.asSharedFlow()

    fun updateFeedUrlTextFieldValue(feedUrlTextFieldValue: String) {
        feedUrl = feedUrlTextFieldValue
        scope.launch {
            feedAddedMutableState.emit(FeedAddedState.FeedNotAdded)
        }
    }

    fun addFeed() {
        scope.launch {
            if (feedUrl.isNotEmpty()) {
                when (val feedResponse = feedRetrieverRepository.fetchSingleFeed(feedUrl)) {
                    is AddFeedResponse.FeedFound -> {
                        feedRetrieverRepository.addFeedSource(feedResponse)
                        feedAddedMutableState.emit(
                            FeedAddedState.FeedAdded(
                                message = StringDesc.ResourceFormatted(
                                    stringRes = MR.strings.feed_added_message,
                                    feedResponse.parsedFeedSource.title,
                                ),
                            ),
                        )
                    }

                    AddFeedResponse.EmptyFeed -> {
                        feedAddedMutableState.emit(
                            FeedAddedState.Error(
                                errorMessage = StringDesc.ResourceFormatted(
                                    stringRes = MR.strings.missing_title_and_link,
                                ),
                            ),
                        )
                    }

                    AddFeedResponse.NotRssFeed -> {
                        feedAddedMutableState.emit(
                            FeedAddedState.Error(
                                errorMessage = StringDesc.ResourceFormatted(
                                    stringRes = MR.strings.invalid_rss_url,
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }

    fun clearAddDoneState() {
        scope.launch {
            feedAddedMutableState.emit(FeedAddedState.FeedNotAdded)
        }
    }
}
