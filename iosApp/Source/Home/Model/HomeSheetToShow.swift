//
//  HomeSheetToShow.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation

enum HomeSheetToShow: Identifiable, Hashable {
    case settings
    case noFeedSource
    case addFeed
    case importExport
    case editFeed(FeedSource)

    var id: Int {
        hashValue
    }
}
