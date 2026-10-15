package com.prof18.feedflow.desktop.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DesktopFeedbackState {
    private val queue = Mutex()
    var currentFeedback: DesktopFeedbackData? by mutableStateOf(null)
        private set

    suspend fun showFeedback(
        message: String,
        actionLabel: String? = null,
        duration: DesktopFeedbackDuration = if (actionLabel == null) {
            DesktopFeedbackDuration.Short
        } else {
            DesktopFeedbackDuration.Indefinite
        },
    ): DesktopFeedbackResult = queue.withLock {
        val completion = CompletableDeferred<DesktopFeedbackResult>()
        try {
            currentFeedback = FeedbackMessage(message, actionLabel, duration, completion)
            completion.await()
        } finally {
            currentFeedback = null
            completion.cancel()
        }
    }
}

private class FeedbackMessage(
    override val message: String,
    override val actionLabel: String?,
    override val duration: DesktopFeedbackDuration,
    private val completion: CompletableDeferred<DesktopFeedbackResult>,
) : DesktopFeedbackData {
    override fun dismiss() {
        completion.complete(DesktopFeedbackResult.Dismissed)
    }

    override fun performAction() {
        completion.complete(DesktopFeedbackResult.ActionPerformed)
    }
}

internal interface DesktopFeedbackData {
    val message: String
    val actionLabel: String?
    val duration: DesktopFeedbackDuration
    fun dismiss()
    fun performAction()
}

internal enum class DesktopFeedbackDuration { Short, Long, Indefinite }
internal enum class DesktopFeedbackResult { Dismissed, ActionPerformed }
