//
//  CommonViewRoute.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 20/04/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation

enum CommonViewRoute: Hashable {
    case feedSuggestions
    case readerMode
    case search
    case accounts
    case dropboxSync
    case deepLinkFeed(String)
    case inAppBrowser(url: URL)
}
