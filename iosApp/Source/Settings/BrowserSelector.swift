//
//  BrowserSelector.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 12/07/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import shared
import UIKit

class BrowserSelector: ObservableObject {
    
    private let supportedBrowsers = [
        Browser(
            id: "",
            name: MR.strings().default_browser.localized,
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
    
    private let feedManagerRepository = KotlinDependencies.shared.getFeedManagerRepository()
    
    @Published var browsers: [Browser] = []
    @Published var selectedBrowser: Browser? {
        didSet {
            feedManagerRepository.setFavouriteBrowser(browser: selectedBrowser!)
        }
    }
    
    init() {
            
        let favouriteBrowser = feedManagerRepository.getFavouriteBrowserId()
                
        for (index, browser) in supportedBrowsers.enumerated() {
            let stringUrl = "\(browser.id)https://www.example.com"
            
            if let url = URL(string: stringUrl), UIApplication.shared.canOpenURL(url) {
                var isFavourite = browser.id == favouriteBrowser
                if favouriteBrowser == nil {
                    isFavourite = index == 0
                }
                
                let updatedBrowser = Browser(
                    id: browser.id,
                    name: browser.name,
                    isFavourite: isFavourite
                )
                
                browsers.append(updatedBrowser)
                if isFavourite {
                    selectedBrowser = updatedBrowser
                }
            }
        }
        
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
