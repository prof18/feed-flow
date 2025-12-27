//
//  GoogleDriveErrors.swift
//  FeedFlow
//
//  Created by Claude on 05/11/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import Foundation

enum GoogleDriveErrors: Error {
    case uploadError(reason: String)
    case downloadError(reason: String)
}
