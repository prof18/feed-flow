//
//  DelayedLoadingPhase.swift
//  FeedFlow
//
//  Copyright © 2026 FeedFlow. All rights reserved.
//

/// What a screen renders while its data loads. Branch on this instead of on the loading flag,
/// so a fast load shows neither the empty state nor a spinner.
enum DelayedLoadingPhase {
    /// Still loading, but not for long enough to show a spinner: render nothing.
    case pending
    case spinner
    case ready
}
