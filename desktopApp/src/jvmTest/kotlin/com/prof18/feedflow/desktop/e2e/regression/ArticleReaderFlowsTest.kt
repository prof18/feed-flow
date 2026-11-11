package com.prof18.feedflow.desktop.e2e.regression

import com.prof18.feedflow.desktop.e2e.harness.DesktopE2eRule
import com.prof18.feedflow.desktop.e2e.harness.Flow
import com.prof18.feedflow.desktop.e2e.harness.SEED_AUDIO_URL
import com.prof18.feedflow.desktop.e2e.harness.SEED_BOOKMARKED_READ_TITLE
import com.prof18.feedflow.desktop.e2e.harness.SEED_BOOKMARKED_UNREAD_TITLE
import com.prof18.feedflow.desktop.e2e.harness.SEED_NEWEST_TITLE
import com.prof18.feedflow.desktop.e2e.harness.Seed
import com.prof18.feedflow.desktop.e2e.harness.articleRow
import com.prof18.feedflow.desktop.e2e.harness.awaitReaderTitle
import com.prof18.feedflow.desktop.e2e.harness.choose
import com.prof18.feedflow.desktop.e2e.harness.clickIn
import com.prof18.feedflow.desktop.e2e.harness.clickReaderButton
import com.prof18.feedflow.desktop.e2e.harness.clickSidebar
import com.prof18.feedflow.desktop.e2e.harness.closeWindow
import com.prof18.feedflow.desktop.e2e.harness.hasButton
import com.prof18.feedflow.desktop.e2e.harness.isArticleShown
import com.prof18.feedflow.desktop.e2e.harness.isOnScreen
import com.prof18.feedflow.desktop.e2e.harness.openArticle
import com.prof18.feedflow.desktop.e2e.harness.openSettings
import com.prof18.feedflow.desktop.e2e.harness.readerButton
import com.prof18.feedflow.desktop.e2e.harness.scrollToTag
import com.prof18.feedflow.desktop.e2e.harness.scrollToText
import com.prof18.feedflow.desktop.e2e.harness.swipe
import com.prof18.feedflow.desktop.e2e.harness.toggleReaderButton
import com.prof18.feedflow.desktop.e2e.harness.typeIntoFocused
import com.prof18.feedflow.desktop.e2e.harness.visibleArticleRows
import com.prof18.feedflow.desktop.reaadermode.READER_AUDIO_BANNER_TAG
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArticleReaderFlowsTest {

    @get:Rule
    val e2e = DesktopE2eRule()

    /**
     * Not covered: the article context menu (bookmark and mark read from the menu), which cannot be
     * opened over attach (DEFERRED under T10.8). The flow reaches the same states through the reader:
     * opening the article marks it read, and the reader toolbar bookmarks it.
     * reader bookmark: desktop reaadermode/ReaderModeScreen.desktop.kt (menuAddToBookmark)
     */
    @Test
    fun `REG-106 article context menu`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openArticle(NEWEST_ID)
        flow.awaitReaderTitle(SEED_NEWEST_TITLE)
        flow.toggleReaderButton(flow.strings.menuAddToBookmark, flow.strings.menuRemoveFromBookmark)
        flow.clickSidebar(flow.strings.drawerTitleBookmarks)
        flow.scrollToText(SEED_NEWEST_TITLE)
        flow.clickSidebar(flow.strings.drawerTitleRead)
        flow.scrollToText(SEED_NEWEST_TITLE)
    }

    /**
     * Desktop has no home "Sort & filter" menu; the order lives in Settings → Feed list → Sort by Date
     * (settings/FeedListPane.kt:230).
     *
     * Not covered: Mark all above / below, which are article context menu entries (DEFERRED under T10.8).
     */
    @Test
    fun `REG-109 feed order mark above below`() {
        val flow = e2e.start(Seed.OldestFirst)
        flow.await("the oldest article first") {
            flow.visibleArticleRows().firstOrNull()?.takeIf { OLD_ARTICLE_TITLE in it.texts }
        }

        flow.openSettings(flow.strings.settingsFeedListTitle)
        flow.choose(
            flow.strings.settingsTitle,
            flow.strings.settingsFeedOrderTitle,
            flow.strings.settingsFeedOrderNewestFirst,
        )
        flow.closeWindow(flow.strings.settingsTitle)
        flow.await("the newest article first") {
            flow.visibleArticleRows().firstOrNull()?.takeIf { SEED_NEWEST_TITLE in it.texts }
        }
    }

    /**
     * Not covered: the whole flow checks the article context menu's action set, which cannot be opened
     * over attach (DEFERRED under T10.8). The flow checks the article with a comments URL is listed.
     */
    @Test
    fun `REG-143 article context menu action set`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)
        flow.scrollToTag(flow.articleRow(COMMENTS_ID))
    }

    /**
     * search: desktop ui/components/DesktopSearchScreenContent.kt:283
     *
     * Not covered: the search result's context menu (DEFERRED under T10.8).
     */
    @Test
    fun `REG-152 search result context menu`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.search("Pixel")
        flow.awaitText(COMMENTS_TITLE)
        flow.awaitText(SEED_BOOKMARKED_UNREAD_TITLE)
    }

    /**
     * Not covered: Mark all below as read from the article context menu (DEFERRED under T10.8). The flow
     * checks the Bookmarks filter lists both bookmarked articles in the oldest-first order.
     */
    @Test
    fun `REG-168 mark read filter boundaries`() {
        val flow = e2e.start(Seed.OldestFirst)
        flow.awaitText(OLD_ARTICLE_TITLE)

        flow.clickSidebar(flow.strings.drawerTitleBookmarks)
        flow.awaitText(SEED_BOOKMARKED_READ_TITLE)
        flow.awaitText(SEED_BOOKMARKED_UNREAD_TITLE)
    }

    /**
     * Swipe rows: desktop ui/components/ArticleSwipeRow.kt (SwipeableActionsBox with the configured
     * left and right actions); the flow drags the row with the mouse.
     *
     * The mouse drag also clicks the row, which opens the article and marks it read, so the read-status
     * swipe cannot be told apart from the click. The bookmark swipe (right) proves the gesture and the
     * left-only and disabled settings.
     */
    @Test
    fun `REG-107 swipe actions`() {
        val flow = e2e.start(Seed.SwipeActions)
        flow.awaitText(SEED_NEWEST_TITLE)
        flow.swipeRow(NEWEST_ID, leftward = true)
        flow.clickSidebar(flow.strings.drawerTitleRead)
        flow.scrollToText(SEED_NEWEST_TITLE)
        flow.clickSidebar(flow.strings.drawerTitleTimeline)
        flow.swipeRow(IMAGE_ID, leftward = false)
        flow.clickSidebar(flow.strings.drawerTitleBookmarks)
        flow.scrollToText(IMAGE_TITLE)

        flow.restart(Seed.SwipeLeftOnly)
        flow.awaitText(SEED_NEWEST_TITLE)
        flow.swipeRow(IMAGE_ID, leftward = false)
        flow.clickSidebar(flow.strings.drawerTitleBookmarks)
        flow.awaitText(SEED_BOOKMARKED_UNREAD_TITLE)
        assertFalse(flow.isArticleShown(IMAGE_TITLE))

        flow.restart(Seed.SwipeDisabled)
        flow.awaitText(SEED_NEWEST_TITLE)
        flow.swipeRow(IMAGE_ID, leftward = false)
        flow.clickSidebar(flow.strings.drawerTitleBookmarks)
        flow.awaitText(SEED_BOOKMARKED_UNREAD_TITLE)
        assertFalse(flow.isArticleShown(IMAGE_TITLE))
    }

    // card layout: desktop ui/components/ArticleRow.kt (FeedLayout.CARD)
    @Test
    fun `REG-108 card layout`() {
        val flow = e2e.start(Seed.CardLayout)
        flow.checkSeededRows(showsDescription = true)
    }

    // big image layout: desktop ui/components/ArticleRow.kt (ArticleHero)
    @Test
    fun `REG-108 big image layout`() {
        val flow = e2e.start(Seed.BigImageLayout)
        flow.checkSeededRows(showsDescription = true)
    }

    /**
     * Grid layout: desktop ui/components/DesktopArticleList.kt:106 renders GRID as big image rows in the
     * two-pane layout (grid needs the single-pane list, isGridLayoutAllowed = !multiPane).
     */
    @Test
    fun `REG-108 grid layout`() {
        val flow = e2e.start(Seed.GridLayout)
        flow.awaitText(SEED_NEWEST_TITLE)
        flow.awaitText(SEED_BOOKMARKED_UNREAD_TITLE)
    }

    // compact list: the seed hides description, feed name, date and image (settings/FeedListPane.kt toggles)
    @Test
    fun `REG-108 compact list`() {
        val flow = e2e.start(Seed.CompactList)
        flow.awaitText(SEED_NEWEST_TITLE)
        flow.assertAbsent(NEWEST_DESCRIPTION)
        assertTrue(flow.visibleArticleRows().none { ANDROID_CENTRAL in it.texts })
        flow.scrollToText(NO_IMAGE_TITLE)
        flow.assertAbsent(NO_IMAGE_DESCRIPTION)
    }

    /**
     * Desktop removes a bookmark from the reader toolbar without the mobile confirmation (see SM-004).
     * empty Bookmarks and Back to timeline: desktop home/DesktopHomeScreenContent.kt:396-405
     *
     * Not covered: the context menu's Remove from bookmarks with its Cancel path (DEFERRED under T10.8).
     */
    @Test
    fun `REG-154 empty bookmarks back to timeline`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.clickSidebar(flow.strings.drawerTitleBookmarks)
        listOf(BOOKMARKED_UNREAD_ID to SEED_BOOKMARKED_UNREAD_TITLE, BOOKMARKED_READ_ID to SEED_BOOKMARKED_READ_TITLE)
            .forEach { (id, title) ->
                flow.openArticle(id)
                flow.awaitReaderTitle(title)
                flow.toggleReaderButton(flow.strings.menuRemoveFromBookmark, flow.strings.menuAddToBookmark)
                flow.clickReaderButton(flow.strings.closeButtonContentDescription)
            }
        flow.awaitText(flow.strings.bookmarkedArticlesEmptyScreenMessage)
        flow.click(flow.strings.emptyScreenBackToTimeline)
        flow.awaitText(SEED_NEWEST_TITLE)
    }

    /**
     * Desktop has no home "more" menu. The Bookmarks footer's Mark all as read
     * (ui/components/DesktopArticleList.kt:324) acts without the bookmarks-specific confirmation, which
     * belongs to the File menu entry (FACT shortcuts). The flow checks the footer marks the bookmarked
     * articles read and keeps them bookmarked.
     */
    @Test
    fun `REG-169 bookmarks mark all read confirmation`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.clickSidebar(flow.strings.drawerTitleBookmarks)
        flow.awaitText(SEED_BOOKMARKED_UNREAD_TITLE)
        flow.click(flow.scrollToText(flow.strings.markAllReadButton))
        flow.clickSidebar(flow.strings.drawerTitleRead)
        flow.scrollToText(SEED_BOOKMARKED_UNREAD_TITLE)
        flow.clickSidebar(flow.strings.drawerTitleBookmarks)
        flow.awaitText(SEED_BOOKMARKED_UNREAD_TITLE)
    }

    // fallback article, browser action, Text Settings: desktop reaadermode/ReaderModeScreen.desktop.kt
    @Test
    fun `REG-110 reader fallback`() {
        val flow = e2e.start(Seed.ReaderMode)
        flow.openArticle(FALLBACK_ID)
        flow.awaitReaderTitle(FALLBACK_TITLE)

        flow.clickReaderButton(flow.strings.readerModeBrowserButtonContentDescription)
        flow.awaitOpenedUri { it.startsWith("http") }
        flow.clickReaderButton(flow.strings.readerModeTextSettings)
        flow.awaitText(flow.strings.readerModeFontSize)
        flow.awaitText(flow.strings.readerModeLineHeight)
    }

    // external browser preference and per-feed override: desktop BrowserManager.kt (getArticleOpenMode)
    @Test
    fun `REG-112 link opening preferences`() {
        val flow = e2e.start(Seed.ExternalBrowser)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openArticle(NEWEST_ID)
        flow.awaitOpenedUri { it.contains("androidcentral.com") }
        val opened = flow.openedUris().size

        flow.openArticle(READER_SUCCESS_ID)
        flow.awaitReaderTitle(READER_SUCCESS_TITLE)
        assertTrue(flow.hasButton(flow.strings.readerModeBrowserButtonContentDescription))
        assertTrue(flow.openedUris().size == opened)
    }

    // previous/next and Text Settings: desktop reaadermode/ReaderTextSettings.kt (readerModeTitleFontSize,
    // readerModeLineHeight, readerModeResetToDefault, font radio buttons)
    @Test
    fun `REG-150 reader previous and font menu`() {
        val flow = e2e.start(Seed.ReaderMode)
        flow.openArticle(READER_SUCCESS_ID)
        flow.awaitReaderTitle(READER_SUCCESS_TITLE)

        flow.clickReaderButton(flow.strings.nextArticle)
        flow.awaitReaderTitle(FALLBACK_TITLE)
        flow.clickReaderButton(flow.strings.previousArticle)
        flow.awaitReaderTitle(READER_SUCCESS_TITLE)

        flow.clickReaderButton(flow.strings.readerModeTextSettings)
        flow.awaitText(flow.strings.readerModeLineHeight)
        flow.awaitText(flow.strings.readerModeTitleFontSize)
        flow.awaitText(flow.strings.readerModeResetToDefault)
        val font = flow.await("the IBM Plex Sans option") {
            flow.nodes().firstOrNull { it.isOnScreen && it.role == "RadioButton" && IBM_PLEX in it.texts }
        }
        flow.click(font)
        flow.await("IBM Plex Sans selected") {
            flow.nodes().firstOrNull { it.role == "RadioButton" && IBM_PLEX in it.texts && it.isSelected }
        }
    }

    /**
     * content source toggle: desktop reaadermode/ReaderModeScreen.desktop.kt (readerContentSourceFeed
     * radio in the reader toolbar)
     */
    @Test
    fun `REG-160 reader content source navigation`() {
        val flow = e2e.start(Seed.ReaderMode)
        flow.openArticle(READER_SUCCESS_ID)
        flow.awaitReaderTitle(READER_SUCCESS_TITLE)

        val source = { flow.readerButton(flow.strings.readerContentSourceFeed) }
        assertFalse(source().isSelected)
        flow.click(source())
        flow.await("feed content selected") { source().takeIf { it.isSelected } }
        flow.click(source())
        flow.await("web content selected") { source().takeIf { !it.isSelected } }
        flow.awaitReaderTitle(READER_SUCCESS_TITLE)
        flow.awaitText(READER_SUCCESS_PARAGRAPH)
        assertTrue(flow.nodes().first { it.testTag == flow.articleRow(READER_SUCCESS_ID) }.isSelected)
    }

    // URL-less article: desktop reaadermode/ReaderModeScreen.desktop.kt (no browser or source actions)
    @Test
    fun `REG-159 reader feed content no url`() {
        val flow = e2e.start(Seed.FeedContent)
        flow.openArticle(FEED_CONTENT_NO_URL_ID)
        flow.awaitReaderTitle(FEED_CONTENT_NO_URL_TITLE)
        flow.awaitText(ANDROID_CENTRAL)
        assertFalse(flow.hasButton(flow.strings.readerModeBrowserButtonContentDescription))
        assertFalse(flow.hasButton(flow.strings.readerContentSourceFeed))
    }

    // no content: desktop reaadermode/DesktopReaderContent.kt (ContentNotAvailable, readerModeNoContentTitle)
    @Test
    fun `REG-161 reader no url no content`() {
        val flow = e2e.start(Seed.FeedContent)
        flow.openArticle(NO_CONTENT_ID)
        flow.awaitText(flow.strings.readerModeNoContentTitle)
        assertFalse(flow.hasButton(flow.strings.readerModeBrowserButtonContentDescription))
    }

    /**
     * RTL rows: desktop ui/components/ArticleRow.kt (CompositionDirection). The row merges its title and
     * image into one node, so the flow cannot compare image and title bounds; it checks every RTL row
     * renders its title.
     */
    @Test
    fun `REG-163 rtl content direction`() {
        val flow = e2e.start(Seed.RtlContent)
        listOf(RTL_TITLE, LATIN_NEIGHBOUR_TITLE, RTL_LATIN_PREFIX_TITLE, RTL_NEUTRAL_TITLE).forEach {
            flow.scrollToText(it)
        }
    }

    // search results in the reader: desktop ui/components/DesktopSearchScreenContent.kt and reader navigation
    @Test
    fun `REG-171 search reader navigation`() {
        val flow = e2e.start(Seed.ReaderMode)
        flow.awaitTag("desktop_main_window_surface")

        flow.search("Reader")
        flow.awaitText(READER_SUCCESS_TITLE)
        flow.awaitText(FALLBACK_TITLE)
        flow.awaitText(READER_LONG_TITLE)
        flow.click(flow.awaitTag(flow.articleRow(READER_SUCCESS_ID)))
        flow.awaitReaderTitle(READER_SUCCESS_TITLE)
        assertTrue(flow.readerButton(flow.strings.previousArticle).isDisabled)
        assertFalse(flow.readerButton(flow.strings.nextArticle).isDisabled)
        flow.clickReaderButton(flow.strings.nextArticle)
        flow.awaitReaderTitle(FALLBACK_TITLE)
        assertFalse(flow.readerButton(flow.strings.previousArticle).isDisabled)
        flow.clickReaderButton(flow.strings.closeButtonContentDescription)
        flow.awaitText(READER_LONG_TITLE)
        assertTrue(flow.nodes().any { it.testTag == "search-query" && it.editableText == "Reader" })
    }

    /**
     * reader next and the list: desktop ui/components/DesktopArticleList.kt (revealArticleId)
     *
     * The large-content articles point at e2e.feedflow.local, which does not resolve, so the reader shows
     * "Unable to parse article"; the flow follows the selected row instead of the reader title.
     */
    @Test
    fun `REG-184 reader navigation list follow`() {
        val flow = e2e.start(Seed.LargeContent)
        flow.click(flow.awaitTag(flow.articleRow("e2e-large-article-001")))
        flow.awaitSelectedRow("e2e-large-article-001")
        repeat(NEXT_PRESSES) { index ->
            flow.clickReaderButton(flow.strings.nextArticle)
            flow.awaitSelectedRow("e2e-large-article-" + (index + 2).toString().padStart(3, '0'))
        }
        flow.clickReaderButton(flow.strings.closeButtonContentDescription)
        flow.await("article 011 in view") {
            flow.nodes().firstOrNull { it.testTag == flow.articleRow("e2e-large-article-011") && it.isOnScreen }
        }
        assertFalse(flow.isArticleShown("E2E Large Article 001"))
    }

    // audio banner: desktop reaadermode/AudioEpisodeBanner.desktop.kt:30 (READER_AUDIO_BANNER_TAG)
    @Test
    fun `REG-173 podcast audio banner`() {
        val flow = e2e.start(Seed.AudioEpisode)
        flow.openArticle(AUDIO_ID)
        flow.awaitReaderTitle(AUDIO_TITLE)
        flow.click(flow.awaitTag(READER_AUDIO_BANNER_TAG))
        flow.awaitOpenedUri { it == SEED_AUDIO_URL }

        flow.clickReaderButton(flow.strings.nextArticle)
        flow.awaitReaderTitle(AUDIO_CONTROL_TITLE)
        flow.awaitTagGone(READER_AUDIO_BANNER_TAG)
    }

    // report action and disclosure: desktop reaadermode/ArticleReportDisclosure.kt (articleReportAction,
    // articleReportSend, articleReportSent, articleReportFailed)
    @Test
    fun `REG-180 reader report`() {
        val flow = e2e.start(Seed.report("accepted"))
        flow.openReport()
        flow.clickIn(flow.strings.articleReportAction, flow.strings.cancelButton)
        flow.awaitWindowGone(flow.strings.articleReportAction)
        flow.awaitReaderTitle(REPORT_TITLE)

        flow.openReport()
        flow.clickIn(flow.strings.articleReportAction, flow.strings.articleReportSend)
        flow.awaitText(flow.strings.articleReportSent, timeoutMs = LONG_WAIT_MS)

        flow.restart(Seed.report("offline"))
        flow.openReport()
        flow.clickIn(flow.strings.articleReportAction, flow.strings.articleReportSend)
        flow.awaitText(flow.strings.articleReportFailed, timeoutMs = LONG_WAIT_MS)
    }

    /** Drags an article row horizontally with the mouse, the desktop counterpart of a swipe. */
    private fun Flow.swipeRow(id: String, leftward: Boolean) {
        val row = scrollToTag(articleRow(id))
        val y = row.bounds.y + row.bounds.height / 2
        val left = row.bounds.x + SWIPE_INSET
        val right = row.bounds.x + row.bounds.width - SWIPE_INSET
        if (leftward) swipe(right, y, left, y) else swipe(left, y, right, y)
    }

    private fun Flow.awaitSelectedRow(id: String) {
        await("row '$id' selected") { nodes().firstOrNull { it.testTag == articleRow(id) && it.isSelected } }
    }

    private fun Flow.openReport() {
        openArticle(REPORT_ID)
        awaitReaderTitle(REPORT_TITLE)
        click(readerButton(strings.moreOptionsButtonContentDescription))
        click(strings.articleReportAction)
        awaitWindow(strings.articleReportAction)
    }

    private fun Flow.search(query: String) {
        click(awaitContentDescription(strings.searchButtonContentDescription))
        awaitTag("search-query")
        typeIntoFocused(query)
    }

    private fun Flow.checkSeededRows(showsDescription: Boolean) {
        awaitText(SEED_NEWEST_TITLE)
        if (showsDescription) awaitText(NEWEST_DESCRIPTION)
        awaitText(ANDROID_CENTRAL)
        scrollToText(NO_IMAGE_TITLE)
        if (showsDescription) scrollToText(NO_IMAGE_DESCRIPTION)
    }

    private companion object {
        const val LONG_WAIT_MS = 30_000L
        const val SWIPE_INSET = 20
        const val NEXT_PRESSES = 10
        const val ANDROID_CENTRAL = "Android Central RSS Feed"
        const val NEWEST_ID = "e2e-article-newest-unread"
        const val IMAGE_ID = "e2e-article-image"
        const val IMAGE_TITLE = "E2E Article With Image: Delivery Hero stake"
        const val NEWEST_DESCRIPTION = "A realistic seeded item using Android Central image metadata"
        const val BOOKMARKED_UNREAD_ID = "e2e-article-bookmarked-unread"
        const val BOOKMARKED_READ_ID = "e2e-article-bookmarked-read"
        const val COMMENTS_ID = "e2e-article-comments"
        const val COMMENTS_TITLE = "E2E Article With Comments: Pixel Glow"
        const val NO_IMAGE_TITLE = "E2E Article Without Image: DeepSeek pricing"
        const val NO_IMAGE_DESCRIPTION = "Seeded item without media"
        const val OLD_ARTICLE_TITLE = "E2E Old Article Eligible For Delete: Venezia council"
        const val READER_SUCCESS_ID = "e2e-article-reader-success"
        const val READER_SUCCESS_PARAGRAPH = "E2E cached reader content loaded from the seed fixture."
        const val READER_SUCCESS_TITLE = "E2E Reader Mode Success Article: Interstitium"
        const val FALLBACK_ID = "e2e-article-reader-fallback"
        const val FALLBACK_TITLE = "E2E Reader Mode Fallback Article: The Witcher 3"
        const val READER_LONG_TITLE = "E2E Reader Mode Long Article: Scroll Retention"
        const val FEED_CONTENT_NO_URL_ID = "e2e-article-feed-content-no-url"
        const val FEED_CONTENT_NO_URL_TITLE = "E2E Feed Content Article Without Link"
        const val NO_CONTENT_ID = "e2e-article-no-content-no-url"
        const val RTL_TITLE = "خبر فوری درباره تاسیسات نفتی"
        const val LATIN_NEIGHBOUR_TITLE = "E2E Latin Neighbour Article"
        const val RTL_LATIN_PREFIX_TITLE = "BBC گزارش تازه از تهران"
        const val RTL_NEUTRAL_TITLE = "2026 (#1) — 12:45"
        const val AUDIO_ID = "e2e-audio-episode"
        const val AUDIO_TITLE = "E2E Audio Episode: Language Notes"
        const val AUDIO_CONTROL_TITLE = "E2E Audio Control Article"
        const val REPORT_ID = "e2e-article-report"
        const val REPORT_TITLE = "E2E Report Article"
        const val IBM_PLEX = "IBM Plex Sans"
    }
}
