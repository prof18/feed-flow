import FeedFlowKit
import RevenueCat
import RevenueCatUI
import SwiftUI

struct SupportPaywallScreen: View {
    @Environment(\.dismiss)
    private var dismiss
    @State private var isShowingPurchaseThankYou = false
    @State private var paywallState: SupportPaywallState = .loading

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        Group {
            switch paywallState {
            case .loading:
                ProgressView()
                    .controlSize(.large)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case let .ready(offering):
                PaywallView(offering: offering)
                    .onPurchaseCompleted { _ in
                        isShowingPurchaseThankYou = true
                    }
            case .fallback:
                PaywallView()
                    .onPurchaseCompleted { _ in
                        isShowingPurchaseThankYou = true
                    }
            }
        }
        .navigationTitle(Text(feedFlowStrings.supportPaywallTitle))
        .navigationBarTitleDisplayMode(.inline)
        .alert(
            feedFlowStrings.supportPurchaseThankYouTitle,
            isPresented: $isShowingPurchaseThankYou
        ) {
            Button(feedFlowStrings.actionDone) {
                dismiss()
            }
        } message: {
            Text(feedFlowStrings.supportPurchaseThankYouMessage)
        }
        .task {
            do {
                let offerings = try await Purchases.shared.offerings()
                guard !Task.isCancelled else {
                    return
                }
                paywallState = offerings.current
                    .map(SupportPaywallState.ready)
                    ?? .fallback
            } catch is CancellationError {
                return
            } catch {
                guard !Task.isCancelled else {
                    return
                }
                paywallState = .fallback
            }
        }
    }
}

private enum SupportPaywallState {
    case loading
    case ready(Offering)
    case fallback
}
