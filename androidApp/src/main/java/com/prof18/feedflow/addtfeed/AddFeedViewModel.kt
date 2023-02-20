package com.prof18.feedflow.addtfeed

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.FeedManagerRepository
import com.prof18.feedflow.FeedRetrieverRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddFeedViewModel(
    private val feedManagerRepository: FeedManagerRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
) : ViewModel() {

    private val feedUrlMutableState = mutableStateOf(TextFieldValue())
    val feedUrlState: State<TextFieldValue> = feedUrlMutableState

    private val feedNameMutableState = mutableStateOf(TextFieldValue())
    val feedNameState: State<TextFieldValue> = feedNameMutableState

    private val isAddDoneMutableState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isAddDoneState = isAddDoneMutableState.asStateFlow()

    fun updateFeedUrlTextFieldValue(feedUrlTextFieldValue: TextFieldValue) {
        feedUrlMutableState.value = feedUrlTextFieldValue
    }

    fun updateFeedNameTextFieldValue(feedNameTextFieldValue: TextFieldValue) {
        feedNameMutableState.value = feedNameTextFieldValue
    }

    // TODO: handle category
    fun addFeed() {
        viewModelScope.launch {
            feedManagerRepository.addFeed(
                url = feedUrlState.value.text,
                name = feedNameState.value.text,
            )
            isAddDoneMutableState.update { true }
            feedUrlMutableState.value = TextFieldValue()
            feedNameMutableState.value = TextFieldValue()
            feedRetrieverRepository.fetchFeeds()
        }
    }

}