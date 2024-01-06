//
//  SheetToShow.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation

enum HomeSheetToShow: Identifiable {
    case feedList
    case settings

    var id: Int {
        hashValue
    }
}
