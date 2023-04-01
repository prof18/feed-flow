//
//  SheetToShow.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation

enum SheetToShow: Identifiable {
    case filePicker

    var id: Int {
        hashValue
    }
}
