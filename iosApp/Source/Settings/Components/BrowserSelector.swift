//
//  BrowserSelector.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 12/07/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import UIKit

@Observable
class BrowserSelector {
    private let supportedBrowsers = [
        Browser(
            id: BrowserURLBuilder.BrowserId.defaultBrowser,
            name: feedFlowStrings.defaultBrowser,
            isFavourite: false
        ),

        Browser(
            id: BrowserURLBuilder.BrowserId.chrome,
            name: "Chrome",
            isFavourite: false
        ),

        Browser(
            id: BrowserURLBuilder.BrowserId.firefox,
            name: "Firefox",
            isFavourite: false
        ),

        Browser(
            id: BrowserURLBuilder.BrowserId.firefoxFocus,
            name: "Firefox Focus",
            isFavourite: false
        ),

        Browser(
            id: BrowserURLBuilder.BrowserId.duckDuckGo,
            name: "DuckDuckGo",
            isFavourite: false
        ),

        Browser(
            id: BrowserURLBuilder.BrowserId.brave,
            name: "Brave",
            isFavourite: false
        )
    ]

    private let settingsRepository = Deps.shared.getSettingsRepository()

    var browsers: [Browser] = []
    var selectedBrowser: Browser? {
        didSet {
            if let browserId = selectedBrowser?.id {
                settingsRepository.saveFavouriteBrowserId(browserId: browserId)
            }
        }
    }

    init() {
        let favouriteBrowser = settingsRepository.getFavouriteBrowserId()

        var available = [
            Browser(
                id: BrowserIds.shared.IN_APP_BROWSER,
                name: feedFlowStrings.inAppBrowser,
                isFavourite: false
            )
        ]

        for browser in supportedBrowsers {
            if browser.id == BrowserURLBuilder.BrowserId.defaultBrowser {
                available.append(browser)
                continue
            }

            if let url = BrowserURLBuilder.url(
                browserId: browser.id,
                stringUrl: "https://www.example.com"
            ), UIApplication.shared.canOpenURL(url) {
                available.append(browser)
            }
        }

        // The picker shows the browser links actually open in, even for an uninstalled favourite.
        let resolvedId = BrowserResolver.shared.linksBrowserId(
            favouriteId: favouriteBrowser,
            availableIds: available.map(\.id)
        )
        browsers = available.map { browser in
            Browser(id: browser.id, name: browser.name, isFavourite: browser.id == resolvedId)
        }
        selectedBrowser = browsers.first { $0.id == resolvedId }
    }

    func openInAppBrowser() -> Bool {
        return currentSelectedBrowserId() == BrowserIds.shared.IN_APP_BROWSER
    }

    func articleOpenMode() -> ArticleOpenMode {
        return settingsRepository.getArticleOpenMode()
    }

    /// Mirrors the shared resolution: per-feed value falls back to the global default, and a reader
    /// mode falls back to the favourite browser when the page cannot be parsed.
    func resolvedOpenMode(for urlInfo: FeedItemUrlInfo) -> ArticleOpenMode {
        return ArticleOpenModeResolver.shared.resolve(urlInfo: urlInfo, globalDefault: articleOpenMode())
    }

    func isReaderModeEligible(link: String) -> Bool {
        return ReaderModeEligibility.shared.canOpenReaderMode(url: link)
    }

    func getUrlForDefaultBrowser(stringUrl: String) -> URL {
        return BrowserURLBuilder.url(browserId: currentSelectedBrowserId(), stringUrl: stringUrl)
            ?? URL(string: stringUrl)
            ?? URL(fileURLWithPath: "")
    }

    /// The browser that opens articles; it falls back to the links browser when unset or uninstalled.
    func articleBrowserId() -> String {
        return BrowserResolver.shared.articleBrowserId(
            articleBrowserId: settingsRepository.getArticleBrowserId(),
            favouriteId: settingsRepository.getFavouriteBrowserId(),
            availableIds: installedBrowserIds()
        )
    }

    func opensArticleInApp(for mode: ArticleOpenMode) -> Bool {
        return mode == .internalBrowser || articleBrowserId() == BrowserIds.shared.IN_APP_BROWSER
    }

    func getUrlForArticleBrowser(stringUrl: String) -> URL {
        return BrowserURLBuilder.url(browserId: articleBrowserId(), stringUrl: stringUrl)
            ?? URL(string: stringUrl)
            ?? URL(fileURLWithPath: "")
    }

    func articleBrowserName() -> String {
        let resolvedId = articleBrowserId()
        return browsers.first { $0.id == resolvedId }?.name ?? feedFlowStrings.defaultBrowser
    }

    func isValidForInAppBrowser(_ url: URL) -> Bool {
        return url.scheme == "http" || url.scheme == "https"
    }

    /// Rechecked on every lookup: a browser can be uninstalled while the app keeps running.
    private func installedBrowserIds() -> [String] {
        browsers.map(\.id).filter { id in
            if id == BrowserIds.shared.IN_APP_BROWSER || id == BrowserURLBuilder.BrowserId.defaultBrowser {
                return true
            }
            guard let url = BrowserURLBuilder.url(browserId: id, stringUrl: "https://www.example.com") else {
                return false
            }
            return UIApplication.shared.canOpenURL(url)
        }
    }

    private func currentSelectedBrowserId() -> String {
        BrowserResolver.shared.linksBrowserId(
            favouriteId: settingsRepository.getFavouriteBrowserId(),
            availableIds: installedBrowserIds()
        )
    }
}
