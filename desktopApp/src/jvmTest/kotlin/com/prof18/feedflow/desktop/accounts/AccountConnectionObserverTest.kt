package com.prof18.feedflow.desktop.accounts

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.prof18.feedflow.core.model.AccountConnectionUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class AccountConnectionObserverTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `failure message is observed after provider detail unmounts`() {
        val parentMounted = mutableStateOf(true)
        val detailMounted = mutableStateOf(true)
        val uiState = MutableStateFlow<AccountConnectionUiState>(AccountConnectionUiState.Unlinked)
        val messages = MutableSharedFlow<TestConnectionMessage>(extraBufferCapacity = 1)
        var draft: CredentialsState? = null
        var handledMessages = 0
        rule.setContent {
            if (parentMounted.value) {
                val ownerScope = rememberCoroutineScope()
                val rememberedDraft = remember { CredentialsState(ownerScope) }
                SideEffect { draft = rememberedDraft }
                if (detailMounted.value) {
                    ObserveAccountConnection(rememberedDraft, uiState, messages) { message ->
                        handledMessages++
                        when (message) {
                            is TestConnectionMessage.Failure -> {
                                rememberedDraft.submissionPending = false
                                rememberedDraft.connectionError = message.message
                                rememberedDraft.connectionErrorRevision++
                            }
                            TestConnectionMessage.AuthReady -> rememberedDraft.authCodeReady = true
                        }
                    }
                }
            }
        }

        rule.waitUntil(timeoutMillis = 5_000) { messages.subscriptionCount.value > 0 }
        rule.runOnIdle {
            requireNotNull(draft).submissionPending = true
            detailMounted.value = false
        }
        rule.waitForIdle()
        messages.tryEmit(TestConnectionMessage.Failure("Synthetic connection failure"))
        rule.waitUntil(timeoutMillis = 5_000) {
            draft?.connectionError == "Synthetic connection failure"
        }

        rule.runOnIdle {
            assertEquals(false, requireNotNull(draft).submissionPending)
            assertEquals("Synthetic connection failure", draft?.connectionError)
            assertEquals(1, draft?.connectionErrorRevision)
            assertEquals(1, handledMessages)
        }
    }

    @Test
    fun `loading to unlinked clears pending work after detail unmounts`() {
        val parentMounted = mutableStateOf(true)
        val detailMounted = mutableStateOf(true)
        val uiState = MutableStateFlow<AccountConnectionUiState>(AccountConnectionUiState.Loading)
        val messages = MutableSharedFlow<TestConnectionMessage>(extraBufferCapacity = 1)
        var draft: CredentialsState? = null
        var handledMessages = 0
        rule.setContent {
            if (parentMounted.value) {
                val ownerScope = rememberCoroutineScope()
                val rememberedDraft = remember { CredentialsState(ownerScope) }
                SideEffect { draft = rememberedDraft }
                if (detailMounted.value) {
                    ObserveAccountConnection(rememberedDraft, uiState, messages) { handledMessages++ }
                }
            }
        }

        rule.waitUntil(timeoutMillis = 5_000) {
            messages.subscriptionCount.value > 0 && uiState.subscriptionCount.value > 0
        }
        rule.runOnIdle {
            requireNotNull(draft).submissionPending = true
            requireNotNull(draft).authCodePending = true
            detailMounted.value = false
        }
        rule.runOnIdle { uiState.value = AccountConnectionUiState.Unlinked }
        rule.waitUntil(timeoutMillis = 5_000) {
            draft?.submissionPending == false && draft?.authCodePending == false
        }

        rule.runOnIdle {
            assertEquals(0, handledMessages)
            assertEquals(0, draft?.connectionErrorRevision)
        }
    }

    @Test
    fun `login completion clears pending submission after provider detail unmounts`() {
        val parentMounted = mutableStateOf(true)
        val detailMounted = mutableStateOf(true)
        val uiState = MutableStateFlow<AccountConnectionUiState>(AccountConnectionUiState.Unlinked)
        val messages = MutableSharedFlow<TestConnectionMessage>(extraBufferCapacity = 1)
        val loginLoading = MutableStateFlow(false)
        var draft: CredentialsState? = null
        rule.setContent {
            if (parentMounted.value) {
                val ownerScope = rememberCoroutineScope()
                val rememberedDraft = remember { CredentialsState(ownerScope) }
                SideEffect { draft = rememberedDraft }
                if (detailMounted.value) {
                    ObserveAccountConnection(
                        draft = rememberedDraft,
                        uiState = uiState,
                        messages = messages,
                        loginLoading = loginLoading,
                    ) { }
                }
            }
        }

        rule.waitUntil(timeoutMillis = 5_000) { messages.subscriptionCount.value > 0 }
        rule.runOnIdle {
            requireNotNull(draft).submissionPending = true
            detailMounted.value = false
        }
        rule.runOnIdle { loginLoading.value = true }
        rule.runOnIdle { loginLoading.value = false }
        rule.waitUntil(timeoutMillis = 5_000) { draft?.submissionPending == false }

        rule.runOnIdle { assertEquals(false, draft?.submissionPending) }
    }

    @Test
    fun `authorization ready message updates draft after provider detail unmounts`() {
        val parentMounted = mutableStateOf(true)
        val detailMounted = mutableStateOf(true)
        val uiState = MutableStateFlow<AccountConnectionUiState>(AccountConnectionUiState.Unlinked)
        val messages = MutableSharedFlow<TestConnectionMessage>(extraBufferCapacity = 1)
        var draft: CredentialsState? = null
        rule.setContent {
            if (parentMounted.value) {
                val ownerScope = rememberCoroutineScope()
                val rememberedDraft = remember { CredentialsState(ownerScope) }
                SideEffect { draft = rememberedDraft }
                if (detailMounted.value) {
                    ObserveAccountConnection(rememberedDraft, uiState, messages) { message ->
                        if (message == TestConnectionMessage.AuthReady) rememberedDraft.authCodeReady = true
                    }
                }
            }
        }

        rule.waitUntil(timeoutMillis = 5_000) { messages.subscriptionCount.value > 0 }
        rule.runOnIdle { detailMounted.value = false }
        rule.waitForIdle()
        messages.tryEmit(TestConnectionMessage.AuthReady)
        rule.waitUntil(timeoutMillis = 5_000) { draft?.authCodeReady == true }

        rule.runOnIdle { assertEquals(true, draft?.authCodeReady) }
    }

    @Test
    fun `disposing parent cancels connection observation`() {
        val parentMounted = mutableStateOf(true)
        val detailMounted = mutableStateOf(true)
        val uiState = MutableStateFlow<AccountConnectionUiState>(AccountConnectionUiState.Unlinked)
        val messages = MutableSharedFlow<TestConnectionMessage>(extraBufferCapacity = 1)
        var draft: CredentialsState? = null
        var handledMessages = 0
        rule.setContent {
            if (parentMounted.value) {
                val ownerScope = rememberCoroutineScope()
                val rememberedDraft = remember { CredentialsState(ownerScope) }
                SideEffect { draft = rememberedDraft }
                if (detailMounted.value) {
                    ObserveAccountConnection(rememberedDraft, uiState, messages) { message ->
                        handledMessages++
                        if (message is TestConnectionMessage.Failure) {
                            rememberedDraft.connectionError = message.message
                        }
                    }
                }
            }
        }

        rule.waitUntil(timeoutMillis = 5_000) { messages.subscriptionCount.value > 0 }
        rule.runOnIdle { parentMounted.value = false }
        rule.waitUntil(timeoutMillis = 5_000) { messages.subscriptionCount.value == 0 }
        messages.tryEmit(TestConnectionMessage.Failure("Must not be handled"))
        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals(0, handledMessages)
            assertEquals(null, draft?.connectionError)
        }
    }
}

private sealed interface TestConnectionMessage {
    data class Failure(val message: String) : TestConnectionMessage
    data object AuthReady : TestConnectionMessage
}
