@file:Suppress("MagicNumber")

package com.prof18.feedflow.android.e2e

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.prof18.feedflow.android.MainActivity
import com.prof18.feedflow.android.base.BaseThemeActivity
import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.prof18.feedflow.shared.e2e.E2eSeedProfile
import com.prof18.feedflow.shared.e2e.E2eSeedRunner
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class E2eSeedActivity : BaseThemeActivity() {

    private val seedRunner by inject<E2eSeedRunner>()
    private val widgetSettingsRepository by inject<WidgetSettingsRepository>()

    private var uiState by mutableStateOf<E2eSeedUiState>(E2eSeedUiState.Running)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runSeed(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        runSeed(intent)
    }

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (val state = uiState) {
                E2eSeedUiState.Running -> {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag("e2e_seed_running"),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("E2E seed running")
                }

                is E2eSeedUiState.Success -> {
                    Text(
                        text = "E2E seed complete",
                        modifier = Modifier
                            .testTag("e2e_seed_complete")
                            .semantics { contentDescription = "e2e_seed_complete" },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Profile: ${state.profileName}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        modifier = Modifier
                            .testTag("e2e_open_app")
                            .semantics { contentDescription = "e2e_open_app" },
                        onClick = ::openMainActivity,
                    ) {
                        Text("Open FeedFlow")
                    }
                }

                is E2eSeedUiState.Error -> {
                    Text(
                        text = "E2E seed failed",
                        modifier = Modifier
                            .testTag("e2e_seed_error")
                            .semantics { contentDescription = "e2e_seed_error" },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.message)
                }
            }
        }
    }

    private fun runSeed(intent: Intent?) {
        val uri = intent?.data
        val action = uri?.pathSegments?.firstOrNull()
        val profileName = uri?.getQueryParameter("profile")
        val accountName = uri?.getQueryParameter("account")
        val deepLinkUrl = uri?.getQueryParameter("url")

        if (action == null) {
            uiState = E2eSeedUiState.Error("Missing E2E seed action")
            return
        }

        if (action == ACTION_OPEN_DEEP_LINK) {
            openMainActivity(deepLinkUrl)
            return
        }

        val profile = E2eSeedProfile.fromQueryValue(profileName)
            ?: E2eSeedProfile.CONTENT_RICH

        uiState = E2eSeedUiState.Running
        lifecycleScope.launch {
            try {
                resetWidgetSettings()
                seedRunner.run(action = action, profileName = profileName, accountName = accountName)
                if (action != E2eSeedRunner.ACTION_RESET && profile == E2eSeedProfile.ANDROID_WIDGET) {
                    applyAndroidWidgetProfile()
                }
                uiState = E2eSeedUiState.Success(profile.queryValue)
            } catch (throwable: Throwable) {
                uiState = E2eSeedUiState.Error(
                    message = throwable.message ?: "Unknown E2E seed failure",
                )
            }
        }
    }

    private fun resetWidgetSettings() {
        widgetSettingsRepository.setFeedWidgetLayout(WidgetFeedLayout.LIST)
        widgetSettingsRepository.setWidgetShowHeader(true)
        widgetSettingsRepository.setWidgetFontScaleFactor(0)
        widgetSettingsRepository.setWidgetBackgroundColor(null)
        widgetSettingsRepository.setWidgetBackgroundOpacityPercent(100)
        widgetSettingsRepository.setWidgetTextColorMode(WidgetTextColorMode.AUTOMATIC)
        widgetSettingsRepository.setWidgetHideImages(false)
    }

    private fun applyAndroidWidgetProfile() {
        widgetSettingsRepository.setFeedWidgetLayout(WidgetFeedLayout.CARD)
        widgetSettingsRepository.setWidgetShowHeader(true)
        widgetSettingsRepository.setWidgetFontScaleFactor(2)
        widgetSettingsRepository.setWidgetBackgroundColor(0xFF1E3A5F.toInt())
        widgetSettingsRepository.setWidgetBackgroundOpacityPercent(85)
        widgetSettingsRepository.setWidgetTextColorMode(WidgetTextColorMode.LIGHT)
        widgetSettingsRepository.setWidgetHideImages(false)
    }

    private fun openMainActivity(deepLinkUrl: String? = null) {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        deepLinkUrl?.let { url ->
            intent.data = android.net.Uri.parse(url)
        }
        startActivity(intent)
        finish()
    }

    private companion object {
        const val ACTION_OPEN_DEEP_LINK = "open-deep-link"
    }
}

private sealed interface E2eSeedUiState {
    data object Running : E2eSeedUiState
    data class Success(val profileName: String) : E2eSeedUiState
    data class Error(val message: String) : E2eSeedUiState
}
