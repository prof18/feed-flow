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

class AppState: ObservableObject {

    @Published var snackbarQueue: Deque<SnackbarData> = Deque()
    @Published var snackbarQueueForSheet: Deque<SnackbarData> = Deque()
    @Published var regularNavigationPath = NavigationPath()
    @Published var compatNavigationPath = NavigationPath()
    @Published var sizeClass: UserInterfaceSizeClass?

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
