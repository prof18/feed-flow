package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.ReadLaterMarkerWithDetails
import com.prof18.feedflow.shared.domain.readlater.ReadLaterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReadLaterViewModel internal constructor(
    private val readLaterRepository: ReadLaterRepository,
) : ViewModel() {

    val readLaterMarkers: StateFlow<List<ReadLaterMarkerWithDetails>> =
        readLaterRepository.observeReadLaterMarkers()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    fun deleteMarker(id: String) {
        viewModelScope.launch {
            readLaterRepository.deleteMarker(id)
        }
    }
}
