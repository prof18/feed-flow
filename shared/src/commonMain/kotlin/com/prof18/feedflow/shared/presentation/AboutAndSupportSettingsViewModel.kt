package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.presentation.model.AboutAndSupportState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AboutAndSupportSettingsViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val stateMutableFlow = MutableStateFlow(AboutAndSupportState())
    val state: StateFlow<AboutAndSupportState> = stateMutableFlow.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val isCrashReportingEnabled = settingsRepository.getCrashReportingEnabled()

        stateMutableFlow.update {
            AboutAndSupportState(
                isCrashReportingEnabled = isCrashReportingEnabled,
            )
        }
    }

    fun updateCrashReporting(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCrashReportingEnabled(value)
            stateMutableFlow.update {
                it.copy(isCrashReportingEnabled = value)
            }
        }
    }
}
