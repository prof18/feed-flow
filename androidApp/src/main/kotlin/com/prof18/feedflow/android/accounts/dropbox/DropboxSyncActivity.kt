package com.prof18.feedflow.android.accounts.dropbox

import FeedFlowTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.BuildConfig
import com.prof18.feedflow.core.model.DropboxSynMessages
import com.prof18.feedflow.shared.presentation.DropboxSyncViewModel
import com.prof18.feedflow.shared.presentation.startDropboxAuth
import com.prof18.feedflow.shared.ui.accounts.dropbox.DropboxSyncContent
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.rememberFeedFlowStrings
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class DropboxSyncActivity : ComponentActivity() {

    private var isAuthOngoing = false

    private val viewModel by viewModel<DropboxSyncViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            FeedFlowTheme {
                val lyricist = rememberFeedFlowStrings()
                ProvideFeedFlowStrings(lyricist) {
                    val snackbarHostState = remember { SnackbarHostState() }
                    val scope = rememberCoroutineScope()

                    val errorMessage = LocalFeedFlowStrings.current.dropboxSyncError

                    LaunchedEffect(Unit) {
                        viewModel.dropboxSyncMessageState.collect { event ->
                            when (event) {
                                DropboxSynMessages.Error -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = errorMessage,
                                            duration = SnackbarDuration.Short,
                                        )
                                    }
                                }

                                is DropboxSynMessages.ProceedToAuth -> {
                                    // no-op
                                }
                            }
                        }
                    }

                    val uiState by viewModel.dropboxConnectionUiState.collectAsStateWithLifecycle()

                    DropboxSyncContent(
                        uiState = uiState,
                        onBackClick = {
                            finish()
                        },
                        onBackupClick = {
                            viewModel.triggerBackup()
                        },
                        onDisconnectClick = {
                            viewModel.unlink()
                        },
                        customPlatformUI = {
                            Column(
                                modifier = Modifier.padding(top = Spacing.regular),
                            ) {
                                Text(
                                    text = LocalFeedFlowStrings.current.dropboxSyncMobileDescription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = Spacing.regular),
                                )

                                SettingItem(
                                    modifier = Modifier.padding(top = Spacing.regular),
                                    title = LocalFeedFlowStrings.current.accountConnectButton,
                                    icon = Icons.Default.Link,
                                    onClick = {
                                        isAuthOngoing = true
                                        val apiKey = BuildConfig.DROPBOX_APP_KEY
                                        startDropboxAuth(this@DropboxSyncActivity, apiKey)
                                    },
                                )
                            }
                        },
                        snackbarHost = {
                            SnackbarHost(snackbarHostState)
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAuthOngoing) {
            viewModel.saveDropboxAuth()
            isAuthOngoing = false
        }
    }
}
