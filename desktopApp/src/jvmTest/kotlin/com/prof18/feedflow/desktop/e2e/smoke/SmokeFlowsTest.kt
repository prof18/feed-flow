package com.prof18.feedflow.desktop.e2e.smoke

import com.prof18.feedflow.desktop.e2e.harness.DesktopE2eRule
import com.prof18.feedflow.desktop.e2e.harness.SEED_BOOKMARKED_READ_TITLE
import com.prof18.feedflow.desktop.e2e.harness.SEED_BOOKMARKED_UNREAD_TITLE
import com.prof18.feedflow.desktop.e2e.harness.SEED_NEWEST_TITLE
import com.prof18.feedflow.desktop.e2e.harness.SEED_READ_TITLE
import com.prof18.feedflow.desktop.e2e.harness.Seed
import com.prof18.feedflow.desktop.e2e.harness.awaitReaderTitle
import com.prof18.feedflow.desktop.e2e.harness.awaitTagIn
import com.prof18.feedflow.desktop.e2e.harness.awaitTextIn
import com.prof18.feedflow.desktop.e2e.harness.choose
import com.prof18.feedflow.desktop.e2e.harness.clickIn
import com.prof18.feedflow.desktop.e2e.harness.clickMenuItem
import com.prof18.feedflow.desktop.e2e.harness.clickReaderButton
import com.prof18.feedflow.desktop.e2e.harness.clickSidebar
import com.prof18.feedflow.desktop.e2e.harness.closeWindow
import com.prof18.feedflow.desktop.e2e.harness.dropdownIn
import com.prof18.feedflow.desktop.e2e.harness.hasButton
import com.prof18.feedflow.desktop.e2e.harness.isArticleShown
import com.prof18.feedflow.desktop.e2e.harness.isSwitchOn
import com.prof18.feedflow.desktop.e2e.harness.openAddMenuItem
import com.prof18.feedflow.desktop.e2e.harness.openArticle
import com.prof18.feedflow.desktop.e2e.harness.openSettings
import com.prof18.feedflow.desktop.e2e.harness.openSidebarMenu
import com.prof18.feedflow.desktop.e2e.harness.replaceText
import com.prof18.feedflow.desktop.e2e.harness.scrollToText
import com.prof18.feedflow.desktop.e2e.harness.setSwitch
import com.prof18.feedflow.desktop.e2e.harness.sidebarCount
import com.prof18.feedflow.desktop.e2e.harness.sidebarRow
import com.prof18.feedflow.desktop.e2e.harness.switchIn
import com.prof18.feedflow.desktop.e2e.harness.toggleReaderButton
import com.prof18.feedflow.desktop.e2e.harness.typeIntoFocused
import com.prof18.feedflow.desktop.e2e.harness.visibleArticleRows
import com.prof18.feedflow.desktop.settings.blocked.BlockedWordsE2eIds
import com.prof18.feedflow.desktop.testing.FeedFormE2eIds
import com.prof18.feedflow.desktop.testing.ImportExportE2eIds
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmokeFlowsTest {

    @get:Rule
    val e2e = DesktopE2eRule()

    /**
     * empty-state message: desktop home/DesktopHomeScreenContent.kt:400 (emptyFeedMessage)
     *
     * The NoFeedsDialog does not open on its own: its only trigger, onEmptyStateClick, is passed down
     * to DesktopHomeScreenContent but never called, so the flow asserts the empty message only.
     */
    @Test
    fun `SM-001 first launch empty`() {
        val flow = e2e.start(Seed.Empty)

        flow.awaitText(flow.strings.emptyFeedMessage)
        flow.assertAbsent(flow.strings.noFeedModalTitle)
    }

    // newest article first: desktop ui/components/ArticleRow.kt:174 (seeded rows, article_row_ tag)
    // Timeline unread count: desktop ui/components/SidebarList.kt (count text in the Timeline row)
    // hidden feed article absent: seeded data, rendered by ArticleRow
    @Test
    fun `SM-002 seeded timeline loads`() {
        val flow = e2e.start(Seed.ContentRich)

        flow.awaitText(SEED_NEWEST_TITLE)
        assertTrue(SEED_NEWEST_TITLE in flow.visibleArticleRows().first().texts)
        assertTrue(flow.sidebarCount(flow.strings.drawerTitleTimeline)?.toIntOrNull()?.let { it > 0 } == true)
        flow.assertAbsent(HIDDEN_FEED_ARTICLE)
    }

    // Timeline, Read and Bookmarks rows: desktop home/drawer/DesktopDrawer.kt (drawerTitle* strings)
    // Bookmarks shows no count: desktop home/drawer/DesktopDrawer.kt (no count passed for Bookmarks)
    // category collapse and expand: desktop home/drawer/DesktopDrawerFeedSourcesByCategories.kt
    //   (collapse/expandCategoryButtonContentDescription)
    // feed and category filters: desktop home/drawer/DesktopFeedSourceDrawerItem.kt
    @Test
    fun `SM-003 library filters`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        assertNull(flow.sidebarCount(flow.strings.drawerTitleBookmarks))

        flow.clickSidebar(flow.strings.drawerTitleRead)
        flow.awaitText(SEED_READ_TITLE)
        flow.assertAbsent(SEED_NEWEST_TITLE)

        flow.clickSidebar(flow.strings.drawerTitleBookmarks)
        flow.awaitText(SEED_BOOKMARKED_UNREAD_TITLE)
        flow.awaitText(SEED_BOOKMARKED_READ_TITLE)

        flow.click(flow.awaitContentDescription(flow.strings.collapseCategoryButtonContentDescription(TECHNOLOGY)))
        flow.awaitGone(ANDROID_CENTRAL_FEED)
        flow.click(flow.awaitContentDescription(flow.strings.expandCategoryButtonContentDescription(TECHNOLOGY)))
        flow.clickSidebar(ANDROID_CENTRAL_FEED)
        flow.awaitText(SEED_NEWEST_TITLE)
        assertFalse(flow.isArticleShown(SEED_BOOKMARKED_UNREAD_TITLE))

        flow.clickSidebar(NEWS)
        flow.awaitText(ARTICLE_WITH_IMAGE)

        flow.clickSidebar(UNCATEGORIZED)
        flow.awaitText(ARTICLE_WITHOUT_IMAGE)
    }

    /**
     * reader bookmark button: desktop reaadermode/ReaderModeScreen.desktop.kt (menuAddToBookmark /
     * menuRemoveFromBookmark content descriptions)
     *
     * Not on desktop: the remove-bookmark confirmation (removeBookmarkDialogMessage has no desktop
     * caller), so removing from the reader toolbar takes effect at once. Skipped: the reader back
     * button (the desktop reader is a pane next to the list).
     */
    @Test
    fun `SM-004 article read and bookmark state`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openArticle(READER_SUCCESS_ID)
        flow.awaitReaderTitle(READER_SUCCESS_TITLE)
        flow.toggleReaderButton(flow.strings.menuAddToBookmark, flow.strings.menuRemoveFromBookmark)
        flow.toggleReaderButton(flow.strings.menuRemoveFromBookmark, flow.strings.menuAddToBookmark)
        flow.toggleReaderButton(flow.strings.menuAddToBookmark, flow.strings.menuRemoveFromBookmark)

        flow.clickSidebar(flow.strings.drawerTitleBookmarks)
        flow.scrollToText(READER_SUCCESS_TITLE)
        flow.clickSidebar(flow.strings.drawerTitleRead)
        flow.scrollToText(READER_SUCCESS_TITLE)
    }

    /**
     * Mark all as read: desktop ui/components/DesktopArticleList.kt:324 (footer button)
     *
     * The footer button marks everything read without the confirmation dialog; on desktop the
     * confirmation (markAllReadDialogMessage, MainWindow.kt:480) belongs to the File menu entry,
     * which Spectre cannot reach (FACT shortcuts).
     */
    @Test
    fun `SM-005 mark all read`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.click(flow.scrollToText(flow.strings.markAllReadButton))
        flow.awaitText(flow.strings.emptyFeedMessage)

        flow.clickSidebar(flow.strings.drawerTitleRead)
        flow.scrollToText(SEED_NEWEST_TITLE)
    }

    // search button: desktop home toolbar (searchButtonContentDescription)
    // query field: desktop ui/components/DesktopSearchScreenContent.kt:283 (search-query)
    // Bookmarks filter: desktop ui/components/DesktopSearchScreenContent.kt (searchFilterBookmarks)
    @Test
    fun `SM-006 search core`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.click(flow.awaitContentDescription(flow.strings.searchButtonContentDescription))
        flow.awaitTag(SEARCH_FIELD_TAG)
        flow.typeIntoFocused("Pixel")
        flow.awaitText(SEED_BOOKMARKED_UNREAD_TITLE)
        flow.awaitText(ARTICLE_WITH_COMMENTS)

        flow.click(
            flow.await("Bookmarks search filter") {
                flow.nodes().firstOrNull { it.role == "RadioButton" && flow.strings.searchFilterBookmarks in it.texts }
            },
        )
        flow.awaitText(SEED_BOOKMARKED_UNREAD_TITLE)
        flow.awaitGone(ARTICLE_WITH_COMMENTS)
    }

    /**
     * reader article and browser action: desktop reaadermode/ReaderModeScreen.desktop.kt
     * (readerModeBrowserButtonContentDescription, nextArticle)
     * Text Settings: desktop reaadermode/ReaderTextSettings.kt (readerModeFontSize, readerModeLineHeight)
     *
     * Not on desktop: Listen to article (readerModeTtsListen has no desktop caller). Skipped: the
     * toolbar long-press tooltip (an Android gesture).
     */
    @Test
    fun `SM-007 reader mode core`() {
        val flow = e2e.start(Seed.ReaderMode)

        flow.openArticle(READER_SUCCESS_ID)
        flow.awaitReaderTitle(READER_SUCCESS_TITLE)
        assertTrue(flow.hasButton(flow.strings.readerModeBrowserButtonContentDescription))

        flow.clickReaderButton(flow.strings.nextArticle)
        flow.awaitReaderTitle(READER_FALLBACK_TITLE)

        flow.toggleReaderButton(flow.strings.menuAddToBookmark, flow.strings.menuRemoveFromBookmark)

        flow.clickReaderButton(flow.strings.readerModeTextSettings)
        flow.awaitText(flow.strings.readerModeFontSize)
        flow.awaitText(flow.strings.readerModeLineHeight)
    }

    /**
     * Feed settings entry: desktop home/drawer/DesktopFeedSourceDrawerItem.kt:249 (openFeedSettings in the
     * sidebar feed menu, opened with Shift+F10)
     * name, hide, keep unread, pin, category and save: desktop feedforms/FeedForms.kt (feedName,
     * hideFeedFromTimelineDescription, keepUnreadWhenScrollingForFeed, pinFeedSourceDescription,
     * addFeedCategoryTitle, actionSave)
     */
    @Test
    fun `SM-008 feed edit core`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openSidebarMenu(ANDROID_CENTRAL_FEED)
        flow.clickMenuItem(flow.strings.openFeedSettings)
        val window = flow.strings.openFeedSettings
        flow.awaitWindow(window)
        flow.replaceText(flow.awaitTagIn(window, FeedFormE2eIds.NAME_INPUT), RENAMED_FEED)
        flow.setSwitch(window, flow.strings.hideFeedFromTimelineDescription, on = true)
        flow.setSwitch(window, flow.strings.keepUnreadWhenScrollingForFeed, on = true)
        flow.awaitTextIn(window, flow.strings.keepUnreadWhenScrollingForFeedDescription)
        flow.setSwitch(window, flow.strings.pinFeedSourceDescription, on = true)
        flow.choose(window, flow.strings.addFeedCategoryTitle, NEWS)
        flow.clickIn(window, flow.strings.actionSave)
        flow.awaitWindowGone(window)

        flow.sidebarRow(RENAMED_FEED)
        flow.clickSidebar(flow.strings.drawerTitleTimeline)
        flow.awaitGone(SEED_NEWEST_TITLE)
    }

    /**
     * layout, hide image, order, date format: desktop settings/FeedListPane.kt:97-206
     * (feedLayoutTitle, settingsHideImages, settingsFeedOrderTitle, dateFormatTitle)
     *
     * Article images sit inside the clickable row, whose merged semantics drop the image test tags, so
     * the flow checks the persisted "Hide article image" switch instead of the missing image.
     */
    @Test
    fun `SM-009 feed list settings persist`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        val settings = flow.strings.settingsTitle
        flow.openSettings(flow.strings.settingsFeedListTitle)
        flow.choose(settings, flow.strings.feedLayoutTitle, flow.strings.settingsFeedLayoutCard)
        flow.setSwitch(settings, flow.strings.settingsHideImages, on = true)
        flow.choose(settings, flow.strings.dateFormatTitle, flow.strings.dateFormatDayMonthDot)
        flow.choose(settings, flow.strings.settingsFeedOrderTitle, flow.strings.settingsFeedOrderOldestFirst)
        flow.closeWindow(settings)

        repeat(2) { round ->
            if (round == 1) flow.relaunch()
            val firstRow = flow.await("the oldest article first") {
                flow.visibleArticleRows().firstOrNull()?.takeIf { OLD_ARTICLE_TITLE in it.texts }
            }
            assertTrue(firstRow.texts.any { DOTTED_DATE.matches(it) }, "dotted date in ${firstRow.texts}")
        }
        flow.openSettings(flow.strings.settingsFeedListTitle)
        assertTrue(flow.isSwitchOn(settings, flow.strings.settingsHideImages))
        assertTrue(flow.strings.settingsFeedLayoutCard in flow.dropdownIn(settings, flow.strings.feedLayoutTitle).texts)
    }

    // open mode, mark read when scrolling: desktop settings/ReadingPane.kt:50,75 (articleOpenMode,
    // toggleMarkReadWhenScrolling); show read and auto-hide: desktop settings/FeedListPane.kt:264-274
    // (settingsToggleShowReadArticles, settingsHideReadItems, settingsHideReadItemsDisabledWhileScrolling)
    @Test
    fun `SM-010 reading behavior settings persist`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        val settings = flow.strings.settingsTitle
        flow.openSettings(flow.strings.settingsReadingBehavior)
        flow.choose(settings, flow.strings.articleOpenMode, flow.strings.readerContentSourceFeed)
        flow.setSwitch(settings, flow.strings.toggleMarkReadWhenScrolling, on = false)
        flow.clickIn(settings, flow.strings.settingsFeedListTitle)
        flow.setSwitch(settings, flow.strings.settingsToggleShowReadArticles, on = true)
        assertFalse(flow.switchIn(settings, flow.strings.settingsHideReadItems).isDisabled)
        flow.clickIn(settings, flow.strings.settingsReadingBehavior)
        flow.setSwitch(settings, flow.strings.toggleMarkReadWhenScrolling, on = true)
        flow.clickIn(settings, flow.strings.settingsFeedListTitle)
        assertTrue(flow.switchIn(settings, flow.strings.settingsHideReadItems).isDisabled)
        flow.closeWindow(settings)

        flow.scrollToText(SEED_READ_TITLE)
        flow.relaunch()
        flow.scrollToText(SEED_READ_TITLE)

        flow.openSettings(flow.strings.settingsReadingBehavior)
        val openMode = flow.dropdownIn(settings, flow.strings.articleOpenMode)
        assertTrue(flow.strings.readerContentSourceFeed in openMode.texts)
        flow.clickIn(settings, flow.strings.settingsFeedListTitle)
        flow.setSwitch(settings, flow.strings.settingsToggleShowReadArticles, on = false)
        flow.closeWindow(settings)
        flow.awaitGone(SEED_READ_TITLE)

        flow.openArticle(READER_SUCCESS_ID)
        flow.awaitReaderTitle(READER_SUCCESS_TITLE)
    }

    /**
     * import entries and success states: desktop importexport/ImportExportContent.kt (ImportExportE2eIds)
     *
     * The file pickers answer through the E2E file-dialog hook instead of the system picker.
     */
    @Test
    fun `SM-011 import export smoke`() {
        val flow = e2e.start(Seed.Empty)
        flow.awaitText(flow.strings.emptyFeedMessage)

        flow.openAddMenuItem(flow.strings.importFeedButton)
        val settings = flow.strings.settingsTitle
        flow.awaitWindow(settings)
        flow.respondToFileDialog(flow.fixtureOpml(VALID_OPML))
        flow.click(flow.awaitTagIn(settings, ImportExportE2eIds.IMPORT_FEEDS))
        flow.awaitTagIn(settings, ImportExportE2eIds.FEED_IMPORT_SUCCESS)
        flow.click(flow.awaitTagIn(settings, ImportExportE2eIds.DONE))

        flow.respondToFileDialog(File(flow.repoRoot, ARTICLES_CSV))
        flow.click(flow.awaitTagIn(settings, ImportExportE2eIds.IMPORT_ARTICLES))
        flow.awaitTagIn(settings, ImportExportE2eIds.ARTICLE_IMPORT_SUCCESS)
        flow.click(flow.awaitTagIn(settings, ImportExportE2eIds.DONE))
    }

    // Filters pane: desktop settings/blocked/BlockedWordsScreen.desktop.kt (BlockedWordsE2eIds)
    // search: desktop ui/components/DesktopSearchScreenContent.kt:283
    @Test
    fun `SM-012 blocked words`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openSettings(flow.strings.settingsFilters)
        flow.typeInto(flow.awaitTag(BlockedWordsE2eIds.INPUT), "Pixel")
        flow.clickTag(BlockedWordsE2eIds.ADD_BUTTON)
        flow.awaitTag(BlockedWordsE2eIds.row("Pixel"))
        flow.closeWindow(flow.strings.settingsTitle)

        flow.awaitGone(SEED_BOOKMARKED_UNREAD_TITLE)
        flow.click(flow.awaitContentDescription(flow.strings.searchButtonContentDescription))
        flow.awaitTag(SEARCH_FIELD_TAG)
        flow.typeIntoFocused("Pixel")
        flow.awaitText(flow.strings.searchNoData("Pixel"))
        flow.assertAbsent(SEED_BOOKMARKED_UNREAD_TITLE)
        flow.assertAbsent(ARTICLE_WITH_COMMENTS)

        flow.openSettings(flow.strings.settingsFilters)
        flow.clickTag(BlockedWordsE2eIds.deleteButton("Pixel"))
        flow.awaitTagGone(BlockedWordsE2eIds.row("Pixel"))
    }

    // reader bookmark button: desktop reaadermode/ReaderModeScreen.desktop.kt
    @Test
    fun `SM-013 relaunch persistence`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openArticle(READER_SUCCESS_ID)
        flow.awaitReaderTitle(READER_SUCCESS_TITLE)
        flow.toggleReaderButton(flow.strings.menuAddToBookmark, flow.strings.menuRemoveFromBookmark)

        flow.relaunch()
        flow.clickSidebar(flow.strings.drawerTitleBookmarks)
        flow.scrollToText(READER_SUCCESS_TITLE)
        flow.clickSidebar(flow.strings.drawerTitleRead)
        flow.scrollToText(READER_SUCCESS_TITLE)
    }

    private companion object {
        const val HIDDEN_FEED_ARTICLE = "E2E Hidden Feed Article"
        const val TECHNOLOGY = "Technology"
        const val NEWS = "News"
        const val UNCATEGORIZED = "Uncategorized"
        const val ANDROID_CENTRAL_FEED = "Android Central RSS Feed"
        const val ARTICLE_WITH_IMAGE = "E2E Article With Image: Delivery Hero stake"
        const val ARTICLE_WITHOUT_IMAGE = "E2E Article Without Image: DeepSeek pricing"
        const val ARTICLE_WITH_COMMENTS = "E2E Article With Comments: Pixel Glow"
        const val READER_SUCCESS_ID = "e2e-article-reader-success"
        const val READER_SUCCESS_TITLE = "E2E Reader Mode Success Article: Interstitium"
        const val READER_FALLBACK_TITLE = "E2E Reader Mode Fallback Article: The Witcher 3"
        const val SEARCH_FIELD_TAG = "search-query"
        const val RENAMED_FEED = "E2E Renamed Android Feed"
        const val OLD_ARTICLE_TITLE = "E2E Old Article Eligible For Delete: Venezia council"
        const val VALID_OPML = "feedflow-valid-opml-smoke.xml"
        const val ARTICLES_CSV = "e2e/fixtures/csv/feedflow-articles-smoke.csv"
        val DOTTED_DATE = Regex("""\d{2}\.\d{2}(\.\d{4})? - .*""")
    }
}
