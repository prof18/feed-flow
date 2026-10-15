import SwiftUI

struct FeedFlowScene<Content: View>: View {
    @State private var appState: AppState

    @ViewBuilder let content: (AppState) -> Content

    init(
        appearance: AppAppearance,
        @ViewBuilder content: @escaping (AppState) -> Content
    ) {
        _appState = State(initialValue: AppState(appearance: appearance))
        self.content = content
    }

    var body: some View {
        content(appState)
            .environment(appState)
            .preferredColorScheme(appState.colorScheme)
    }
}
