package com.prof18.feedflow.android.deeplink

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.shouldOpenInBrowser
import com.prof18.feedflow.shared.presentation.DeeplinkFeedViewModel
import com.prof18.feedflow.shared.presentation.model.DeeplinkFeedState
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DeepLinkScreen(
    feedId: String,
    navigateBack: () -> Unit,
    navigateToReaderMode: (FeedItemUrlInfo) -> Unit,
) {
    val viewModel: DeeplinkFeedViewModel = koinViewModel()
    val browserManager = koinInject<BrowserManager>()

    LaunchedEffect(feedId) {
        viewModel.getReaderModeUrl(FeedItemId(feedId))
    }

    val context = LocalContext.current
    val state by viewModel.deeplinkFeedState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        when (state) {
            DeeplinkFeedState.Error -> {
                navigateBack()
            }
            DeeplinkFeedState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    CircularProgressIndicator()
                }
            }
            is DeeplinkFeedState.Success -> {
                val feedUrlInfo = (state as DeeplinkFeedState.Success).data
                viewModel.markAsRead(FeedItemId(feedUrlInfo.id))
                when (feedUrlInfo.linkOpeningPreference) {
                    LinkOpeningPreference.READER_MODE -> navigateToReaderMode(feedUrlInfo)
                    LinkOpeningPreference.INTERNAL_BROWSER -> {
                        browserManager.openUrlWithFavoriteBrowser(feedUrlInfo.url, context)
                        navigateBack()
                    }
                    LinkOpeningPreference.PREFERRED_BROWSER -> {
                        browserManager.openUrlWithFavoriteBrowser(feedUrlInfo.url, context)
                        navigateBack()
                    }
                    LinkOpeningPreference.DEFAULT -> {
                        if (browserManager.openReaderMode() && !feedUrlInfo.shouldOpenInBrowser()) {
                            navigateToReaderMode(feedUrlInfo)
                        } else {
                            browserManager.openUrlWithFavoriteBrowser(feedUrlInfo.url, context)
                            navigateBack()
                        }
                    }
                }
            }
        }
    }
}
