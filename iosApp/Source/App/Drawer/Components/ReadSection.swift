//
//  ReadSection.swift
//  FeedFlow
//
//  Created by AI Assistant on 19/03/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct ReadSection: View {
    let read: [DrawerItem]
    let onSelect: (DrawerItem) -> Void
    let onFeedFilterSelected: (FeedFilter) -> Void

    var body: some View {
        ForEach(read, id: \.self) { drawerItem in
            HStack {
                Label(feedFlowStrings.drawerTitleRead, systemImage: "text.badge.checkmark")
                Spacer()
            }
            .contentShape(Rectangle())
            .onTapGesture {
                onSelect(drawerItem)
                onFeedFilterSelected(FeedFilter.Read())
            }
        }
    }
}
