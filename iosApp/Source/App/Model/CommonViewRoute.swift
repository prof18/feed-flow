//
//  BrowserViewRoute.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 20/04/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import Foundation

enum CommonViewRoute: Hashable {
    case readerMode(url: URL)
    case search
    case accounts
    case dropboxSync
}
