//
//  AppState.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 29/03/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation

class AppState: ObservableObject {

    @Published var snackbarData: SnackbarData = SnackbarData()

    @Published var snackbarDataForSheet: SnackbarData = SnackbarData()
}
