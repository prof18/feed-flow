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

        var isInAppBrowserFavourite = false
        if favouriteBrowser == nil {
            isInAppBrowserFavourite = true
        } else {
            isInAppBrowserFavourite = favouriteBrowser == BrowserIds.shared.IN_APP_BROWSER
        }

        let inAppBrowser = Browser(
            id: BrowserIds.shared.IN_APP_BROWSER,
            name: feedFlowStrings.inAppBrowser,
            isFavourite: isInAppBrowserFavourite
        )
        browsers.append(inAppBrowser)
        if isInAppBrowserFavourite {
            selectedBrowser = inAppBrowser
        }

        for browser in supportedBrowsers {
            if browser.id == BrowserURLBuilder.BrowserId.defaultBrowser {
                let updatedBrowser = Browser(
                    id: browser.id,
                    name: browser.name,
                    isFavourite: browser.id == favouriteBrowser
                )

                browsers.append(updatedBrowser)
                if updatedBrowser.isFavourite {
                    selectedBrowser = updatedBrowser
                }
                continue
            }

            if let url = BrowserURLBuilder.url(
                browserId: browser.id,
                stringUrl: "https://www.example.com"
            ), UIApplication.shared.canOpenURL(url) {
                let updatedBrowser = Browser(
                    id: browser.id,
                    name: browser.name,
                    isFavourite: browser.id == favouriteBrowser
                )

                browsers.append(updatedBrowser)
                if updatedBrowser.isFavourite {
                    selectedBrowser = updatedBrowser
                }
            }
        }

        if selectedBrowser == nil {
            selectedBrowser = browsers.first { $0.id == BrowserURLBuilder.BrowserId.defaultBrowser }
                ?? browsers.first
        }
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

    func isValidForInAppBrowser(_ url: URL) -> Bool {
        return url.scheme == "http" || url.scheme == "https"
    }

    private func currentSelectedBrowserId() -> String {
        settingsRepository.getFavouriteBrowserId() ?? selectedBrowser?.id ?? BrowserIds.shared.IN_APP_BROWSER
    }
}
