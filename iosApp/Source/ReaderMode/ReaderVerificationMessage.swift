import FeedFlowKit
import SwiftUI

/// Mirrors the Android reader: a failed save or verification is shown above the article.
struct ReaderVerificationMessage: ViewModifier {
    let state: ReaderVerificationState?

    func body(content: Content) -> some View {
        content.overlay(alignment: .top) {
            if let message {
                Text(message)
                    .font(.footnote)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(.regularMaterial)
                    .accessibilityIdentifier(ReaderVerificationMessageIds.message)
            }
        }
    }

    private var message: String? {
        guard let state else { return nil }
        if state.saveFailed {
            return feedFlowStrings.readerContentSaveFailed
        }
        if state.failed {
            return feedFlowStrings.feedVerificationFailed
        }
        return nil
    }
}
