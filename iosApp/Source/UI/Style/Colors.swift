//
//  Colors.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI

extension Color {
    static let colorOnBackground = Color("ColorOnBackground")
    static let secondaryBackgroundColor = Color(uiColor: .systemGroupedBackground)

    // The sidebar shares the feed list's background on every layout, so the selection pill looks the same
    // on iPhone and iPad and moving between sidebar and list doesn't change the surface.
    static let sidebarBackground = Color(uiColor: .systemBackground)
    static let sidebarSelection = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? .secondarySystemBackground
            : UIColor(red: 0.906, green: 0.906, blue: 0.914, alpha: 1)
    })
    static let sidebarSelectionBorder = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? .tertiarySystemBackground
            : UIColor(red: 0.859, green: 0.859, blue: 0.871, alpha: 1)
    })
}
