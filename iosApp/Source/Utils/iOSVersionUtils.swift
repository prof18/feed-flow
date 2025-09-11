//
//  iOSVersionUtils.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 10/08/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import Foundation
import UIKit

func isiOS26OrLater() -> Bool {
    if #available(iOS 26.0, *) {
        return true
    } else {
        return false
    }
}
