package com.prof18.feedflow.android.feedsourcelist

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.presentation.FeedSourceListViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun FeedSourceVerificationActions(
    source: FeedSource,
    hideMenu: () -> Unit,
    onVerificationRequested: () -> Unit,
) {
    val viewModel = koinViewModel<FeedSourceListViewModel>()
    val states by viewModel.recoveryStates.collectAsStateWithLifecycle()
    val available = viewModel.verificationAvailable
    val state = states[source.id] ?: return
    val strings = LocalFeedFlowStrings.current
    Column {
        if (state.verificationRequired) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (available) {
                            strings.feedVerifyWebsite
                        } else {
                            strings.feedVerificationUnsupported
                        },
                    )
                },
                enabled = available,
                onClick = {
                    onVerificationRequested()
                    hideMenu()
                },
            )
        }
        if (state.profileId != null) {
            DropdownMenuItem(
                text = { Text(strings.feedClearVerification) },
                onClick = {
                    viewModel.clearWebsiteVerification(source)
                    hideMenu()
                },
            )
        }
    }
}
