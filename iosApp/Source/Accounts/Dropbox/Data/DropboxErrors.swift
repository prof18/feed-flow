//
//  DropboxErrors.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 29/06/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import Foundation

enum DropboxErrors: Error {
    case uploadError(reason: String)
    case downloadError(reason: String)
}
