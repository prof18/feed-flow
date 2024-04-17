//
//  BrowserSelector.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 12/07/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation
import shared
import UIKit

class BrowserSelector: ObservableObject {

    private let supportedBrowsers = [
        Browser(
            id: "",
            name: feedFlowStrings.defaultBrowser,
            isFavourite: false
        ),

        Browser(
            id: "googlechromes://",
            name: "Chrome",
            isFavourite: false
        ),

        Browser(
            id: "firefox://open-url?url=",
            name: "Firefox",
            isFavourite: false
        ),

        Browser(
            id: "firefox-focus://open-url?url=",
            name: "Firefox Focus",
            isFavourite: false
        ),

        Browser(
            id: "ddgQuickLink://",
            name: "DuckDuckGo",
            isFavourite: false
        ),

        Browser(
            id: "brave://open-url?url=",
            name: "Brave",
            isFavourite: false
        )
    ]

    private let browserSettingsRepository = KotlinDependencies.shared.getBrowserSettingsRepository()

    @Published var browsers: [Browser] = []
    @Published var selectedBrowser: Browser? {
        didSet {
            browserSettingsRepository.setFavouriteBrowser(browser: selectedBrowser!)
        }
    }

    init() {

        let favouriteBrowser = browserSettingsRepository.getFavouriteBrowserId()

        var isFavourite = false
        if favouriteBrowser == nil {
            isFavourite = true
        } else {
            isFavourite = favouriteBrowser == BrowserIds.shared.IN_APP_BROWSER
        }

        let inAppBrowser =  Browser(
            id: BrowserIds.shared.IN_APP_BROWSER,
            name: feedFlowStrings.inAppBrowser,
            isFavourite: isFavourite
        )
        browsers.append(inAppBrowser)
        if isFavourite {
            selectedBrowser = inAppBrowser
        }

        supportedBrowsers.forEach { browser in
            let stringUrl = "\(browser.id)https://www.example.com"

            if let url = URL(string: stringUrl), UIApplication.shared.canOpenURL(url) {
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
    }

    func openInAppBrowser() -> Bool {
        return selectedBrowser?.id == BrowserIds.shared.IN_APP_BROWSER
    }

    func getUrlForDefaultBrowser(stringUrl: String) -> URL {
        let url: String
        if let selectedBrowser {
            if selectedBrowser.id == "googlechromes://" {
                url = "\(selectedBrowser.id)\(stringUrl.replacingOccurrences(of: "https://", with: ""))"
            } else {
                url = "\(selectedBrowser.id)\(stringUrl)"
            }
            return URL(string: url)!
        } else {
            return URL(string: stringUrl)!
        }
    }
}
