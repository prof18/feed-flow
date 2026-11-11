package com.prof18.feedflow.desktop.e2e.regression

import com.prof18.feedflow.desktop.e2e.harness.DesktopE2eRule
import com.prof18.feedflow.desktop.e2e.harness.FIXTURE_FEED_PATH
import com.prof18.feedflow.desktop.e2e.harness.FIXTURE_FEED_TITLE
import com.prof18.feedflow.desktop.e2e.harness.SEED_NEWEST_TITLE
import com.prof18.feedflow.desktop.e2e.harness.Seed
import com.prof18.feedflow.desktop.e2e.harness.awaitTagIn
import com.prof18.feedflow.desktop.e2e.harness.awaitTextIn
import com.prof18.feedflow.desktop.e2e.harness.choose
import com.prof18.feedflow.desktop.e2e.harness.clickIn
import com.prof18.feedflow.desktop.e2e.harness.clickMenuItem
import com.prof18.feedflow.desktop.e2e.harness.clickSidebar
import com.prof18.feedflow.desktop.e2e.harness.clickUntil
import com.prof18.feedflow.desktop.e2e.harness.closeWindow
import com.prof18.feedflow.desktop.e2e.harness.dropdownIn
import com.prof18.feedflow.desktop.e2e.harness.isOnScreen
import com.prof18.feedflow.desktop.e2e.harness.nodesIn
import com.prof18.feedflow.desktop.e2e.harness.openAddFeed
import com.prof18.feedflow.desktop.e2e.harness.openAddMenuItem
import com.prof18.feedflow.desktop.e2e.harness.openSidebarMenu
import com.prof18.feedflow.desktop.e2e.harness.replaceText
import com.prof18.feedflow.desktop.e2e.harness.scrollInWindow
import com.prof18.feedflow.desktop.e2e.harness.scrollToText
import com.prof18.feedflow.desktop.e2e.harness.setSwitch
import com.prof18.feedflow.desktop.e2e.harness.sidebarRow
import com.prof18.feedflow.desktop.testing.FeedFormE2eIds
import com.prof18.feedflow.desktop.testing.FeedSuggestionsE2eIds
import com.prof18.feedflow.shared.e2e.E2eSeedAccount
import com.prof18.feedflow.shared.e2e.E2eSeedProfile
import org.junit.Rule
import org.junit.Test
import java.net.URLEncoder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LibraryFlowsTest {

    @get:Rule
    val e2e = DesktopE2eRule()

    /**
     * Add feed window: desktop feedforms/FeedForms.kt (FeedFormE2eIds.ADD_FEED_URL_INPUT, addFeed,
     * addFeedCategoryTitle); entry: desktop home/drawer/DesktopDrawerFeedSourcesByCategories.kt:115
     *
     * Beyond mobile: the flow saves the fixture feed and checks it lands in the sidebar and Timeline.
     * The desktop window stays open after a successful add, ready for the next feed.
     */
    @Test
    fun `REG-101 add feed form validation`() {
        val flow = e2e.start(Seed.Empty)
        flow.awaitText(flow.strings.emptyFeedMessage)

        flow.openAddFeed()
        val window = flow.strings.addFeed
        assertTrue(flow.saveButton(window).isDisabled)
        flow.click(flow.dropdownIn(window, flow.strings.addFeedCategoryTitle))
        flow.awaitTextIn(window, flow.strings.noCategory)
        flow.key(ESCAPE)

        flow.typeInto(flow.urlInput(window), flow.fixtureUrl(FIXTURE_FEED_PATH))
        flow.await("Save to be enabled") { flow.saveButton(window).takeIf { !it.isDisabled } }
        flow.click(flow.saveButton(window))
        flow.await("the fixture feed in the sidebar", LONG_WAIT_MS) {
            flow.nodes().firstOrNull { it.role == "Tab" && it.texts.firstOrNull() == FIXTURE_FEED_TITLE }
        }
        flow.closeWindow(window)
        flow.awaitText("Fixture item 1", timeoutMs = LONG_WAIT_MS)
    }

    /**
     * Feed suggestions window: desktop feedsuggestions/FeedSuggestionsContent.kt (FeedSuggestionsE2eIds,
     * feedSuggestionAdded)
     *
     * A suggestion is saved without downloading it and its articles load with the refresh that runs when the
     * window closes; the flow asserts only the Added state and the new sidebar entry.
     */
    @Test
    fun `REG-103 feed suggestions`() {
        val flow = e2e.start(Seed.Empty)
        flow.awaitText(flow.strings.emptyFeedMessage)

        flow.openAddMenuItem(flow.strings.feedSuggestionsTitle)
        val window = flow.strings.feedSuggestionsTitle
        flow.awaitWindow(window)
        flow.click(flow.awaitTagIn(window, FeedSuggestionsE2eIds.category("business")))
        val add = FeedSuggestionsE2eIds.addButton(BUSINESS_INSIDER_URL)
        flow.click(flow.awaitTagIn(window, add))
        flow.await("the Added state", LONG_WAIT_MS) {
            flow.nodesIn(window).firstOrNull {
                it.testTag == add && flow.strings.feedSuggestionAdded in it.texts
            }
        }
        flow.clickIn(window, flow.strings.actionDone)
        flow.awaitWindowGone(window)
        flow.sidebarRow(BUSINESS_INSIDER)
    }

    // add-feed link argument: desktop Main.kt (AddFeedLinkParser) and main/MainWindow.kt (addFeedLinkInbox);
    // a feed:// link opens as https, as AddFeedLinkParser defines
    @Test
    fun `REG-189 add feed deep link`() {
        val feedUrl = e2e.fixtureUrl(FIXTURE_FEED_PATH)
        val link = "feedflow://add?url=" + URLEncoder.encode(feedUrl, Charsets.UTF_8)
        val flow = e2e.start(Seed.Empty, args = listOf(link))
        val window = flow.strings.addFeed
        flow.awaitWindow(window)
        assertEquals(feedUrl, flow.urlInput(window).editableText)
        assertFalse(flow.saveButton(window).isDisabled)
        flow.clickIn(window, flow.strings.cancelButton)
        flow.awaitWindowGone(window)
        flow.awaitText(flow.strings.emptyFeedMessage)

        val hostAndPath = feedUrl.removePrefix("http://")
        flow.restart(Seed.Empty, args = listOf("feed://$hostAndPath"))
        flow.awaitWindow(window)
        assertEquals("https://$hostAndPath", flow.urlInput(window).editableText)
        flow.clickIn(window, flow.strings.cancelButton)
        flow.awaitWindowGone(window)
        flow.awaitText(flow.strings.emptyFeedMessage)
    }

    // Hide Shorts option: desktop feedforms/FeedForms.kt:188 (ADD_FEED_HIDE_YOUTUBE_SHORTS, hideYoutubeShorts)
    @Test
    fun `REG-130 add feed secondary options`() {
        val flow = e2e.start(Seed.Notifications)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openAddFeed()
        val window = flow.strings.addFeed
        val input = flow.urlInput(window)
        assertFalse(flow.hasShortsToggle(window))
        flow.typeInto(input, YOUTUBE_CHANNEL)
        assertFalse(flow.shortsToggle(window).isShortsChecked())
        flow.click(flow.shortsToggle(window))
        flow.await("Hide Shorts to be checked") { flow.shortsToggle(window).takeIf { it.isShortsChecked() } }
        flow.awaitTextIn(window, flow.strings.hideYoutubeShortsDescription)

        flow.replaceText(flow.urlInput(window), "https://example.com/rss.xml")
        flow.await("Hide Shorts to disappear") { Unit.takeIf { !flow.hasShortsToggle(window) } }
        flow.replaceText(flow.urlInput(window), YOUTUBE_CHANNEL)
        assertFalse(flow.shortsToggle(window).isShortsChecked())
        flow.clickIn(window, flow.strings.cancelButton)
        flow.awaitWindowGone(window)

        flow.openAddFeed()
        flow.typeInto(flow.urlInput(window), YOUTUBE_CHANNEL)
        assertFalse(flow.shortsToggle(window).isShortsChecked())
        flow.clickIn(window, flow.strings.cancelButton)
    }

    /**
     * Feed settings from an article: not on desktop over attach (article context menus cannot be opened,
     * see Known limitations); the flow opens Feed settings from the sidebar feed menu instead.
     * open mode and notification toggle: desktop feedforms/FeedForms.kt (articleOpenMode,
     * enableNotificationsForFeed)
     */
    @Test
    fun `REG-129 edit feed secondary options`() {
        val flow = e2e.start(Seed.Notifications)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openFeedSettings(ANDROID_CENTRAL)
        val window = flow.strings.openFeedSettings
        flow.choose(window, flow.strings.articleOpenMode, flow.strings.readerContentSourceWeb)
        if (flow.nodesIn(window).any { flow.strings.enableNotificationsForFeed in it.texts }) {
            flow.setSwitch(window, flow.strings.enableNotificationsForFeed, on = true)
        }
        flow.clickIn(window, flow.strings.actionSave)
        flow.awaitWindowGone(window)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openFeedSettings(ANDROID_CENTRAL)
        assertTrue(flow.strings.readerContentSourceWeb in flow.dropdownIn(window, flow.strings.articleOpenMode).texts)
        flow.clickIn(window, flow.strings.cancelButton)
    }

    // YouTube feed and playlist URLs: desktop feedforms/FeedForms.kt:188 (hideYoutubeShorts)
    @Test
    fun `REG-178 add feed youtube feed and playlist options`() {
        val flow = e2e.start(Seed.Notifications)
        flow.awaitText(SEED_NEWEST_TITLE)
        flow.checkYoutubeFeedAndPlaylist()

        flow.restart(Seed.of(E2eSeedProfile.SYNC_LINKED_MOCK, E2eSeedAccount.FEEDBIN))
        flow.awaitTag(MAIN_SURFACE_TAG)
        flow.checkYoutubeFeedAndPlaylist()
    }

    /**
     * Edit feed Hide Shorts: desktop feedforms/FeedForms.kt:298 (EDIT_HIDE_YOUTUBE_SHORTS_TOGGLE)
     *
     * Opened from the sidebar feed menu: article context menus cannot be opened over attach.
     */
    @Test
    fun `REG-183 edit feed youtube hide shorts`() {
        val flow = e2e.start(Seed.Notifications)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openFeedSettings(ANDROID_CENTRAL)
        val window = flow.strings.openFeedSettings
        assertTrue(flow.nodesIn(window).none { it.testTag == FeedFormE2eIds.EDIT_HIDE_YOUTUBE_SHORTS_TOGGLE })
        flow.replaceText(flow.urlInput(window), YOUTUBE_FEED_2)
        val toggle = { flow.awaitTagIn(window, FeedFormE2eIds.EDIT_HIDE_YOUTUBE_SHORTS_TOGGLE) }
        assertEquals("Off", toggle().toggleableState)
        flow.click(toggle())
        flow.await("the no-Shorts playlist URL") {
            flow.urlInput(window).takeIf {
                it.editableText?.contains("playlist_id=UULFsBjURrPoezykLs9EqgamOA") == true
            }
        }
        flow.click(toggle())
        flow.await("the channel URL") {
            flow.urlInput(window).takeIf {
                it.editableText?.contains("channel_id=UCsBjURrPoezykLs9EqgamOA") == true
            }
        }
        flow.clickIn(window, flow.strings.cancelButton)
    }

    /**
     * Desktop has no feed list screen in Settings; feeds are managed from the sidebar.
     * expand and collapse: desktop home/drawer/DesktopDrawerFeedSourcesByCategories.kt
     * Feed settings, rename (feed name), delete: desktop home/drawer/DesktopFeedSourceDrawerItem.kt:249,
     * feedforms/FeedForms.kt (feedName, deleteFeedButton)
     *
     * Not covered: the fetch-failed warning, which needs a feed whose last fetch failed and no seed
     * profile provides one.
     */
    @Test
    fun `REG-104 feed source list management`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.click(flow.awaitContentDescription(flow.strings.collapseCategoryButtonContentDescription(TECHNOLOGY)))
        flow.awaitSidebarGone(ANDROID_CENTRAL)
        flow.click(flow.awaitContentDescription(flow.strings.expandCategoryButtonContentDescription(TECHNOLOGY)))
        flow.sidebarRow(ANDROID_CENTRAL)

        flow.openFeedSettings(ANDROID_CENTRAL)
        val window = flow.strings.openFeedSettings
        flow.replaceText(flow.awaitTagIn(window, FeedFormE2eIds.NAME_INPUT), RENAMED_SOURCE)
        flow.clickIn(window, flow.strings.actionSave)
        flow.awaitWindowGone(window)
        flow.sidebarRow(RENAMED_SOURCE)

        flow.openSidebarMenu(RENAMED_SOURCE)
        flow.clickMenuItem(flow.strings.deleteFeed)
        flow.confirmDialog()
        flow.awaitSidebarGone(RENAMED_SOURCE)
    }

    /**
     * New category from the Add feed category picker: desktop feedforms/FeedForms.kt (newCategoryHint,
     * categoryNameAlreadyExists); rename and delete all feeds: desktop
     * home/drawer/DesktopDrawerFeedSourcesByCategories.kt:400-460 (renameCategory, deleteAllFeedsInCategory)
     */
    @Test
    fun `REG-105 category management`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openAddFeed()
        val window = flow.strings.addFeed
        flow.choose(
            window,
            flow.strings.addFeedCategoryTitle,
            flow.strings.desktopNewCategoryMenu,
            expectSelected = false,
        )
        val categoryWindow = flow.strings.desktopNewCategoryMenu.trimEnd('…', '.')
        flow.typeInto(flow.awaitTagIn(categoryWindow, FeedFormE2eIds.CATEGORY_ADD_INPUT), TECHNOLOGY)
        flow.awaitTextIn(categoryWindow, flow.strings.categoryNameAlreadyExists)
        flow.replaceText(flow.awaitTagIn(categoryWindow, FeedFormE2eIds.CATEGORY_ADD_INPUT), CREATED_CATEGORY)
        flow.click(flow.awaitTagIn(categoryWindow, FeedFormE2eIds.CATEGORY_ADD_CONFIRM))
        flow.awaitWindowGone(categoryWindow)
        flow.await("the new category selected") {
            flow.dropdownIn(window, flow.strings.addFeedCategoryTitle).takeIf { CREATED_CATEGORY in it.texts }
        }
        flow.clickIn(window, flow.strings.cancelButton)
        flow.awaitWindowGone(window)

        flow.openSidebarMenu(TECHNOLOGY)
        flow.clickMenuItem(flow.strings.renameCategory)
        val renameWindow = flow.strings.editCategory
        flow.replaceText(flow.awaitTagIn(renameWindow, FeedFormE2eIds.CATEGORY_RENAME_INPUT), RENAMED_CATEGORY)
        flow.click(flow.awaitTagIn(renameWindow, FeedFormE2eIds.CATEGORY_RENAME_SAVE))
        flow.awaitWindowGone(renameWindow)
        flow.sidebarRow(RENAMED_CATEGORY)

        flow.openSidebarMenu(RENAMED_CATEGORY)
        flow.clickMenuItem(flow.strings.deleteAllFeedsInCategory)
        flow.confirmDialog()
        flow.awaitSidebarGone(ANDROID_CENTRAL)
    }

    /**
     * Desktop has no "more" menu on the article list header; the in-window control for the selected
     * feed's settings is the sidebar feed menu.
     * open mode: desktop feedforms/FeedForms.kt (articleOpenMode)
     */
    @Test
    fun `REG-131 home source filter edit entry`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.clickSidebar(ANDROID_CENTRAL)
        flow.awaitText(SEED_NEWEST_TITLE)
        flow.openFeedSettings(ANDROID_CENTRAL)
        val window = flow.strings.openFeedSettings
        flow.choose(window, flow.strings.articleOpenMode, flow.strings.readerContentSourceWeb)
        flow.clickIn(window, flow.strings.actionSave)
        flow.awaitWindowGone(window)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openFeedSettings(ANDROID_CENTRAL)
        assertTrue(flow.strings.readerContentSourceWeb in flow.dropdownIn(window, flow.strings.articleOpenMode).texts)
        flow.clickIn(window, flow.strings.cancelButton)
    }

    /**
     * The empty library view: desktop home/DesktopHomeScreenContent.kt:222-245 (noFeedsFoundMessage,
     * addFeed, importExportLabel), shown after Refresh feeds finds no feeds.
     *
     * Not on desktop: the NoFeedsDialog's Accounts and Feed Suggestions buttons (the dialog cannot be
     * reached, see SM-001), so this view offers Add feed and Import and export only.
     */
    @Test
    fun `REG-147 no feeds empty state cta`() {
        val flow = e2e.start(Seed.Empty)
        flow.awaitText(flow.strings.emptyFeedMessage)

        flow.clickUntil("the no-feeds view", { flow.awaitText(flow.strings.refreshFeeds) }) {
            flow.isShown(flow.strings.noFeedsFoundMessage)
        }

        flow.click(flow.mainButton(flow.strings.addFeed))
        flow.awaitWindow(flow.strings.addFeed)
        flow.awaitTagIn(flow.strings.addFeed, FeedFormE2eIds.ADD_FEED_URL_INPUT)
        flow.closeWindow(flow.strings.addFeed)

        flow.click(flow.mainButton(flow.strings.importExportLabel))
        flow.awaitWindow(flow.strings.settingsTitle)
        flow.awaitTextIn(flow.strings.settingsTitle, flow.strings.importExportOpmlSectionTitle)
        flow.awaitTextIn(flow.strings.settingsTitle, flow.strings.importExportArticlesSectionTitle)
    }

    // sidebar feed menu entries: desktop home/drawer/DesktopFeedSourceDrawerItem.kt:240-260
    // change category: desktop home/drawer/DesktopFeedMoveDialog.kt (changeCategory)
    @Test
    fun `REG-146 drawer feed source context menu`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openSidebarMenu(ANDROID_CENTRAL)
        listOf(
            flow.strings.markAllReadButton,
            flow.strings.openFeedWebsiteButton,
            flow.strings.openFeedSettings,
            flow.strings.changeCategory,
            flow.strings.menuAddToPinned,
            flow.strings.deleteFeed,
        ).forEach { flow.awaitText(it) }
        flow.clickMenuItem(flow.strings.menuAddToPinned)
        flow.await("the feed in Pinned feeds") {
            flow.sidebarRow(ANDROID_CENTRAL).takeIf { it.bounds.y < flow.sidebarRow(UNCATEGORIZED).bounds.y }
        }

        flow.openSidebarMenu(ANDROID_CENTRAL)
        flow.clickMenuItem(flow.strings.menuRemoveFromPinned)
        flow.openSidebarMenu(ANDROID_CENTRAL)
        flow.awaitText(flow.strings.menuAddToPinned)
        flow.assertAbsent(flow.strings.menuRemoveFromPinned)

        flow.clickMenuItem(flow.strings.openFeedWebsiteButton)
        flow.awaitOpenedUri { it.contains("androidcentral.com") }

        flow.openSidebarMenu(ANDROID_CENTRAL)
        flow.clickMenuItem(flow.strings.changeCategory)
        val moveWindow = flow.strings.changeCategory
        flow.clickIn(moveWindow, NEWS)
        flow.await("Confirm to be enabled") {
            flow.nodesIn(moveWindow).lastOrNull {
                it.role == "Button" && flow.strings.confirmButton in it.texts && !it.isDisabled
            }
        }
        flow.clickIn(moveWindow, flow.strings.confirmButton)
        flow.awaitWindowGone(moveWindow)
        flow.await("the feed under News") {
            val news = flow.sidebarRow(NEWS).bounds.y
            val technology = flow.sidebarRow(TECHNOLOGY).bounds.y
            flow.sidebarRow(ANDROID_CENTRAL).bounds.y.takeIf { it in news..technology }
        }

        flow.openSidebarMenu(ANDROID_CENTRAL)
        flow.clickMenuItem(flow.strings.openFeedSettings)
        flow.awaitWindow(flow.strings.openFeedSettings)
        flow.closeWindow(flow.strings.openFeedSettings)

        flow.openSidebarMenu(ANDROID_CENTRAL)
        flow.clickMenuItem(flow.strings.deleteFeed)
        flow.cancelDialog()
        flow.sidebarRow(ANDROID_CENTRAL)
    }

    // category menu Mark all as read: desktop home/drawer/DesktopDrawerFeedSourcesByCategories.kt:415
    @Test
    fun `REG-149 drawer category mark all read`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openSidebarMenu(TECHNOLOGY)
        flow.clickMenuItem(flow.strings.markAllReadButton)
        flow.clickSidebar(TECHNOLOGY)
        flow.awaitText(flow.strings.emptyFeedMessage)
    }

    // category menu Delete all feeds: desktop home/drawer/DesktopDrawerFeedSourcesByCategories.kt
    @Test
    fun `REG-151 feed source list delete all in category`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openSidebarMenu(TECHNOLOGY)
        flow.clickMenuItem(flow.strings.deleteAllFeedsInCategory)
        flow.confirmDialog()
        flow.awaitSidebarGone(ANDROID_CENTRAL)
        flow.awaitSidebarGone(ANDROID_POLICE)
    }

    // sidebar add menu: desktop home/drawer/DesktopDrawerFeedSourcesByCategories.kt:115
    @Test
    fun `REG-153 drawer add import feed entries`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.openAddMenuItem(flow.strings.addFeed)
        flow.awaitTagIn(flow.strings.addFeed, FeedFormE2eIds.ADD_FEED_URL_INPUT)
        flow.closeWindow(flow.strings.addFeed)

        flow.openAddMenuItem(flow.strings.feedSuggestionsTitle)
        flow.awaitTagIn(flow.strings.feedSuggestionsTitle, FeedSuggestionsE2eIds.category("business"))
        flow.closeWindow(flow.strings.feedSuggestionsTitle)

        flow.openAddMenuItem(flow.strings.importFeedButton)
        flow.awaitTextIn(flow.strings.settingsTitle, flow.strings.importExportOpmlSectionTitle)
        flow.awaitTextIn(flow.strings.settingsTitle, flow.strings.importExportArticlesSectionTitle)
    }

    /**
     * Feed settings URL field: desktop feedforms/FeedForms.kt (hidden for server-synced feeds).
     *
     * Not covered: the URL-less article's context menu (no Share entry), because article context menus
     * cannot be opened over attach (DEFERRED under T10.8).
     */
    @Test
    fun `REG-186 server feed url and urlless share guards`() {
        val flow = e2e.start(Seed.of(E2eSeedProfile.SYNC_LINKED_MOCK, E2eSeedAccount.FRESH_RSS))
        flow.awaitTag(MAIN_SURFACE_TAG)

        flow.sidebarRow(ANDROID_CENTRAL)
        flow.openFeedSettings(ANDROID_CENTRAL)
        val window = flow.strings.openFeedSettings
        flow.awaitTagIn(window, FeedFormE2eIds.NAME_INPUT)
        assertTrue(flow.nodesIn(window).none { it.testTag == FeedFormE2eIds.ADD_FEED_URL_INPUT })
        flow.closeWindow(window)

        flow.restart(Seed.FeedContent)
        flow.scrollToText(URLLESS_ARTICLE)
    }

    /**
     * Next feed button: desktop ui/components/DesktopArticleList.kt:326 (nextFeedState title in the list
     * footer), the desktop counterpart of the pull-to-next-feed gesture.
     */
    @Test
    fun `REG-148 pull to next feed`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.clickSidebar(ANDROID_CENTRAL)
        flow.awaitText(SEED_NEWEST_TITLE)
        val next = flow.nextFeedButton()
        val nextFeed = next.texts.first()
        flow.click(next)
        flow.await("feed '$nextFeed' selected") { flow.sidebarRow(nextFeed).takeIf { it.isSelected } }
        assertFalse(flow.sidebarRow(ANDROID_CENTRAL).isSelected)
    }

    /**
     * Open another feed: desktop home/DesktopHomeScreenContent.kt:409 (openAnotherFeed), which toggles the
     * sidebar in the two-pane layout.
     */
    @Test
    fun `REG-155 empty home open another feed`() {
        val flow = e2e.start(Seed.Empty)
        flow.awaitText(flow.strings.emptyFeedMessage)
        flow.awaitText(flow.strings.refreshFeeds)

        flow.clickUntil("the sidebar to hide", { flow.awaitText(flow.strings.openAnotherFeed) }) {
            !flow.isShown(flow.strings.drawerTitleTimeline)
        }
        flow.clickUntil("the sidebar to show", { flow.awaitText(flow.strings.openAnotherFeed) }) {
            flow.isShown(flow.strings.drawerTitleTimeline)
        }
    }

    /**
     * Drag in the Feeds tree: desktop home/drawer/DesktopDrawerDragModifiers.kt (FACT drag).
     *
     * Desktop reorders by dragging rows directly, without the mobile reorder toggles.
     */
    @Test
    fun `REG-157 drawer reorder smoke`() {
        val flow = e2e.start(Seed.ReorderDrag)
        flow.awaitTag(MAIN_SURFACE_TAG)

        val pinned = flow.await("two pinned feeds") {
            runCatching { flow.feedsUnder(flow.strings.drawerTitlePinnedFeeds) }.getOrNull()?.takeIf { it.size >= 2 }
        }
        flow.drag(flow.sidebarRow(pinned.first()), flow.sidebarRow(pinned.last()))
        flow.await("the pinned feeds to be reordered") {
            flow.feedsUnder(flow.strings.drawerTitlePinnedFeeds).takeIf { it != pinned && it.toSet() == pinned.toSet() }
        }

        val category = TECHNOLOGY
        val feeds = flow.feedsUnder(category)
        assertTrue(feeds.size >= 2, "category '$category' needs two feeds, has $feeds")
        flow.drag(flow.sidebarRow(feeds.first()), flow.sidebarRow(feeds.last()))
        flow.await("the feeds of '$category' to be reordered") {
            flow.feedsUnder(category).takeIf { it != feeds && it.toSet() == feeds.toSet() }
        }
        flow.clickSidebar(flow.feedsUnder(category).first())
        flow.clickSidebar(flow.strings.drawerTitleTimeline)
    }

    // next feed button after the last unread article: desktop ui/components/DesktopArticleList.kt:326
    @Test
    fun `REG-158 empty feed next feed navigation`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.awaitText(SEED_NEWEST_TITLE)

        flow.clickSidebar(PINNED_FEED)
        flow.awaitText(PINNED_ARTICLE)
        flow.click(flow.scrollToText(flow.strings.markAllReadButton))
        flow.awaitText(flow.strings.emptyFeedMessage)
        val next = flow.await("a next-feed button") {
            flow.nodes().firstOrNull {
                it.isOnScreen && it.role == "Button" && it.texts.size == 1 &&
                    flow.nodes().any { tab -> tab.role == "Tab" && tab.texts.firstOrNull() == it.texts.first() }
            }
        }
        val nextFeed = next.texts.first()
        assertNotEquals(PINNED_FEED, nextFeed)
        flow.click(next)
        flow.await("feed '$nextFeed' selected") { flow.sidebarRow(nextFeed).takeIf { it.isSelected } }
        flow.assertAbsent(PINNED_ARTICLE)
    }

    private fun com.prof18.feedflow.desktop.e2e.harness.Flow.saveButton(window: String) =
        await("the $window save button") {
            nodesIn(window).lastOrNull { it.role == "Button" && strings.addFeed in it.texts }
        }

    private fun com.prof18.feedflow.desktop.e2e.harness.Flow.mainButton(text: String) =
        await("button '$text'") {
            nodes().firstOrNull {
                it.isOnScreen && it.role == "Button" && text in it.texts && it.key.startsWith("window:1:0:")
            }
        }

    private fun com.prof18.feedflow.desktop.e2e.harness.Flow.urlInput(window: String) =
        scrollInWindow(window, "the feed URL field") { it.testTag == FeedFormE2eIds.ADD_FEED_URL_INPUT }

    private fun com.prof18.feedflow.desktop.e2e.harness.Flow.hasShortsToggle(window: String) =
        nodesIn(window).any { it.testTag == FeedFormE2eIds.ADD_FEED_HIDE_YOUTUBE_SHORTS }

    private fun com.prof18.feedflow.desktop.e2e.harness.Flow.shortsToggle(window: String) =
        scrollInWindow(window, "the Hide Shorts option") { it.testTag == FeedFormE2eIds.ADD_FEED_HIDE_YOUTUBE_SHORTS }

    private fun dev.sebastiano.spectre.agent.transport.NodeSnapshotDto.isShortsChecked() = toggleableState == "On"

    private fun com.prof18.feedflow.desktop.e2e.harness.Flow.awaitSidebarGone(name: String) {
        await("sidebar row '$name' to disappear") {
            Unit.takeIf { nodes().none { it.role == "Tab" && it.texts.firstOrNull() == name } }
        }
    }

    private fun com.prof18.feedflow.desktop.e2e.harness.Flow.openFeedSettings(feed: String) {
        openSidebarMenu(feed)
        clickMenuItem(strings.openFeedSettings)
        awaitWindow(strings.openFeedSettings)
    }

    private fun com.prof18.feedflow.desktop.e2e.harness.Flow.confirmDialog() {
        val dialog = await("a confirmation dialog") { windows().lastOrNull { it.isPopup && it.isShowing }?.title }
        click(
            await("the confirm button of '$dialog'") {
                nodesIn(dialog).lastOrNull { it.role == "Button" && strings.cancelButton !in it.texts }
            },
        )
        awaitWindowGone(dialog)
    }

    private fun com.prof18.feedflow.desktop.e2e.harness.Flow.cancelDialog() {
        val dialog = await("a confirmation dialog") { windows().lastOrNull { it.isPopup && it.isShowing }?.title }
        clickIn(dialog, strings.cancelButton)
        awaitWindowGone(dialog)
    }

    private fun com.prof18.feedflow.desktop.e2e.harness.Flow.nextFeedButton() =
        await("the next-feed button") {
            scrollToText(strings.markAllReadButton)
            nodes().firstOrNull {
                it.isOnScreen && it.role == "Button" && it.texts.size == 1 &&
                    it.texts.first() != strings.markAllReadButton &&
                    nodes().any { tab -> tab.role == "Tab" && tab.texts.firstOrNull() == it.texts.first() }
            }
        }

    private fun com.prof18.feedflow.desktop.e2e.harness.Flow.isCategoryHeader(
        tab: dev.sebastiano.spectre.agent.transport.NodeSnapshotDto,
    ): Boolean {
        val description = strings.collapseCategoryButtonContentDescription(tab.texts.first())
        return nodes().any { description in it.contentDescriptions }
    }

    /** Feed rows listed under a category in the sidebar, top to bottom. */
    private fun com.prof18.feedflow.desktop.e2e.harness.Flow.feedsUnder(category: String): List<String> {
        val tabs = nodes().filter { it.role == "Tab" && it.key.startsWith("window:1:0:") }.sortedBy { it.bounds.y }
        if (category == strings.drawerTitlePinnedFeeds) {
            val header = nodes().first { it.texts == listOf(category) }.bounds.y
            val firstCategory = tabs.first { tab ->
                isCategoryHeader(tab)
            }.bounds.y
            return tabs
                .filter { it.bounds.y in header..firstCategory && it.bounds.y != firstCategory }
                .map { it.texts.first() }
        }
        val start = tabs.indexOfFirst { it.texts.firstOrNull() == category }
        val headers = tabs.filter { tab ->
            isCategoryHeader(tab)
        }.toSet()
        return tabs.drop(start + 1).takeWhile { it !in headers }.map { it.texts.first() }
    }

    private fun com.prof18.feedflow.desktop.e2e.harness.Flow.checkYoutubeFeedAndPlaylist() {
        openAddFeed()
        val window = strings.addFeed
        typeInto(urlInput(window), YOUTUBE_FEED)
        assertFalse(shortsToggle(window).isShortsChecked())
        replaceText(urlInput(window), YOUTUBE_PLAYLIST)
        await("Hide Shorts to disappear") { Unit.takeIf { !hasShortsToggle(window) } }
        clickIn(window, strings.cancelButton)
        awaitWindowGone(window)
    }

    private companion object {
        const val LONG_WAIT_MS = 30_000L
        const val ESCAPE = java.awt.event.KeyEvent.VK_ESCAPE
        const val MAIN_SURFACE_TAG = "desktop_main_window_surface"
        const val TECHNOLOGY = "Technology"
        const val NEWS = "News"
        const val UNCATEGORIZED = "Uncategorized"
        const val ANDROID_CENTRAL = "Android Central RSS Feed"
        const val ANDROID_POLICE = "Android Police"
        const val PINNED_FEED = "Ars Technica"
        const val PINNED_ARTICLE = "E2E Pinned Feed Article: Eufy C15"
        const val BUSINESS_INSIDER = "Business Insider"
        const val BUSINESS_INSIDER_URL = "https://feeds2.feedburner.com/businessinsider"
        const val YOUTUBE_CHANNEL = "https://www.youtube.com/@mkbhd"
        const val YOUTUBE_FEED = "https://www.youtube.com/feeds/videos.xml?channel_id=UCBJycsmduvYEL83R_U4JriQ"
        const val YOUTUBE_FEED_2 = "https://www.youtube.com/feeds/videos.xml?channel_id=UCsBjURrPoezykLs9EqgamOA"
        const val YOUTUBE_PLAYLIST = "https://www.youtube.com/playlist?list=PLbpi6ZahtOH7MBdd2q811v_7Tu31vnsyq"
        const val RENAMED_SOURCE = "E2E Renamed Source"
        const val CREATED_CATEGORY = "E2E Created Category"
        const val RENAMED_CATEGORY = "E2E Renamed Category"
        const val URLLESS_ARTICLE = "E2E Feed Content Article Without Link"
        val LIBRARY_TABS = setOf("Timeline", "Read", "Bookmarks")
    }
}
