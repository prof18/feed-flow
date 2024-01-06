//
//  NavigationExtensions.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 05/01/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

extension View {
    func withSheetDestinations(sheetDestinations: Binding<SheetDestination?>) -> some View {
        sheet(item: sheetDestinations) { destination in
            Group {
                switch destination {
                case .settings:
                    SettingsScreen()
                }
            }
        }
    }
}
