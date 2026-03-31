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
    private enum BrowserId {
        static let defaultBrowser = ""
        static let chrome = "googlechromes://"
        static let firefox = "firefox://open-url?url="
        static let firefoxFocus = "firefox-focus://open-url?url="
        static let duckDuckGo = "ddgQuickLink://"
        static let brave = "brave://open-url?url="
    }

    private let supportedBrowsers = [
        Browser(
            id: BrowserId.defaultBrowser,
            name: feedFlowStrings.defaultBrowser,
            isFavourite: false
        ),

        Browser(
            id: BrowserId.chrome,
            name: "Chrome",
            isFavourite: false
        ),

        Browser(
            id: BrowserId.firefox,
            name: "Firefox",
            isFavourite: false
        ),

        Browser(
            id: BrowserId.firefoxFocus,
            name: "Firefox Focus",
            isFavourite: false
        ),

        Browser(
            id: BrowserId.duckDuckGo,
            name: "DuckDuckGo",
            isFavourite: false
        ),

        Browser(
            id: BrowserId.brave,
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
            if browser.id == BrowserId.defaultBrowser {
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

            if let url = makeBrowserURL(
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
            selectedBrowser = browsers.first(where: { $0.id == BrowserId.defaultBrowser }) ?? browsers.first
        }
    }

    func openInAppBrowser() -> Bool {
        return currentSelectedBrowserId() == BrowserIds.shared.IN_APP_BROWSER
    }

    func openReaderMode(link: String) -> Bool {
        return settingsRepository.isUseReaderModeEnabled() && !link.contains("type=pdf")
            && !link.contains("youtube.com")
    }

    func getUrlForDefaultBrowser(stringUrl: String) -> URL {
        return makeBrowserURL(browserId: currentSelectedBrowserId(), stringUrl: stringUrl)
            ?? URL(string: stringUrl)
            ?? URL(fileURLWithPath: "")
    }

    func isValidForInAppBrowser(_ url: URL) -> Bool {
        return url.scheme == "http" || url.scheme == "https"
    }

    private func makeBrowserURL(browserId: String?, stringUrl: String) -> URL? {
        guard let url = URL(string: stringUrl) else {
            return nil
        }

        guard let browserId else {
            return url
        }

        switch browserId {
        case BrowserIds.shared.IN_APP_BROWSER, BrowserId.defaultBrowser:
            return url
        case BrowserId.chrome:
            return makeChromeURL(url: url)
        case BrowserId.firefox:
            return makeQueryBrowserURL(scheme: "firefox", targetUrl: stringUrl)
        case BrowserId.firefoxFocus:
            return makeQueryBrowserURL(scheme: "firefox-focus", targetUrl: stringUrl)
        case BrowserId.duckDuckGo:
            return makeQuickLinkBrowserURL(url: url, scheme: "ddgQuickLink")
        case BrowserId.brave:
            return makeQueryBrowserURL(scheme: "brave", targetUrl: stringUrl)
        default:
            return url
        }
    }

    private func currentSelectedBrowserId() -> String {
        settingsRepository.getFavouriteBrowserId() ?? selectedBrowser?.id ?? BrowserIds.shared.IN_APP_BROWSER
    }

    private func makeChromeURL(url: URL) -> URL? {
        guard var components = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            return nil
        }

        switch url.scheme {
        case "https":
            components.scheme = "googlechromes"
        case "http":
            components.scheme = "googlechrome"
        default:
            return nil
        }

        return components.url
    }

    private func makeQueryBrowserURL(scheme: String, targetUrl: String) -> URL? {
        guard let encodedTargetUrl = targetUrl.addingPercentEncoding(
            withAllowedCharacters: CharacterSet.urlBrowserQueryAllowed
        ) else {
            return nil
        }

        return URL(string: "\(scheme)://open-url?url=\(encodedTargetUrl)")
    }

    private func makeQuickLinkBrowserURL(url: URL, scheme: String) -> URL? {
        guard var components = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            return nil
        }

        switch url.scheme {
        case "https", "http":
            components.scheme = scheme
            return components.url
        default:
            return nil
        }
    }
}

private extension CharacterSet {
    static let urlBrowserQueryAllowed: CharacterSet = {
        var allowed = CharacterSet.alphanumerics
        allowed.insert(charactersIn: "-._~")
        return allowed
    }()
}
