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

    @Published
    var snackbarQueue: Deque<SnackbarData> = Deque()

    @Published
    var snackbarQueueForSheet: Deque<SnackbarData> = Deque()

    @Published
    var path = NavigationPath()

    @Published var sizeClass: UserInterfaceSizeClass?

    init() {
        path.append(CompactViewRoute.feed)
    }

    func navigate(route: any Hashable) {
        path.append(route)
    }

    func emitGenericError() {
        snackbarQueue.append(
            SnackbarData(
                title: localizer.generic_error_message.localized,
                subtitle: nil,
                showBanner: true
            )
        )
    }
}
