//
//  DelayedProgressView.swift
//  FeedFlow
//
//  Copyright © 2026 FeedFlow. All rights reserved.
//

import SwiftUI

/// A spinner that stays invisible for the first 300 ms, so a state that resolves quickly doesn't flash it.
struct DelayedProgressView: View {
    @State private var isVisible = false

    var body: some View {
        ProgressView()
            .opacity(isVisible ? 1 : 0)
            .task {
                try? await Task.sleep(for: .milliseconds(300))
                guard !Task.isCancelled else { return }
                isVisible = true
            }
    }
}
