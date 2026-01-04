package com.prof18.feedflow.android.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.prof18.feedflow.android.util.isSystemInDarkTheme
import com.prof18.feedflow.android.util.rememberSystemReducedMotionEnabled
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.presentation.ThemeViewModel
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import com.prof18.feedflow.shared.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.rememberFeedFlowStrings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

abstract class BaseThemeActivity : ComponentActivity() {

    private val themeViewModel by viewModel<ThemeViewModel>()
    private val settingsRepository by inject<SettingsRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // We keep this as a mutable state, so that we can track changes inside the composition.
        // This allows us to react to dark/light mode changes.
        var darkTheme by mutableStateOf(
            resources.configuration.isSystemInDarkTheme,
        )
        var useOledTheme by mutableStateOf(false)

        // Update the theme and system bars dynamically
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    isSystemInDarkTheme(),
                    themeViewModel.themeState,
                ) { systemDark, themeState ->
                    val isDarkTheme = when (themeState) {
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK -> true
                        ThemeMode.OLED -> true
                        ThemeMode.SYSTEM -> systemDark
                    }
                    isDarkTheme to (themeState == ThemeMode.OLED)
                }
                    .onEach { (isDarkTheme, isOledTheme) ->
                        darkTheme = isDarkTheme
                        useOledTheme = isOledTheme
                    }
                    .distinctUntilChanged()
                    .collect { (isDark, isOled) ->
                        val navigationDarkScrim = if (isOled) {
                            android.graphics.Color.BLACK
                        } else {
                            darkScrim
                        }
                        // Update system bars dynamically
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.auto(
                                lightScrim = android.graphics.Color.TRANSPARENT,
                                darkScrim = android.graphics.Color.TRANSPARENT,
                            ) { isDark },
                            navigationBarStyle = SystemBarStyle.auto(
                                lightScrim = lightScrim,
                                darkScrim = navigationDarkScrim,
                            ) { isDark },
                        )
                    }
            }
        }

        setContent {
            val reduceMotionOverride by settingsRepository.reduceMotionEnabledFlow.collectAsStateWithLifecycle()
            val systemReducedMotion = rememberSystemReducedMotionEnabled()
            val reduceMotionEnabled = reduceMotionOverride || systemReducedMotion

            FeedFlowTheme(
                darkTheme = darkTheme,
                useOledTheme = useOledTheme,
            ) {
                val lyricist = rememberFeedFlowStrings()
                ProvideFeedFlowStrings(lyricist) {
                    CompositionLocalProvider(LocalReduceMotion provides reduceMotionEnabled) {
                        Content()
                    }
                }
            }
        }
    }

    @Composable
    abstract fun Content()
}

/**
 * The default light scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=35-38;drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

/**
 * The default dark scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=40-44;drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)
