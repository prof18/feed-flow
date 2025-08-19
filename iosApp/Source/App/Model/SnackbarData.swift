//
//  SnackbarData.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 29/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation

struct SnackbarData: Equatable {
    let title: String
    let subtitle: String?
    var showSnackbar: Bool

    init() {
        title = ""
        subtitle = nil
        showSnackbar = false
    }

    init(title: String, subtitle: String?, showBanner: Bool) {
        self.title = title
        self.subtitle = subtitle
        showSnackbar = showBanner
    }

    static func == (lhs: SnackbarData, rhs: SnackbarData) -> Bool {
        return lhs.subtitle == rhs.subtitle && lhs.title == rhs.title
    }
}

extension SnackbarData {
    func isEmpty() -> Bool {
        return title.isEmpty && subtitle == nil && showSnackbar == false
    }
}
