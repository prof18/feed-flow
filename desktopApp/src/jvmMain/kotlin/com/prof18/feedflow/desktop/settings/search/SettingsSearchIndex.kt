package com.prof18.feedflow.desktop.settings.search

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.accounts.desktopTitle
import com.prof18.feedflow.desktop.accounts.isBackupProvider
import com.prof18.feedflow.desktop.settings.DesktopSettingsCategory
import com.prof18.feedflow.i18n.FeedFlowStrings
import java.text.Normalizer
import java.util.Locale

/**
 * Every searchable title in the desktop Settings window. Only the selected pane is composed, so rows cannot
 * register themselves: this list mirrors the panes and must change with them.
 */
@Suppress("LongMethod")
internal fun settingsSearchIndex(
    strings: FeedFlowStrings,
    supportedAccounts: List<SyncAccounts>,
    isCrashReportingAvailable: Boolean,
    isGridToggleVisible: Boolean,
    isFaqEnabled: Boolean,
): List<SettingsSearchEntry> = buildList {
    fun page(category: DesktopSettingsCategory, pageTitle: String, title: String = pageTitle) =
        add(SettingsSearchEntry(category, title, pageTitle))

    fun rows(category: DesktopSettingsCategory, pageTitle: String, section: String?, vararg titles: String) =
        titles.forEach { add(SettingsSearchEntry(category, it, pageTitle, section, targetTitle = it)) }

    fun cardRows(category: DesktopSettingsCategory, pageTitle: String, section: String, vararg titles: String) =
        titles.forEach { add(SettingsSearchEntry(category, it, pageTitle, section, targetTitle = section)) }

    val general = strings.settingsAppTitle
    page(DesktopSettingsCategory.GENERAL, general)
    rows(
        DesktopSettingsCategory.GENERAL,
        general,
        strings.settingsAppearance,
        strings.settingsTheme,
        strings.settingsDesktopMultiPaneLayout,
        strings.settingsHideUnreadCount,
        strings.settingsReduceMotion,
        strings.settingsForceEnglish,
    )

    val accounts = strings.settingsAccounts
    page(DesktopSettingsCategory.ACCOUNTS, accounts)
    supportedAccounts.filter { it != SyncAccounts.LOCAL }.distinct().forEach { account ->
        val section = if (account.isBackupProvider()) {
            strings.desktopAccountsBackupServicesTitle
        } else {
            strings.desktopAccountsReaderServicesTitle
        }
        add(SettingsSearchEntry(DesktopSettingsCategory.ACCOUNTS, account.desktopTitle(strings), accounts, section))
    }

    val feedList = strings.settingsFeedListTitle
    page(DesktopSettingsCategory.FEED_LIST, feedList)
    rows(DesktopSettingsCategory.FEED_LIST, feedList, strings.settingsAppearance, strings.feedLayoutTitle)
    val feedAppearance = strings.settingsAppearance
    if (isGridToggleVisible) {
        rows(DesktopSettingsCategory.FEED_LIST, feedList, feedAppearance, strings.settingsFeedLayoutGridToggle)
    }
    cardRows(DesktopSettingsCategory.FEED_LIST, feedList, feedAppearance, strings.settingsFeedListScaleTitle)
    rows(DesktopSettingsCategory.FEED_LIST, feedList, strings.settingsAppearance, strings.settingsDescriptionMaxLines)
    rows(
        DesktopSettingsCategory.FEED_LIST,
        feedList,
        null,
        strings.settingsHideDescription,
        strings.settingsHideImages,
        strings.settingsHideDate,
        strings.settingsHideUnreadDot,
        strings.settingsHideFeedSource,
        strings.settingsHideDuplicatedTitleFromDesc,
        strings.dateFormatTitle,
        strings.timeFormatTitle,
    )
    rows(
        DesktopSettingsCategory.FEED_LIST,
        feedList,
        strings.settingsReadingBehavior,
        strings.settingsToggleShowReadArticles,
        strings.settingsHideReadItems,
        strings.settingsFeedOrderTitle,
        strings.settingsLeftSwipeAction,
        strings.settingsRightSwipeAction,
    )

    val reading = strings.settingsReadingBehavior
    page(DesktopSettingsCategory.READING, reading)
    rows(
        DesktopSettingsCategory.READING,
        reading,
        null,
        strings.articleOpenMode,
        strings.settingsSaveReaderModeContent,
        strings.settingsPrefetchArticleContent,
        strings.toggleMarkReadWhenScrolling,
    )
    cardRows(
        DesktopSettingsCategory.READING,
        reading,
        strings.readerModeTextSettings,
        strings.readerModeFont,
        strings.readerModeFontSize,
        strings.readerModeTitleFontSize,
        strings.readerModeLineHeight,
    )

    val filters = strings.settingsFilters
    page(DesktopSettingsCategory.FILTERS, filters)
    page(DesktopSettingsCategory.FILTERS, filters, strings.settingsBlockedWords)

    val syncStorage = strings.settingsSyncAndStorage
    page(DesktopSettingsCategory.SYNC_STORAGE, syncStorage)
    rows(
        DesktopSettingsCategory.SYNC_STORAGE,
        syncStorage,
        null,
        strings.settingsRefreshFeedsOnLaunch,
        strings.settingsDesktopRefreshWhileOpen,
        strings.settingsAutoDelete,
        strings.importExportLabel,
    )
    // The mobile apps call the same setting "Background sync"; keep that wording searchable on desktop.
    add(
        SettingsSearchEntry(
            DesktopSettingsCategory.SYNC_STORAGE,
            strings.settingsSyncPeriod,
            syncStorage,
            targetTitle = strings.settingsDesktopRefreshWhileOpen,
        ),
    )
    rows(
        DesktopSettingsCategory.SYNC_STORAGE,
        syncStorage,
        strings.settingsDangerTitle,
        strings.clearOldArticlesButton,
        strings.settingsClearDownloadedArticles,
        strings.settingsClearImageCache,
    )

    val about = strings.settingsAboutAndSupport
    page(DesktopSettingsCategory.ABOUT, about)
    rows(DesktopSettingsCategory.ABOUT, about, strings.settingsHelpTitle, strings.reportIssueButton)
    if (isFaqEnabled) {
        rows(DesktopSettingsCategory.ABOUT, about, strings.settingsHelpTitle, strings.aboutMenuFaq)
    }
    rows(
        DesktopSettingsCategory.ABOUT,
        about,
        strings.settingsHelpTitle,
        strings.openWebsiteButton,
        strings.aboutMenuContributeTranslations,
    )
    if (isCrashReportingAvailable) {
        rows(DesktopSettingsCategory.ABOUT, about, null, strings.settingsCrashReporting)
    }
    rows(DesktopSettingsCategory.ABOUT, about, null, "${strings.authorLabel} Marco Gomiero", strings.openSourceLicenses)
}

internal fun List<SettingsSearchEntry>.search(query: String): List<SettingsSearchEntry> {
    val needle = normalizeForSearch(query.trim())
    if (needle.isEmpty()) return emptyList()
    val byTitle = filter { normalizeForSearch(it.title).contains(needle) }
    val bySectionOrPage = filter { entry ->
        entry !in byTitle &&
            listOfNotNull(entry.sectionTitle, entry.pageTitle).any { normalizeForSearch(it).contains(needle) }
    }
    return byTitle + bySectionOrPage
}

internal fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)

private val COMBINING_MARKS = Regex("\\p{M}+")

internal data class SettingsSearchEntry(
    val category: DesktopSettingsCategory,
    val title: String,
    val pageTitle: String,
    val sectionTitle: String? = null,
    // Title of the row or SettingsSection to highlight; null only opens the page.
    val targetTitle: String? = null,
)
