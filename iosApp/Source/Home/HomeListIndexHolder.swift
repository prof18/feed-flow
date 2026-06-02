//
//  HomeListIndexHolder.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 02/04/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation

@Observable
class HomeListIndexHolder {
    @ObservationIgnored let homeViewModel: HomeViewModel?

    private var visibleSnapshotKey: [VisibleFeedItemSnapshotKey] = []

    init(homeViewModel: HomeViewModel) {
        self.homeViewModel = homeViewModel
    }

    init(fakeHomeViewModel _: Bool) {
        // The variable is just to remind not to declare empty constructors by mistake
        homeViewModel = nil
    }

    func getLastReadIndex() -> Int {
        return visibleSnapshotKey.map(\.index).min() ?? 0
    }

    func refresh() {
        visibleSnapshotKey.removeAll()
    }

    func clear() {
        visibleSnapshotKey.removeAll()
    }

    func visibleItemsChanged(_ visibleItems: [VisibleFeedItem]) {
        let snapshotKey = visibleItems.map { item in
            VisibleFeedItemSnapshotKey(
                id: item.id,
                index: Int(item.index)
            )
        }
        guard snapshotKey != visibleSnapshotKey else { return }
        visibleSnapshotKey = snapshotKey
        homeViewModel?.onVisibleFeedItemsChanged(visibleItems: visibleItems)
    }
}

private struct VisibleFeedItemSnapshotKey: Equatable {
    let id: String
    let index: Int
}
