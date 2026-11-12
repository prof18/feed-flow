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

    private var visibleSnapshotKey: [VisibleFeedItemSnapshot] = []

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

    func isVisible(id: String) -> Bool {
        visibleSnapshotKey.contains { $0.id == id }
    }

    func refresh() {
        visibleSnapshotKey.removeAll()
    }

    func clear() {
        visibleSnapshotKey.removeAll()
    }

    func visibleItemsChanged(_ visibleItems: [VisibleFeedItemSnapshot]) {
        guard visibleItems != visibleSnapshotKey else { return }
        visibleSnapshotKey = visibleItems
        let firstVisibleIndex = visibleItems.map(\.index).min() ?? 0
        homeViewModel?.onFeedListScrolledAwayFromTopChanged(isAwayFromTop: firstVisibleIndex > awayFromTopThreshold)
        guard let homeViewModel else { return }
        homeViewModel.onVisibleFeedItemsChanged(
            visibleItems: visibleItems.map { VisibleFeedItem(id: $0.id, index: Int32($0.index)) }
        )
    }
}

private let awayFromTopThreshold = 3
