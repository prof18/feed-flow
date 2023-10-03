//
//  AppState.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 29/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation
import Collections

class AppState: ObservableObject {

    @Published var snackbarQueue: Deque<SnackbarData> = Deque()

    @Published var snackbarQueueForSheet: Deque<SnackbarData> = Deque()

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
