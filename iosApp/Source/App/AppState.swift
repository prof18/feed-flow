//
//  AppState.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 29/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Collections
import FeedFlowKit
import Foundation
import SwiftUI

@Observable
class AppState {
    var snackbarQueue: Deque<SnackbarData> = Deque()
    var snackbarQueueForSheet: Deque<SnackbarData> = Deque()
    var regularNavigationPath = NavigationPath()
    var colorScheme: ColorScheme?

    var redrawAfterFeedSourceEdit = false
    var pendingBrowserURL: URL?

    func navigate(route: any Hashable) {
        regularNavigationPath.append(route)
    }

    func openInAppBrowser(url: URL) {
        pendingBrowserURL = url
    }

    func emitGenericError() {
        snackbarQueue.append(
            SnackbarData(
                title: feedFlowStrings.genericErrorMessage,
                subtitle: nil,
                showBanner: true
            )
        )
    }

    func updateTheme(_ themeMode: ThemeMode) {
        switch themeMode {
        case .light:
            colorScheme = .light
        case .dark:
            colorScheme = .dark
        case .oled:
            colorScheme = .dark
        case .system:
            colorScheme = nil
        }
    }
}
