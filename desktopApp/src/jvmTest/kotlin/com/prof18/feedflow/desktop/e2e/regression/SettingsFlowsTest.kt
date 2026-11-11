package com.prof18.feedflow.desktop.e2e.regression

import com.prof18.feedflow.desktop.e2e.harness.DesktopE2eRule
import com.prof18.feedflow.desktop.e2e.harness.Flow
import com.prof18.feedflow.desktop.e2e.harness.SEED_NEWEST_TITLE
import com.prof18.feedflow.desktop.e2e.harness.SEED_READ_TITLE
import com.prof18.feedflow.desktop.e2e.harness.Seed
import com.prof18.feedflow.desktop.e2e.harness.awaitTextIn
import com.prof18.feedflow.desktop.e2e.harness.choose
import com.prof18.feedflow.desktop.e2e.harness.clickIn
import com.prof18.feedflow.desktop.e2e.harness.closeWindow
import com.prof18.feedflow.desktop.e2e.harness.dropdownIn
import com.prof18.feedflow.desktop.e2e.harness.isSwitchOn
import com.prof18.feedflow.desktop.e2e.harness.nodesIn
import com.prof18.feedflow.desktop.e2e.harness.openAddMenuItem
import com.prof18.feedflow.desktop.e2e.harness.openSettings
import com.prof18.feedflow.desktop.e2e.harness.scrollInWindow
import com.prof18.feedflow.desktop.e2e.harness.scrollToText
import com.prof18.feedflow.desktop.e2e.harness.setSwitch
import com.prof18.feedflow.desktop.e2e.harness.sidebarCount
import com.prof18.feedflow.desktop.e2e.harness.sidebarRow
import com.prof18.feedflow.desktop.e2e.harness.switchIn
import com.prof18.feedflow.desktop.e2e.harness.typeIntoFocused
import com.prof18.feedflow.i18n.ItFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsFlowsTest {

    @get:Rule
    val e2e = DesktopE2eRule()

    /**
     * refresh on launch, refresh while open, auto-delete, delete offline copies: desktop
     * settings/SyncStoragePane.kt (settingsRefreshFeedsOnLaunch, settingsDesktopRefreshWhileOpen,
     * settingsAutoDelete, settingsClearDownloadedArticles)
     */
    @Test
    fun `REG-113 sync storage settings`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openSettings(flow.strings.settingsSyncAndStorage)
        flow.setSwitch(SETTINGS(flow), flow.strings.settingsRefreshFeedsOnLaunch, on = true)
        flow.choose(
            SETTINGS(flow),
            flow.strings.settingsDesktopRefreshWhileOpen,
            flow.strings.settingsSyncPeriodOneHour,
        )
        flow.choose(SETTINGS(flow), flow.strings.settingsAutoDelete, flow.strings.settingsAutoDeletePeriodOneWeek)
        flow.clickSettingsButton(flow.strings.settingsClearDownloadedArticles)
        flow.cancelDialog(flow.strings.settingsClearDownloadedArticlesDialogTitle)
    }

    // theme, hide unread counts, reduce motion: desktop settings/GeneralPane.kt (settingsTheme,
    // settingsThemeOled, settingsHideUnreadCount, settingsReduceMotion)
    @Test
    fun `REG-114 appearance settings`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)
        assertTrue(flow.sidebarCount(flow.strings.drawerTitleTimeline) != null)

        flow.openSettings(flow.strings.settingsAppTitle)
        flow.choose(SETTINGS(flow), flow.strings.settingsTheme, flow.strings.settingsThemeDark)
        flow.choose(SETTINGS(flow), flow.strings.settingsTheme, flow.strings.settingsThemeOled)
        flow.setSwitch(SETTINGS(flow), flow.strings.settingsHideUnreadCount, on = true)
        val reduceMotion = flow.isSwitchOn(SETTINGS(flow), flow.strings.settingsReduceMotion)
        flow.setSwitch(SETTINGS(flow), flow.strings.settingsReduceMotion, on = !reduceMotion)
        flow.closeWindow(SETTINGS(flow))
        flow.await("the Timeline count to disappear") {
            Unit.takeIf { flow.sidebarCount(flow.strings.drawerTitleTimeline) == null }
        }

        flow.openSettings(flow.strings.settingsAppTitle)
        assertTrue(flow.strings.settingsThemeOled in flow.dropdownIn(SETTINGS(flow), flow.strings.settingsTheme).texts)
        assertTrue(flow.isSwitchOn(SETTINGS(flow), flow.strings.settingsHideUnreadCount))
        assertTrue(flow.isSwitchOn(SETTINGS(flow), flow.strings.settingsReduceMotion) != reduceMotion)
    }

    /**
     * offline copies, prefetch confirmation, mark read when scrolling: desktop settings/ReadingPane.kt
     * (settingsSaveReaderModeContent, settingsPrefetchArticleContent, settingsPrefetchArticleContentWarning,
     * toggleMarkReadWhenScrolling)
     *
     * Not on desktop: the Browser row (BrowserManager.kt opens links in the system default browser).
     */
    @Test
    fun `REG-121 reading behavior secondary settings`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openSettings(flow.strings.settingsReadingBehavior)
        val saveContent = flow.isSwitchOn(SETTINGS(flow), flow.strings.settingsSaveReaderModeContent)
        flow.setSwitch(SETTINGS(flow), flow.strings.settingsSaveReaderModeContent, on = !saveContent)
        val markRead = flow.isSwitchOn(SETTINGS(flow), flow.strings.toggleMarkReadWhenScrolling)
        flow.setSwitch(SETTINGS(flow), flow.strings.toggleMarkReadWhenScrolling, on = !markRead)
        flow.click(flow.switchIn(SETTINGS(flow), flow.strings.settingsPrefetchArticleContent))
        flow.awaitWindow(flow.strings.settingsPrefetchArticleContent)
        flow.awaitTextIn(
            flow.strings.settingsPrefetchArticleContent,
            flow.strings.settingsPrefetchArticleContentWarning,
        )
        flow.clickIn(flow.strings.settingsPrefetchArticleContent, flow.strings.confirmButton)
        flow.awaitWindowGone(flow.strings.settingsPrefetchArticleContent)
        flow.await("prefetch on") {
            Unit.takeIf { flow.isSwitchOn(SETTINGS(flow), flow.strings.settingsPrefetchArticleContent) }
        }
    }

    // feed list detail controls: desktop settings/FeedListPane.kt:115-246
    @Test
    fun `REG-122 feed list settings detail controls`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openSettings(flow.strings.settingsFeedListTitle)
        flow.awaitTextIn(SETTINGS(flow), flow.strings.settingsFeedListScaleTitle)
        flow.click(flow.awaitDescriptionIn(flow.strings.increaseFontSizeButtonContentDescription))
        listOf(
            flow.strings.settingsHideDate,
            flow.strings.settingsHideUnreadDot,
            flow.strings.settingsHideFeedSource,
            flow.strings.settingsHideDuplicatedTitleFromDesc,
        ).forEach { flow.setSwitch(SETTINGS(flow), it, on = true) }
        flow.choose(
            SETTINGS(flow),
            flow.strings.settingsDescriptionMaxLines,
            flow.strings.settingsDescriptionLinesNoLimit,
        )
        flow.choose(SETTINGS(flow), flow.strings.dateFormatTitle, flow.strings.dateFormatIso)
        flow.choose(SETTINGS(flow), flow.strings.timeFormatTitle, flow.strings.timeFormatHours12)
        flow.choose(SETTINGS(flow), flow.strings.settingsLeftSwipeAction, flow.strings.settingsSwipeActionToggleRead)
        flow.choose(
            SETTINGS(flow),
            flow.strings.settingsRightSwipeAction,
            flow.strings.settingsSwipeActionToggleBookmark,
        )
    }

    /**
     * refresh while open and delete offline copies: desktop settings/SyncStoragePane.kt
     *
     * Skipped: the Android-only background sync Wi-Fi and charging restrictions.
     */
    @Test
    fun `REG-123 sync storage advanced settings`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openSettings(flow.strings.settingsSyncAndStorage)
        val refreshWhileOpen = flow.dropdownIn(SETTINGS(flow), flow.strings.settingsDesktopRefreshWhileOpen)
        assertTrue(flow.strings.settingsSyncPeriodNever in refreshWhileOpen.texts)
        flow.clickSettingsButton(flow.strings.settingsClearDownloadedArticles)
        flow.awaitTextIn(
            flow.strings.settingsClearDownloadedArticlesDialogTitle,
            flow.strings.settingsClearDownloadedArticlesDialogTitle,
        )
        flow.cancelDialog(flow.strings.settingsClearDownloadedArticlesDialogTitle)
    }

    /**
     * Desktop has no home "more" menu: Refresh feeds is a toolbar button (not pressed: it would fetch live
     * feeds), sort and show-read live in Settings → Feed list, and Delete week-old articles in Settings →
     * Sync & storage.
     */
    @Test
    fun `REG-124 home overflow secondary actions`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)
        flow.await("the Refresh feeds button") {
            flow.nodes().firstOrNull { node ->
                node.contentDescriptions.any { it.startsWith(flow.strings.refreshFeeds) }
            }
        }

        flow.openSettings(flow.strings.settingsFeedListTitle)
        flow.choose(SETTINGS(flow), flow.strings.settingsFeedOrderTitle, flow.strings.settingsFeedOrderOldestFirst)
        flow.setSwitch(SETTINGS(flow), flow.strings.settingsToggleShowReadArticles, on = true)
        flow.clickIn(SETTINGS(flow), flow.strings.settingsSyncAndStorage)
        flow.clickSettingsButton(flow.strings.clearOldArticlesButton)
        flow.awaitTextIn(flow.dialogTitle(), flow.strings.clearOldArticlesDialogMessage, exact = false)
        flow.cancelDialog(flow.dialogTitle())
        flow.closeWindow(SETTINGS(flow))
        flow.scrollToText(OLD_ARTICLE_TITLE)
        flow.scrollToText(SEED_READ_TITLE)

        flow.openSettings(flow.strings.settingsSyncAndStorage)
        flow.clickSettingsButton(flow.strings.clearOldArticlesButton)
        val dialog = flow.dialogTitle()
        flow.clickIn(dialog, flow.strings.confirmButton)
        flow.awaitWindowGone(dialog)
        flow.closeWindow(SETTINGS(flow))
        flow.click(flow.awaitContentDescription(flow.strings.searchButtonContentDescription))
        flow.awaitTag(SEARCH_FIELD_TAG)
        flow.typeIntoFocused(OLD_ARTICLE_SEARCH)
        flow.awaitText(flow.strings.searchNoData(OLD_ARTICLE_SEARCH))
    }

    // About pane: desktop settings/AboutPane.kt (reportIssueButton, aboutButton, openSourceLicenses)
    @Test
    fun `REG-125 about support navigation`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openSettings(flow.strings.settingsAboutAndSupport)
        flow.awaitTextIn(SETTINGS(flow), flow.strings.reportIssueButton)
        flow.awaitTextIn(SETTINGS(flow), ABOUT_TEXT_START, exact = false)
        flow.clickSettingsButton(flow.strings.openSourceLicenses)
        flow.await("the licenses page") {
            flow.windows().firstOrNull { it.title == flow.strings.openSourceLicenses && it.isShowing }
                ?: flow.nodesIn(SETTINGS(flow)).firstOrNull {
                    it.texts.firstOrNull() == flow.strings.openSourceLicenses && it.role == null
                }
        }
    }

    /**
     * FAQ: desktop settings/AboutPane.kt (aboutMenuFaq, Websites.faqUrl)
     *
     * The crash-reporting switch renders only in release builds (settings/AboutPane.kt:102), so it is
     * absent in this debug build.
     */
    @Test
    fun `REG-132 about support secondary options`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openSettings(flow.strings.settingsAboutAndSupport)
        flow.awaitTextIn(SETTINGS(flow), flow.strings.reportIssueButton)
        assertTrue(flow.nodesIn(SETTINGS(flow)).none { flow.strings.settingsCrashReporting in it.texts })
        flow.clickSettingsButton(flow.strings.aboutMenuFaq)
        flow.awaitOpenedUri { it.startsWith("https://www.feedflow.dev/faq") }
    }

    // feed list persistence: desktop settings/FeedListPane.kt
    @Test
    fun `REG-135 feed list secondary persistence`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openSettings(flow.strings.settingsFeedListTitle)
        val choices = listOf(
            flow.strings.settingsDescriptionMaxLines to flow.strings.settingsDescriptionLinesNoLimit,
            flow.strings.dateFormatTitle to flow.strings.dateFormatIso,
            flow.strings.timeFormatTitle to flow.strings.timeFormatHours12,
            flow.strings.settingsLeftSwipeAction to flow.strings.settingsSwipeActionToggleRead,
            flow.strings.settingsRightSwipeAction to flow.strings.settingsSwipeActionToggleBookmark,
        )
        choices.forEach { (label, option) -> flow.choose(SETTINGS(flow), label, option) }
        flow.closeWindow(SETTINGS(flow))

        flow.relaunch()
        flow.openSettings(flow.strings.settingsFeedListTitle)
        choices.forEach { (label, option) ->
            assertTrue(option in flow.dropdownIn(SETTINGS(flow), label).texts, "$label shows $option")
        }
    }

    // sync and storage persistence: desktop settings/SyncStoragePane.kt
    @Test
    fun `REG-136 sync storage secondary persistence`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openSettings(flow.strings.settingsSyncAndStorage)
        flow.choose(
            SETTINGS(flow),
            flow.strings.settingsDesktopRefreshWhileOpen,
            flow.strings.settingsSyncPeriodOneHour,
        )
        flow.choose(SETTINGS(flow), flow.strings.settingsAutoDelete, flow.strings.settingsAutoDeletePeriodOneWeek)
        flow.closeWindow(SETTINGS(flow))

        flow.relaunch()
        flow.openSettings(flow.strings.settingsSyncAndStorage)
        assertTrue(
            flow.strings.settingsSyncPeriodOneHour in
                flow.dropdownIn(SETTINGS(flow), flow.strings.settingsDesktopRefreshWhileOpen).texts,
        )
        assertTrue(
            flow.strings.settingsAutoDeletePeriodOneWeek in
                flow.dropdownIn(SETTINGS(flow), flow.strings.settingsAutoDelete).texts,
        )
    }

    /**
     * Force English: desktop settings/GeneralPane.kt (settingsForceEnglish). The app starts in Italian;
     * Italian text comes from ItFeedFlowStrings.
     */
    @Test
    fun `REG-170 force english`() {
        val flow = e2e.start(Seed.ContentRich, language = "it")
        flow.awaitText(SEED_NEWEST_TITLE)
        flow.sidebarRow(ItFeedFlowStrings.drawerTitleTimeline)

        flow.openSettingsIn(ItFeedFlowStrings.settingsTitle, ItFeedFlowStrings.settingsAppTitle)
        assertFalse(flow.isSwitchOn(ItFeedFlowStrings.settingsTitle, ItFeedFlowStrings.settingsForceEnglish))
        flow.click(flow.switchIn(ItFeedFlowStrings.settingsTitle, ItFeedFlowStrings.settingsForceEnglish))
        flow.awaitWindow(flow.strings.settingsTitle)
        assertTrue(flow.isSwitchOn(flow.strings.settingsTitle, flow.strings.settingsForceEnglish))
        flow.closeWindow(flow.strings.settingsTitle)
        flow.sidebarRow(flow.strings.drawerTitleTimeline)

        flow.relaunch()
        flow.sidebarRow(flow.strings.drawerTitleTimeline)
        flow.openSettings(flow.strings.settingsAppTitle)
        flow.click(flow.switchIn(SETTINGS(flow), flow.strings.settingsForceEnglish))
        flow.awaitWindow(ItFeedFlowStrings.settingsTitle)
        assertFalse(flow.isSwitchOn(ItFeedFlowStrings.settingsTitle, ItFeedFlowStrings.settingsForceEnglish))
    }

    /**
     * Sidebar toggle: the article list toolbar's Open navigation menu button
     * (home/DesktopHomeScaffold.kt, toggleSidebar); the View menu shortcut is not reachable (FACT shortcuts).
     */
    @Test
    fun `REG-172 sidebar visibility persistence`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)
        flow.sidebarRow(flow.strings.drawerTitleTimeline)

        flow.click(flow.awaitContentDescription(flow.strings.drawerMenuButtonContentDescription))
        flow.awaitGone(flow.strings.drawerTitleTimeline)
        flow.relaunch()
        flow.awaitText(SEED_NEWEST_TITLE)
        assertNull(
            flow.nodes().firstOrNull { it.role == "Tab" && it.texts.firstOrNull() == flow.strings.drawerTitleTimeline },
        )

        flow.click(flow.awaitContentDescription(flow.strings.drawerMenuButtonContentDescription))
        flow.sidebarRow(flow.strings.drawerTitleTimeline)
        flow.relaunch()
        flow.sidebarRow(flow.strings.drawerTitleTimeline)
    }

    private fun Flow.clickSettingsButton(text: String) =
        click(scrollInWindow(strings.settingsTitle, "button '$text'") { it.role == "Button" && text in it.texts })

    private fun Flow.awaitDescriptionIn(description: String) =
        await("'$description' in Settings") {
            nodesIn(strings.settingsTitle).firstOrNull { description in it.contentDescriptions }
        }

    private fun Flow.dialogTitle(): String =
        await("a dialog over Settings") {
            windows().lastOrNull { it.isPopup && it.isShowing && it.title != strings.settingsTitle }?.title
        }

    private fun Flow.cancelDialog(title: String) {
        awaitWindow(title)
        clickIn(title, strings.cancelButton)
        awaitWindowGone(title)
    }

    /** Opens Settings on a pane when the UI is in Italian. */
    private fun Flow.openSettingsIn(settingsTitle: String, pane: String) {
        openAddMenuItem(ItFeedFlowStrings.importFeedButton, addButtonDescription = ItFeedFlowStrings.addFeed)
        awaitWindow(settingsTitle)
        clickIn(settingsTitle, pane)
    }

    private companion object {
        val SETTINGS: (Flow) -> String = { it.strings.settingsTitle }
        const val OLD_ARTICLE_TITLE = "E2E Old Article Eligible For Delete: Venezia council"
        const val OLD_ARTICLE_SEARCH = "Venezia council"
        const val SEARCH_FIELD_TAG = "search-query"
        const val ABOUT_TEXT_START = "FeedFlow is a minimal RSS reader"
    }
}
