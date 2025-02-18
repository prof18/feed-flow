//
//  CategoriesSection.swift
//  FeedFlow
//
//  Created by AI Assistant on 19/03/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct CategoriesSection: View {
    let categories: [DrawerItem]
    let onSelect: (DrawerItem) -> Void
    let onFeedFilterSelected: (FeedFilter) -> Void

    var body: some View {
        if !categories.isEmpty {
            Section(
                content: {
                    ForEach(categories, id: \.self) { drawerItem in
                        if let categoryItem = drawerItem as? DrawerItem.DrawerCategory {
                            HStack {
                                Label(categoryItem.category.title, systemImage: "tag")
                                Spacer()
                                if categoryItem.unreadCount > 0 {
                                    Text("\(categoryItem.unreadCount)")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                        .padding(.horizontal, 8)
                                        .background(Color.secondary.opacity(0.2))
                                        .clipShape(Capsule())
                                }
                            }
                            .contentShape(Rectangle())
                            .onTapGesture {
                                onSelect(categoryItem)
                                onFeedFilterSelected(FeedFilter.Category(feedCategory: categoryItem.category))
                            }
                        }
                    }
                },
                header: {
                    Text(feedFlowStrings.drawerTitleCategories)
                }
            )
        }
    }
}
