package com.prof18.feedflow.desktop.accounts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.testing.AccountE2eIds
import com.prof18.feedflow.desktop.testing.NativeDesktopWindowFixture
import com.prof18.feedflow.desktop.testing.NativeDesktopWindowFixtureRule
import com.prof18.feedflow.desktop.testing.awaitVisibleNode
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import dev.sebastiano.spectre.core.ComposeAutomator
import dev.sebastiano.spectre.core.RobotDriver
import dev.sebastiano.spectre.testing.ComposeAutomatorRule
import dev.sebastiano.spectre.testing.runSpectreTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(Parameterized::class)
class ProviderContentSpectreTest(private val account: SyncAccounts, private val theme: ThemeMode) {
    private val fake = FakeDesktopAccount(account)
    private val fixture = NativeDesktopWindowFixture {
        CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
            FeedFlowTheme(darkTheme = theme != ThemeMode.LIGHT, useOledTheme = theme == ThemeMode.OLED) {
                Box(Modifier.size(width = 560.dp, height = 640.dp)) {
                    AccountTestContent(fake)
                }
            }
        }
    }
    private val spectre = ComposeAutomatorRule(
        factory = {
            ComposeAutomator.inProcess(robotDriver = RobotDriver.synthetic(rootWindow = fixture.awaitWindow()))
        },
    )

    @get:Rule
    val rules = RuleChain.outerRule(NativeDesktopWindowFixtureRule(fixture)).around(spectre)

    @Test
    fun `native provider surface supports guarded loading failure retry and linked states`() = runSpectreTest {
        val automator = spectre.automator
        onEdt { fake.populateDraft() }

        if (account == SyncAccounts.DROPBOX) {
            clickVisible(automator, "dropbox-start-auth")
        }
        clickVisible(automator, connectTag())
        automator.waitUntil(description = "The provider has received its connection request") {
            fake.requests.get() == 1
        }
        assertEquals(1, fake.requests.get(), "The provider's real connect control should submit once")

        onEdt {
            fake.loginLoading.value = true
            fake.uiState.value = com.prof18.feedflow.core.model.AccountConnectionUiState.Loading
        }
        automator.waitForNode(text = EnFeedFlowStrings.desktopAccountUpdating)
        assertNotNull(automator.findOneByText(EnFeedFlowStrings.desktopAccountUpdating))
        check(automator.findOneByTestTag(connectTag()) == null) {
            "Connection controls should be removed while this provider is loading"
        }

        onEdt { fake.fail() }
        automator.waitForNode(text = FakeDesktopAccount.ERROR)
        assertNotNull(automator.findOneByText(FakeDesktopAccount.ERROR))
        clickVisible(automator, connectTag())
        automator.waitUntil(description = "The provider has received its retry request") {
            fake.requests.get() == 2
        }
        assertEquals(2, fake.requests.get(), "A visible failure should permit one retry")

        onEdt { fake.link() }
        automator.waitForIdle()
        automator.awaitVisibleNode(AccountE2eIds.CONNECTED_MESSAGE)
        check(automator.findOneByTestTag(connectTag()) == null) {
            "A linked provider should not show its connect controls"
        }
    }

    private suspend fun clickVisible(automator: ComposeAutomator, tag: String) {
        automator.waitUntil(description = "The $tag control is enabled") {
            automator.findOneByTestTag(tag)?.isDisabled == false
        }
        automator.waitForIdle()
        val node = automator.awaitVisibleNode(tag)
        automator.focusWindow(node)
        automator.click(node)
    }

    private fun onEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait(action)
        }
    }

    private fun connectTag(): String = if (account == SyncAccounts.DROPBOX) {
        "dropbox-confirm-auth"
    } else {
        AccountE2eIds.CONNECT_BUTTON
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}, {1}")
        fun providers(): List<Array<Any>> = SyncAccounts.entries
            .filter { it != SyncAccounts.LOCAL }
            .flatMap { account ->
                listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED).map { theme -> arrayOf(account, theme) }
            }
    }
}
