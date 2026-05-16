//
//  HomeViewOptionsSheet.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 14/05/26.
//  Copyright © 2026. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct HomeViewOptionsSheet: View {
    @Environment(\.dismiss)
    private var dismiss

    let state: HomeViewMenuState
    let onFeedOrderChange: (FeedOrder) -> Void
    let onShowReadArticlesTimelineChange: (Bool) -> Void

    var body: some View {
        let orderBinding = Binding(
            get: { state.feedOrder },
            set: { onFeedOrderChange($0) }
        )
        let showReadBinding = Binding(
            get: { state.showReadArticlesTimeline },
            set: { onShowReadArticlesTimelineChange($0) }
        )

        NavigationStack {
            Form {
                Section {
                    Picker(selection: orderBinding) {
                        Text(feedFlowStrings.settingsFeedOrderNewestFirst)
                            .tag(FeedOrder.newestFirst)
                        Text(feedFlowStrings.settingsFeedOrderOldestFirst)
                            .tag(FeedOrder.oldestFirst)
                    } label: {
                        Text(feedFlowStrings.settingsFeedOrderTitle)
                    }

                    Toggle(isOn: showReadBinding) {
                        Text(feedFlowStrings.settingsToggleShowReadArticles)
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(Color.secondaryBackgroundColor)
            .navigationTitle(feedFlowStrings.sortAndFilterButton)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        self.dismiss()
                    } label: {
                        Text(feedFlowStrings.actionDone).bold()
                    }
                }
            }
        }
        .presentationDetents([.medium])
        .presentationBackground(Color.secondaryBackgroundColor)
    }
}
