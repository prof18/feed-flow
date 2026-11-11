//
//  SnackbarData.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 29/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation

struct SnackbarData: Equatable {
    let id = UUID()
    let title: String
    let subtitle: String?
    var showSnackbar: Bool
    let action: SnackbarAction?
    let duration: Duration

    init() {
        title = ""
        subtitle = nil
        showSnackbar = false
        action = nil
        duration = SnackbarData.defaultDuration
    }

    init(
        title: String,
        subtitle: String?,
        showBanner: Bool,
        action: SnackbarAction? = nil,
        duration: Duration = SnackbarData.defaultDuration
    ) {
        self.title = title
        self.subtitle = subtitle
        showSnackbar = showBanner
        self.action = action
        self.duration = duration
    }

    static let defaultDuration: Duration = .seconds(5)

    static func == (lhs: SnackbarData, rhs: SnackbarData) -> Bool {
        return lhs.subtitle == rhs.subtitle && lhs.title == rhs.title
    }
}

extension SnackbarData {
    func isEmpty() -> Bool {
        return title.isEmpty && subtitle == nil && showSnackbar == false
    }
}
