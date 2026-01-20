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

    @ObservationIgnored var lastAppearedIndex = 0

    private var lastReadIndex = 0
    private var timer: Timer?
    private var updatesBlocked = false

    init(homeViewModel: HomeViewModel) {
        self.homeViewModel = homeViewModel
    }

    init(fakeHomeViewModel _: Bool) {
        // The variable is just to remind not to declare empty constructors by mistake
        homeViewModel = nil
    }

    func getLastReadIndex() -> Int {
        return lastReadIndex
    }

    func pauseUpdates() {
        updatesBlocked = true
        timer?.invalidate()
    }

    func resumeUpdates() {
        updatesBlocked = false
    }

    func refresh() {
        timer?.invalidate()
        lastReadIndex = 0
    }

    func clear() {
        lastReadIndex = 0
        timer?.invalidate()
        pauseUpdates()
    }

    func updateReadIndex(index: Int) {
        guard !updatesBlocked, shouldUpdateReadIndex(index: index) else { return }
        lastReadIndex = index
        scheduleReadMark()
    }

    private func shouldUpdateReadIndex(index: Int) -> Bool {
        index < lastAppearedIndex && index > lastReadIndex
    }

    private func scheduleReadMark() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 2, repeats: false) { [weak self] _ in
            guard let self else { return }
            self.homeViewModel?.markAsReadOnScroll(lastVisibleIndex: Int32(self.getLastReadIndex()))
        }
    }
}
