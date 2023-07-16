//
//  SheetToShow.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation

enum SheetToShow: Identifiable {
    case filePicker
    case feedList
    case shareSheet

    var id: Int {
        hashValue
    }
}
