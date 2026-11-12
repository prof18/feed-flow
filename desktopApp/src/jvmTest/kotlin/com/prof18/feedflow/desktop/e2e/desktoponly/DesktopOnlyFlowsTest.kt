package com.prof18.feedflow.desktop.e2e.desktoponly

import com.prof18.feedflow.desktop.e2e.harness.DesktopE2eRule
import com.prof18.feedflow.desktop.e2e.harness.FIXTURE_FEED_PATH
import com.prof18.feedflow.desktop.e2e.harness.SEED_NEWEST_TITLE
import com.prof18.feedflow.desktop.e2e.harness.Seed
import com.prof18.feedflow.desktop.e2e.harness.awaitReaderTitle
import com.prof18.feedflow.desktop.e2e.harness.awaitTagIn
import com.prof18.feedflow.desktop.e2e.harness.awaitTextIn
import com.prof18.feedflow.desktop.e2e.harness.clickSidebar
import com.prof18.feedflow.desktop.e2e.harness.isOnScreen
import com.prof18.feedflow.desktop.e2e.harness.nodesIn
import com.prof18.feedflow.desktop.e2e.harness.openArticle
import com.prof18.feedflow.desktop.e2e.harness.openSettings
import com.prof18.feedflow.desktop.e2e.harness.scrollInWindow
import com.prof18.feedflow.desktop.e2e.harness.sidebarRow
import com.prof18.feedflow.desktop.e2e.harness.visibleArticleRows
import com.prof18.feedflow.desktop.testing.FeedFormE2eIds
import org.junit.Rule
import org.junit.Test
import java.awt.event.KeyEvent
import java.net.URLEncoder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopOnlyFlowsTest {

    @get:Rule
    val e2e = DesktopE2eRule()

    /**
     * settings search field and results: desktop settings/search/SettingsSearchSidebar.kt:52-159
     * (SETTINGS_SEARCH_FIELD_TAG, SETTINGS_SEARCH_RESULTS_TAG, SETTINGS_SEARCH_CLEAR_TAG); the reportIssueButton
     * row is indexed in settings/search/SettingsSearchIndex.kt.
     *
     * While a query is active the sidebar lists results instead of panes, so the flow checks the About pane
     * by its heading. The row highlight (SettingsSearchHighlight.kt) is a timed tint with no semantics, so it
     * is not asserted.
     */
    @Test
    fun `DSK-001 settings search`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openSettings(flow.strings.settingsAppTitle)
        val settings = flow.strings.settingsTitle
        flow.typeInto(flow.awaitTagIn(settings, SEARCH_FIELD), flow.strings.reportIssueButton)
        val results = flow.awaitTagIn(settings, SEARCH_RESULTS)
        val result = flow.await("the Report issue result") {
            flow.nodesIn(settings).firstOrNull { node ->
                node.isOnScreen && node.role == "Button" && flow.strings.reportIssueButton in node.texts &&
                    node.bounds.x < results.bounds.x + results.bounds.width
            }
        }
        flow.click(result)
        flow.awaitTextIn(settings, flow.strings.settingsAboutAndSupport)
        flow.scrollInWindow(settings, "the Report issue row") {
            it.role == "Button" && flow.strings.reportIssueButton in it.texts
        }
        flow.click(flow.awaitTagIn(settings, SEARCH_CLEAR))
        flow.await("the query to clear") {
            flow.awaitTagIn(settings, SEARCH_FIELD).takeIf { it.editableText.isNullOrEmpty() }
        }
    }

    /**
     * The menu-bar shortcuts (Add feed, next and previous article, open in browser, sidebar) live in the
     * native macOS menu bar, which Spectre's synthetic keys do not reach (FACT shortcuts). All steps are
     * DEFERRED under T10.8; the flow checks the reader's in-window Next and Previous buttons, which back
     * the same Article menu actions.
     */
    @Test
    fun `DSK-002 menu-bar shortcuts`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openArticle(NEWEST_ID)
        flow.awaitReaderTitle(SEED_NEWEST_TITLE)
        flow.click(flow.awaitContentDescription(flow.strings.nextArticle))
        flow.await("another article in the reader") {
            flow.nodes().firstOrNull { it.isSelected && it.testTag?.startsWith("article_row_") == true }
                ?.takeIf { SEED_NEWEST_TITLE !in it.texts }
        }
    }

    /**
     * Feed tree keyboard: desktop home/drawer/DesktopFeedTreeKeyboard.kt (Down and Up move focus only).
     * Activation is the row's own key handling: Enter on a focused, clickable row.
     */
    @Test
    fun `DSK-003 feed tree keyboard navigation`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.clickSidebar(ANDROID_CENTRAL)
        val rows = flow.treeRows()
        val start = rows.indexOf(ANDROID_CENTRAL)
        flow.key(KeyEvent.VK_DOWN)
        flow.awaitFocusedRow(rows[start + 1])
        flow.key(KeyEvent.VK_DOWN)
        flow.awaitFocusedRow(rows[start + 2])
        flow.key(KeyEvent.VK_UP)
        val target = rows[start + 1]
        flow.awaitFocusedRow(target)
        assertTrue(flow.sidebarRow(ANDROID_CENTRAL).isSelected)

        flow.key(KeyEvent.VK_ENTER)
        flow.await("'$target' selected") { flow.sidebarRow(target).takeIf { it.isSelected } }
        flow.await("the articles of '$target'") {
            flow.visibleArticleRows().takeIf { rows -> rows.isNotEmpty() && rows.all { target in it.texts } }
        }
    }

    // second-instance activation: desktop startup/InstanceActivation.kt and startup/AddFeedLinkInbox.kt
    @Test
    fun `DSK-004 second-instance add-feed link`() {
        val flow = e2e.start(Seed.Empty)
        flow.awaitText(flow.strings.emptyFeedMessage)

        val feedUrl = flow.fixtureUrl(FIXTURE_FEED_PATH)
        val exit = flow.launchSecondInstance(listOf("feedflow://add?url=" + URLEncoder.encode(feedUrl, Charsets.UTF_8)))
        assertEquals(0, exit)
        flow.awaitWindow(flow.strings.addFeed)
        flow.await("the pre-filled feed URL") {
            flow.nodesIn(flow.strings.addFeed).firstOrNull {
                it.testTag == FeedFormE2eIds.ADD_FEED_URL_INPUT && it.editableText == feedUrl
            }
        }
    }

    // report issue: desktop settings/AboutPane.kt:74 (reportIssueButton, openDesktopMailSafely)
    @Test
    fun `DSK-005 report issue mail`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openSettings(flow.strings.settingsAboutAndSupport)
        flow.click(
            flow.scrollInWindow(flow.strings.settingsTitle, "the Report issue button") {
                it.role == "Button" && flow.strings.reportIssueButton in it.texts
            },
        )
        flow.awaitOpenedUri { it.startsWith("mailto:") }
    }

    /**
     * Markdown reader: desktop reaadermode/DesktopReaderContent.kt (buildReaderModeMarkdown) and
     * DesktopReaderMarkdownRenderer.kt. The reader-mode seed stores the article as Markdown on desktop,
     * the format the desktop reader caches, so the paragraph must render as text and not as source.
     * Jewel's Markdown renderer exposes an inline image to accessibility by its Markdown source, so the
     * image is found by its alt text in a node of its own.
     */
    @Test
    fun `DSK-006 Markdown reader rendering`() {
        val flow = e2e.start(Seed.ReaderMode)

        flow.openArticle(READER_SUCCESS_ID)
        flow.awaitReaderTitle(READER_SUCCESS_TITLE)
        val paragraph = flow.awaitText(READER_PARAGRAPH)
        val image = flow.await("the inline image") {
            flow.nodes().firstOrNull { node -> node.texts.any { it.trim().startsWith("![$READER_IMAGE_ALT](") } }
        }
        assertTrue(image.key != paragraph.key)
        assertTrue(flow.nodes().none { node -> node.texts.any { "<article>" in it || "<img" in it } })
    }

    /**
     * Empty category: desktop home/DesktopHomeScreenContent.kt (EmptyTimeline, isCategoryWithoutFeeds) and
     * home/drawer/DesktopDrawerFeedSourcesByCategories.kt (showDisclosure). The content-rich seed has a
     * category with no feeds.
     */
    @Test
    fun `DSK-007 empty category state`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.awaitText(flow.strings.emptyCategoryHint)
        flow.clickSidebar(EMPTY_CATEGORY)
        flow.awaitText(flow.strings.emptyCategoryMessage)
        flow.awaitText(flow.strings.addFeed)
        flow.assertAbsent(flow.strings.refreshFeeds)
        val disclosures = setOf(
            flow.strings.desktopCollapseFeedCategory(EMPTY_CATEGORY),
            flow.strings.desktopExpandFeedCategory(EMPTY_CATEGORY),
        )
        assertTrue(flow.nodes().none { node -> node.contentDescriptions.any { it in disclosures } })
    }

    private fun com.prof18.feedflow.desktop.e2e.harness.Flow.treeRows(): List<String> =
        nodes().filter { it.role == "Tab" && it.key.startsWith("window:1:0:") && it.isOnScreen }
            .sortedBy { it.bounds.y }
            .map { it.texts.first() }

    private fun com.prof18.feedflow.desktop.e2e.harness.Flow.awaitFocusedRow(name: String) {
        await("sidebar row '$name' focused") {
            nodes().firstOrNull { it.role == "Tab" && it.texts.firstOrNull() == name && it.isFocused }
        }
    }

    private companion object {
        const val SEARCH_FIELD = "settings-search-field"
        const val SEARCH_RESULTS = "settings-search-results"
        const val SEARCH_CLEAR = "settings-search-clear"
        const val NEWEST_ID = "e2e-article-newest-unread"
        const val ANDROID_CENTRAL = "Android Central RSS Feed"
        const val EMPTY_CATEGORY = "Empty Category"
        const val READER_SUCCESS_ID = "e2e-article-reader-success"
        const val READER_SUCCESS_TITLE = "E2E Reader Mode Success Article: Interstitium"
        const val READER_PARAGRAPH = "E2E cached reader content loaded from the seed fixture."
        const val READER_IMAGE_ALT = "E2E reader image"
    }
}
