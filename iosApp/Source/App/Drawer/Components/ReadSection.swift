//
//  ReadSection.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 19/03/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct ReadSection: View {
    let read: [DrawerItem]
    let isSelected: Bool
    let isCompact: Bool
    let onSelect: () -> Void
    let onFeedFilterSelected: (FeedFilter) -> Void

    var body: some View {
        if !read.isEmpty {
            HStack {
                Label(feedFlowStrings.drawerTitleRead, systemImage: "text.badge.checkmark")
                Spacer()
            }
            .contentShape(Rectangle())
            .onTapGesture {
                onSelect()
                onFeedFilterSelected(FeedFilter.Read())
            }
            .tag(SidebarSelection.read)
            .listRowBackground(sidebarSelectionBackground(isSelected: isSelected, isCompact: isCompact))
        }
    }
}
