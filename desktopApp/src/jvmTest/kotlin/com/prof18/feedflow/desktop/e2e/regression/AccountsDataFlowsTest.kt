package com.prof18.feedflow.desktop.e2e.regression

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.e2e.harness.DesktopE2eRule
import com.prof18.feedflow.desktop.e2e.harness.Flow
import com.prof18.feedflow.desktop.e2e.harness.SEED_NEWEST_TITLE
import com.prof18.feedflow.desktop.e2e.harness.Seed
import com.prof18.feedflow.desktop.e2e.harness.awaitTagIn
import com.prof18.feedflow.desktop.e2e.harness.clickIn
import com.prof18.feedflow.desktop.e2e.harness.clickSidebar
import com.prof18.feedflow.desktop.e2e.harness.closeWindow
import com.prof18.feedflow.desktop.e2e.harness.nodesIn
import com.prof18.feedflow.desktop.e2e.harness.openAddMenuItem
import com.prof18.feedflow.desktop.e2e.harness.openSettings
import com.prof18.feedflow.desktop.e2e.harness.scrollToTag
import com.prof18.feedflow.desktop.e2e.harness.typeIntoFocused
import com.prof18.feedflow.desktop.e2e.harness.visibleArticleRows
import com.prof18.feedflow.desktop.testing.AccountE2eIds
import com.prof18.feedflow.desktop.testing.ImportExportE2eIds
import com.prof18.feedflow.shared.e2e.E2eSeedAccount
import com.prof18.feedflow.shared.e2e.E2eSeedProfile
import dev.sebastiano.spectre.agent.transport.NodeSnapshotDto
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountsDataFlowsTest {

    @get:Rule
    val e2e = DesktopE2eRule()

    // invalid OPML and Choose another file: desktop importexport/ImportExportContent.kt:181
    // (ImportExportE2eIds.INVALID_OPML, CHOOSE_ANOTHER_FILE)
    @Test
    fun `REG-119 opml import error states`() {
        val flow = e2e.start(Seed.Empty)
        flow.openImportExport()

        flow.respondToFileDialog(File(flow.repoRoot, INVALID_OPML))
        flow.click(flow.awaitTagIn(SETTINGS, ImportExportE2eIds.IMPORT_FEEDS))
        flow.awaitTagIn(SETTINGS, ImportExportE2eIds.INVALID_OPML)
        flow.respondToFileDialog(flow.fixtureOpml(VALID_OPML))
        flow.click(flow.awaitTagIn(SETTINGS, ImportExportE2eIds.CHOOSE_ANOTHER_FILE))
        flow.awaitTagIn(SETTINGS, ImportExportE2eIds.FEED_IMPORT_SUCCESS)
        flow.click(flow.awaitTagIn(SETTINGS, ImportExportE2eIds.DONE))
    }

    /**
     * CSV import: desktop importexport/ImportExportContent.kt:205 (ARTICLE_IMPORT_SUCCESS)
     *
     * The CSV rows reference the feed by an id derived from the original OPML feed URL, so this flow
     * imports the committed OPML unchanged instead of the fixture-server copy. Its feed host,
     * e2e.feedflow.local, is an mDNS name: the refresh after Done fails on the local network and sends
     * nothing outside the machine.
     */
    @Test
    fun `REG-120 csv import article states`() {
        val flow = e2e.start(Seed.Empty)
        flow.openImportExport()

        flow.respondToFileDialog(File(flow.repoRoot, "e2e/fixtures/opml/$VALID_OPML"))
        flow.click(flow.awaitTagIn(SETTINGS, ImportExportE2eIds.IMPORT_FEEDS))
        flow.awaitTagIn(SETTINGS, ImportExportE2eIds.FEED_IMPORT_SUCCESS)
        flow.click(flow.awaitTagIn(SETTINGS, ImportExportE2eIds.DONE))
        flow.respondToFileDialog(File(flow.repoRoot, ARTICLES_CSV))
        flow.click(flow.awaitTagIn(SETTINGS, ImportExportE2eIds.IMPORT_ARTICLES))
        flow.awaitTagIn(SETTINGS, ImportExportE2eIds.ARTICLE_IMPORT_SUCCESS)
        flow.click(flow.awaitTagIn(SETTINGS, ImportExportE2eIds.DONE))
        flow.closeWindow(SETTINGS)

        flow.clickSidebar(flow.strings.drawerTitleTimeline)
        flow.awaitText(CSV_UNREAD)
        flow.awaitText(CSV_BOOKMARKED_UNREAD)
        flow.assertAbsent(CSV_READ)
        flow.clickSidebar(flow.strings.drawerTitleRead)
        flow.awaitText(CSV_READ)
        flow.awaitText(CSV_BOOKMARKED_READ)
        flow.assertAbsent(CSV_UNREAD)
        flow.clickSidebar(flow.strings.drawerTitleBookmarks)
        flow.awaitText(CSV_BOOKMARKED_UNREAD)
        flow.awaitText(CSV_BOOKMARKED_READ)
        flow.assertAbsent(CSV_READ)
        flow.assertAbsent(CSV_UNREAD)
    }

    // bookmarked export: desktop importexport/ImportExportScreen.desktop.kt (articlesExportFilterBookmarked)
    @Test
    fun `REG-139 article export filter`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.openImportExport()
        flow.exportArticles(flow.strings.articlesExportFilterBookmarked, "export-bookmarked.csv")
    }

    // OPML export: desktop importexport/ImportExportContent.kt:189 (FEED_EXPORT_SUCCESS)
    @Test
    fun `REG-140 opml export success`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.openImportExport()

        val file = File(flow.homeDir, "export-feeds.opml")
        flow.respondToFileDialog(file)
        flow.click(flow.awaitTagIn(SETTINGS, ImportExportE2eIds.EXPORT_FEEDS))
        flow.awaitTagIn(SETTINGS, ImportExportE2eIds.FEED_EXPORT_SUCCESS)
        assertTrue(file.readText().contains("<opml"))
    }

    // export filter matrix: desktop importexport/ImportExportScreen.desktop.kt (articlesExportFilter*)
    @Test
    fun `REG-141 article export filter matrix`() {
        val flow = e2e.start(Seed.ContentRich)
        flow.openImportExport()
        listOf(
            flow.strings.articlesExportFilterAll to "export-all.csv",
            flow.strings.articlesExportFilterRead to "export-read.csv",
            flow.strings.articlesExportFilterUnread to "export-unread.csv",
        ).forEach { (filter, name) ->
            flow.exportArticles(filter, name)
            flow.click(flow.awaitTagIn(SETTINGS, ImportExportE2eIds.DONE))
        }
    }

    // account list and disconnect: desktop accounts/DesktopAccountsContent.kt (AccountE2eIds.providerRow,
    // desktopAccountsUnavailableStatus) and accounts/AccountComponents.kt:184-241 (accountDisconnectTitle)
    @Test
    fun `REG-116 account list one account constraint`() {
        val flow = e2e.start(Seed.of(E2eSeedProfile.SYNC_LINKED_MOCK, E2eSeedAccount.FRESH_RSS))
        flow.openAccounts()

        assertFalse(flow.provider(SyncAccounts.FRESH_RSS).isDisabled)
        listOf(SyncAccounts.MINIFLUX, SyncAccounts.FEEDBIN, SyncAccounts.BAZQUX).forEach {
            assertTrue(flow.provider(it).isDisabled, "$it disabled")
        }
        flow.click(flow.provider(SyncAccounts.FRESH_RSS))
        flow.awaitTagIn(SETTINGS, AccountE2eIds.CONNECTED_MESSAGE)
        flow.awaitTagIn(SETTINGS, AccountE2eIds.LAST_SYNC_LABEL)

        flow.click(flow.awaitTagIn(SETTINGS, AccountE2eIds.DISCONNECT_BUTTON))
        flow.awaitWindow(flow.strings.accountDisconnectTitle)
        flow.clickIn(flow.strings.accountDisconnectTitle, flow.strings.cancelButton)
        flow.awaitWindowGone(flow.strings.accountDisconnectTitle)
        flow.awaitTagIn(SETTINGS, AccountE2eIds.CONNECTED_MESSAGE)

        flow.click(flow.awaitTagIn(SETTINGS, AccountE2eIds.DISCONNECT_BUTTON))
        flow.awaitWindow(flow.strings.accountDisconnectTitle)
        flow.click(
            flow.await("the disconnect confirmation") {
                flow.nodesIn(flow.strings.accountDisconnectTitle).lastOrNull {
                    it.role == "Button" && flow.strings.accountDisconnectButton in it.texts
                }
            },
        )
        flow.awaitWindowGone(flow.strings.accountDisconnectTitle)
        flow.backToServices()
        flow.await("Miniflux to be enabled") { flow.provider(SyncAccounts.MINIFLUX).takeIf { !it.isDisabled } }
    }

    /**
     * credentials form: desktop accounts/CredentialsForm.kt (AccountE2eIds inputs and connect button)
     *
     * The flow never presses Connect.
     *
     * Not on desktop: the server subscriptions warning (serverAccountSubscriptionsWarning has no desktop
     * caller), so the restart with local feeds is skipped.
     */
    @Test
    fun `REG-117 greader provider form validation`() {
        val flow = e2e.start(Seed.Empty)
        flow.openAccounts()
        listOf(SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX, SyncAccounts.BAZQUX, SyncAccounts.FEEDBIN).forEach {
            flow.provider(it)
        }

        flow.click(flow.provider(SyncAccounts.FRESH_RSS))
        assertTrue(flow.connectButton().isDisabled)
        flow.typeInto(flow.awaitTagIn(SETTINGS, AccountE2eIds.SERVER_URL_INPUT), "https://e2e.example.com")
        flow.typeInto(flow.awaitTagIn(SETTINGS, AccountE2eIds.USERNAME_INPUT), "e2e")
        flow.typeInto(flow.awaitTagIn(SETTINGS, AccountE2eIds.PASSWORD_INPUT), "e2e-password")
        flow.await("Connect to be enabled") { flow.connectButton().takeIf { !it.isDisabled } }
        flow.backToServices()
        flow.click(flow.provider(SyncAccounts.MINIFLUX))
        assertTrue(flow.connectButton().isDisabled)
        flow.backToServices()
        flow.click(flow.provider(SyncAccounts.BAZQUX))
        flow.backToServices()
        flow.click(flow.provider(SyncAccounts.FEEDBIN))
        assertTrue(flow.connectButton().isDisabled)
    }

    /**
     * Dropbox linked state: desktop accounts/AccountComponents.kt.
     *
     * Not covered: the iCloud linked state; the iCloud entry is absent from the Accounts pane in this
     * debug build (setupICloudSupport() in Main.kt).
     */
    @Test
    fun `REG-118 cloud provider mock states`() {
        val flow = e2e.start(Seed.of(E2eSeedProfile.SYNC_LINKED_MOCK, E2eSeedAccount.DROPBOX))
        flow.openAccounts()
        assertFalse(flow.provider(SyncAccounts.DROPBOX).isDisabled)
        assertTrue(flow.provider(SyncAccounts.GOOGLE_DRIVE).isDisabled)
        assertTrue(flow.provider(SyncAccounts.FRESH_RSS).isDisabled)
        flow.click(flow.provider(SyncAccounts.DROPBOX))
        flow.awaitTagIn(SETTINGS, AccountE2eIds.CONNECTED_MESSAGE)
        flow.awaitTagIn(SETTINGS, AccountE2eIds.LAST_SYNC_LABEL)
        flow.awaitTagIn(SETTINGS, AccountE2eIds.DISCONNECT_BUTTON)

        flow.restart(Seed.of(E2eSeedProfile.SYNC_LINKED_MOCK, E2eSeedAccount.ICLOUD))
        flow.openAccounts()
        assertTrue(flow.nodesIn(SETTINGS).none { it.testTag == AccountE2eIds.providerRow(SyncAccounts.ICLOUD) })
    }

    /**
     * Back up now: desktop accounts/AccountComponents.kt:173 (backupButton, account_backup_button).
     *
     * Not covered: the home pending-upload action, which lives only in the File menu of the native
     * menu bar (home/menubar/FileMenu.kt:30) and cannot be reached over attach. The backup is not run.
     */
    @Test
    fun `REG-133 home sync backup action`() {
        val flow = e2e.start(Seed.of(E2eSeedProfile.SYNC_UPLOAD_REQUIRED, E2eSeedAccount.DROPBOX))
        flow.awaitText(SEED_NEWEST_TITLE)
        flow.openAccounts()
        flow.click(flow.provider(SyncAccounts.DROPBOX))
        flow.awaitTagIn(SETTINGS, BACKUP_BUTTON_TAG)
    }

    // linked network providers: desktop accounts/AccountComponents.kt (CONNECTED_MESSAGE, LAST_SYNC_LABEL)
    @Test
    fun `REG-145 network provider linked states`() {
        val accounts = listOf(
            E2eSeedAccount.MINIFLUX to SyncAccounts.MINIFLUX,
            E2eSeedAccount.BAZQUX to SyncAccounts.BAZQUX,
            E2eSeedAccount.FEEDBIN to SyncAccounts.FEEDBIN,
        )
        val flow = e2e.start(Seed.of(E2eSeedProfile.SYNC_LINKED_MOCK, accounts.first().first))
        accounts.forEachIndexed { index, (seedAccount, account) ->
            if (index > 0) flow.restart(Seed.of(E2eSeedProfile.SYNC_LINKED_MOCK, seedAccount))
            flow.openAccounts()
            assertFalse(flow.provider(account).isDisabled)
            accounts.map { it.second }.filter { it != account }.forEach { other ->
                assertTrue(flow.provider(other).isDisabled)
            }
            flow.click(flow.provider(account))
            flow.awaitTagIn(SETTINGS, AccountE2eIds.CONNECTED_MESSAGE)
            flow.awaitTagIn(SETTINGS, AccountE2eIds.LAST_SYNC_LABEL)
            flow.awaitTagIn(SETTINGS, AccountE2eIds.DISCONNECT_BUTTON)
            flow.closeWindow(SETTINGS)
        }
    }

    /**
     * Not covered: the iCloud entry is absent from the Accounts pane in this debug build
     * (setupICloudSupport() in Main.kt), so the unlink flow cannot run. The flow checks the entry is absent.
     */
    @Test
    fun `REG-138 cloud provider disconnect`() {
        val flow = e2e.start(Seed.of(E2eSeedProfile.SYNC_LINKED_MOCK, E2eSeedAccount.ICLOUD))
        flow.openAccounts()
        assertTrue(flow.nodesIn(SETTINGS).none { it.testTag == AccountE2eIds.providerRow(SyncAccounts.ICLOUD) })
    }

    /**
     * Not covered: the iCloud Backup action, because the iCloud entry is absent from the Accounts pane in
     * this debug build (setupICloudSupport() in Main.kt).
     */
    @Test
    fun `REG-142 icloud provider backup`() {
        val flow = e2e.start(Seed.of(E2eSeedProfile.SYNC_LINKED_MOCK, E2eSeedAccount.ICLOUD))
        flow.openAccounts()
        assertTrue(flow.nodesIn(SETTINGS).none { it.testTag == AccountE2eIds.providerRow(SyncAccounts.ICLOUD) })
    }

    // large dataset scroll and search: desktop ui/components/DesktopArticleList.kt (keyset pages, FACT scroll)
    @Test
    fun `REG-134 large content pagination search`() {
        val flow = e2e.start(Seed.LargeContent)
        flow.awaitTag(flow.largeRow(1))
        flow.scrollToTag(flow.largeRow(PAST_FIRST_PAGE))

        flow.click(flow.awaitContentDescription(flow.strings.searchButtonContentDescription))
        flow.awaitTag(SEARCH_FIELD_TAG)
        flow.typeIntoFocused("Pagination Needle")
        flow.awaitText(NEEDLE_TITLE)
    }

    /**
     * mark read on scroll: shared keyset pagination (docs/PAGINATION.md) on the desktop list; scrolling uses
     * Spectre's scrollWheel (FACT scroll). Guards issue #1319 like the Maestro flow: offset pagination over
     * the unread-only query skipped articles 041-080, so article 050 and the last article must be reachable.
     */
    @Test
    fun `REG-162 pagination scroll read`() {
        val flow = e2e.start(Seed.PaginationScrollRead)
        flow.awaitText(scrollReadTitle(1))

        flow.scrollTimelineTo(scrollReadTitle(MID_PAGE_ARTICLE), down = true)
        flow.awaitScrollReadFlush()
        flow.scrollTimelineTo(SKIP_NEEDLE_TITLE, down = true)
        flow.scrollTimelineTo(scrollReadTitle(LAST_SCROLL_READ_ARTICLE), down = true)
    }

    /**
     * scroll read keeps earlier rows reachable: shared pagination on the desktop list (FACT scroll). As in
     * the Maestro flow, rows scrolled past and marked read stay in the same timeline: the flow scrolls back
     * up to the first article, then down past the first page to article 050.
     */
    @Test
    fun `REG-167 scroll read auto hide`() {
        val flow = e2e.start(Seed.PaginationScrollRead)
        flow.awaitText(scrollReadTitle(1))

        flow.scrollTimelineTo(scrollReadTitle(MID_PAGE_ARTICLE), down = true)
        flow.awaitScrollReadFlush()
        flow.scrollTimelineTo(scrollReadTitle(1), down = false)
        flow.scrollTimelineTo(SKIP_NEEDLE_TITLE, down = true)
    }

    /** Scrolls the timeline until a row showing [title] is on screen; up uses the topmost row as the anchor. */
    private fun Flow.scrollTimelineTo(title: String, down: Boolean): NodeSnapshotDto {
        fun found() = visibleArticleRows().firstOrNull { title in it.texts }
        repeat(MAX_TIMELINE_SCROLLS) {
            found()?.let { return it }
            val rows = visibleArticleRows()
            val anchor = (if (down) rows.lastOrNull() else rows.firstOrNull()) ?: return await(title) { found() }
            scroll(anchor, if (down) SCROLL_STEP else -SCROLL_STEP)
        }
        return await("row '$title' after $MAX_TIMELINE_SCROLLS scrolls") { found() }
    }

    /**
     * HomeViewModel writes scrolled-past rows as read only after SCROLL_READ_DEBOUNCE (2 s) without scrolling,
     * and the unread query then shrinks. Waiting past it, as the Maestro flows do, is what exposes a
     * pagination that skips rows; Spectre's idle wait settles the UI only.
     */
    private fun Flow.awaitScrollReadFlush() {
        Thread.sleep(SCROLL_READ_FLUSH_MS)
        idle()
    }

    private fun scrollReadTitle(index: Int) = "E2E Scroll Read Article ${index.toString().padStart(3, '0')}"

    /** The sidebar's import entry opens Settings with the import and export section expanded. */
    private fun Flow.openImportExport() {
        awaitTag(MAIN_SURFACE_TAG)
        openAddMenuItem(strings.importFeedButton)
        awaitTagIn(SETTINGS, ImportExportE2eIds.IMPORT_FEEDS)
    }

    private fun Flow.openAccounts() {
        awaitTag(MAIN_SURFACE_TAG)
        openSettings(strings.settingsAccounts)
    }

    private fun Flow.provider(account: SyncAccounts) = awaitTagIn(SETTINGS, AccountE2eIds.providerRow(account))

    private fun Flow.connectButton() = awaitTagIn(SETTINGS, AccountE2eIds.CONNECT_BUTTON)

    private fun Flow.backToServices() = clickIn(SETTINGS, strings.desktopAccountsBackToServices)

    private fun Flow.exportArticles(filter: String, fileName: String) {
        val file = File(homeDir, fileName)
        respondToFileDialog(file)
        click(awaitTagIn(SETTINGS, ImportExportE2eIds.EXPORT_ARTICLES))
        val dialog = strings.articlesExportFilterTitle
        awaitWindow(dialog)
        clickIn(dialog, filter)
        clickIn(dialog, strings.confirmButton)
        awaitTagIn(SETTINGS, ImportExportE2eIds.ARTICLE_EXPORT_SUCCESS)
        await("the exported file") { file.takeIf { it.isFile && it.length() > 0 } }
    }

    private fun Flow.largeRow(index: Int) = "article_row_e2e_large_article_" + index.toString().padStart(3, '0')

    private companion object {
        val SETTINGS = com.prof18.feedflow.i18n.EnFeedFlowStrings.settingsTitle
        const val MAIN_SURFACE_TAG = "desktop_main_window_surface"
        const val SEARCH_FIELD_TAG = "search-query"
        const val BACKUP_BUTTON_TAG = "account_backup_button"
        const val INVALID_OPML = "e2e/fixtures/opml/zz-feedflow-invalid-opml.xml"
        const val VALID_OPML = "feedflow-valid-opml-smoke.xml"
        const val ARTICLES_CSV = "e2e/fixtures/csv/feedflow-articles-smoke.csv"
        const val CSV_UNREAD = "E2E Imported CSV Unread Article"
        const val CSV_READ = "E2E Imported CSV Read Article"
        const val CSV_BOOKMARKED_UNREAD = "E2E Imported CSV Bookmarked Unread Article"
        const val CSV_BOOKMARKED_READ = "E2E Imported CSV Bookmarked Read Article"
        const val NEEDLE_TITLE = "E2E Large Article 045: Pagination Needle"
        const val PAST_FIRST_PAGE = 41
        const val MID_PAGE_ARTICLE = 35
        const val LAST_SCROLL_READ_ARTICLE = 90
        const val MAX_TIMELINE_SCROLLS = 150
        const val SCROLL_READ_FLUSH_MS = 4_000L
        const val SKIP_NEEDLE_TITLE = "E2E Scroll Read Article 050: Skip Needle"
        const val SCROLL_STEP = 30
    }
}
