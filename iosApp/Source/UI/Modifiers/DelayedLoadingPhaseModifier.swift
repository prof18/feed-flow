//
//  DelayedLoadingPhaseModifier.swift
//  FeedFlow
//
//  Copyright © 2026 FeedFlow. All rights reserved.
//

import SwiftUI

/// Mirrors the shared `toLoadingPhase` timing: nothing for 300 ms, then a spinner that stays
/// at least 500 ms once it appears.
struct DelayedLoadingPhaseModifier: ViewModifier {
    let isLoading: Bool
    @Binding var phase: DelayedLoadingPhase

    @State private var spinnerShownAt: ContinuousClock.Instant?

    private let showAfter: Duration = .milliseconds(300)
    private let minVisible: Duration = .milliseconds(500)

    func body(content: Content) -> some View {
        content
            .task(id: isLoading) {
                if isLoading {
                    await showSpinnerAfterDelay()
                } else {
                    await hideSpinnerAfterMinimumTime()
                }
            }
    }

    private func showSpinnerAfterDelay() async {
        if spinnerShownAt == nil {
            phase = .pending
            try? await Task.sleep(for: showAfter)
            guard !Task.isCancelled else { return }
            spinnerShownAt = .now
        }
        phase = .spinner
    }

    private func hideSpinnerAfterMinimumTime() async {
        if let spinnerShownAt {
            let remaining = minVisible - (ContinuousClock.now - spinnerShownAt)
            if remaining > .zero {
                try? await Task.sleep(for: remaining)
                guard !Task.isCancelled else { return }
            }
        }
        spinnerShownAt = nil
        phase = .ready
    }
}

extension View {
    func delayedLoadingPhase(isLoading: Bool, phase: Binding<DelayedLoadingPhase>) -> some View {
        modifier(DelayedLoadingPhaseModifier(isLoading: isLoading, phase: phase))
    }
}
