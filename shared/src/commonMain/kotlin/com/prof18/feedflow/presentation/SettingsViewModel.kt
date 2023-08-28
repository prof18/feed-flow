package com.prof18.feedflow.presentation

import co.touchlab.kermit.Logger
import com.prof18.feedflow.MR
import com.prof18.feedflow.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.domain.opml.OpmlInput
import com.prof18.feedflow.domain.opml.OpmlOutput
import com.prof18.feedflow.presentation.model.UIErrorState
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val feedManagerRepository: FeedManagerRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
    private val logger: Logger,
) : BaseViewModel() {

    private val isImportDoneMutableState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @NativeCoroutinesState
    val isImportDoneState = isImportDoneMutableState.asStateFlow()

    private val isExportDoneMutableState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @NativeCoroutinesState
    val isExportDoneState = isExportDoneMutableState.asStateFlow()

    private val mutableUIErrorState: MutableSharedFlow<UIErrorState?> = MutableSharedFlow()

    @NativeCoroutines
    val errorState = mutableUIErrorState.asSharedFlow()

    @Suppress("TooGenericExceptionCaught")
    fun importFeed(opmlInput: OpmlInput) {
        scope.launch {
            isImportDoneMutableState.update { false }
            try {
                feedManagerRepository.addFeedsFromFile(opmlInput)
                isImportDoneMutableState.update { true }
                feedRetrieverRepository.fetchFeeds(updateLoadingInfo = false)
            } catch (e: Throwable) {
                logger.e(e) { "Error while importing feed" }
                mutableUIErrorState.emit(
                    UIErrorState(
                        message = StringDesc.Resource(MR.strings.generic_error_message),
                    ),
                )
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun exportFeed(opmlOutput: OpmlOutput) {
        scope.launch {
            isExportDoneMutableState.update { false }
            try {
                feedManagerRepository.exportFeedsAsOpml(opmlOutput)
                isExportDoneMutableState.update { true }
            } catch (e: Throwable) {
                logger.e(e) { "Error while exporting feed" }
                mutableUIErrorState.emit(
                    UIErrorState(
                        message = StringDesc.Resource(MR.strings.generic_error_message),
                    ),
                )
            }
        }
    }
}
