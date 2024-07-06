//
//  CustomNotification.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 29/06/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import Foundation

extension Notification.Name {
    static let didDropboxSuccess = Notification.Name("didDropboxSuccess")
    static let didDropboxCancel = Notification.Name("didDropboxCancel")
    static let didDropboxError = Notification.Name("didDropboxError")
}
