//
//  ImportExportSheet.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/09/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation

enum ImportExportSheetToShow: Identifiable {
    case filePicker
    case shareSheet

    var id: Int {
        hashValue
    }
}
