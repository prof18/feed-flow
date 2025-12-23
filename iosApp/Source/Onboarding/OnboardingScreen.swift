import SwiftUI
import FeedFlowKit

struct OnboardingScreen: View {
    @StateObject private var vmStoreOwner = VMStoreOwner<OnboardingViewModel>()
    var onOnboardingComplete: () -> Void

    var body: some View {
        if let viewModel = vmStoreOwner.viewModel {
            OnboardingScreenContent(
                viewModel: viewModel,
                onOnboardingComplete: onOnboardingComplete
            )
        }
    }
}
