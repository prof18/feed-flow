//
//  AppState.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 29/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation
import Collections
import SwiftUI

@Observable class AppState {

    var snackbarQueue: Deque<SnackbarData> = Deque()
    var snackbarQueueForSheet: Deque<SnackbarData> = Deque()
    var regularNavigationPath = NavigationPath()
    var compatNavigationPath = NavigationPath()
    var sizeClass: UserInterfaceSizeClass?

    var redrawAfterFeedSourceEdit: Bool = false

    init() {
        compatNavigationPath.append(CompactViewRoute.feed)
    }

    func navigate(route: any Hashable) {
        if sizeClass == .compact {
            compatNavigationPath.append(route)
        } else {
            regularNavigationPath.append(route)
        }
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
}
