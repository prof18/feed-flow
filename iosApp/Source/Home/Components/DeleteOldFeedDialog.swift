import SwiftUI

struct DeleteOldFeedDialog: ViewModifier {
  let isLoading: Bool
  let message: String

  func body(content: Content) -> some View {
    ZStack {
      content

      if isLoading {
        ZStack {
          Color.black.opacity(0.3)
            .ignoresSafeArea()
            .allowsHitTesting(true)

          VStack(spacing: Spacing.regular) {
            ProgressView()
              .scaleEffect(1.5)

            Text(message)
              .multilineTextAlignment(.center)
          }
          .padding(Spacing.medium)
          .background {
            RoundedRectangle(cornerRadius: 12)
              .fill(.ultraThinMaterial)
          }
          .padding(Spacing.medium)
        }
        .transition(.opacity)
      }
    }
  }
}

extension View {
  func deletingFeedDialog(isLoading: Bool, message: String) -> some View {
    modifier(DeleteOldFeedDialog(isLoading: isLoading, message: message))
  }
}
