//
//  BrowserToPresent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 17/04/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import Foundation

enum BrowserToPresent: Hashable, Identifiable {
    var id: Self {
        return self
    }

    case inAppBrowser(url: URL)
}
