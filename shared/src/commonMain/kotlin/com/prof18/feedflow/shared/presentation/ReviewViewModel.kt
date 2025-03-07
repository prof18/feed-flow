package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.shared.data.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReviewViewModel internal constructor(
    private val reviewRepository: ReviewRepository,
) : ViewModel() {

    private val canShowReviewDialogMutableFlow = MutableStateFlow(false)
    val canShowReviewDialog: StateFlow<Boolean> = canShowReviewDialogMutableFlow.asStateFlow()

    init {
        viewModelScope.launch {
            val canShowReview = reviewRepository.shouldShowReview()
            canShowReviewDialogMutableFlow.emit(canShowReview)
        }
    }

    fun onReviewShown() {
        viewModelScope.launch {
            reviewRepository.onReviewShown()
        }
    }
}
