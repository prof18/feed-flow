import SwiftUI

struct SnackbarPresentationSurfaceModifier: ViewModifier {
    @Environment(AppState.self)
    private var appState
    @Environment(\.snackbarSurfaceDepth)
    private var parentDepth
    @State private var surface = UUID()

    func body(content: Content) -> some View {
        content
            .environment(\.snackbarSurfaceDepth, parentDepth + 1)
            .overlay(alignment: .bottom) {
                SnackbarOverlay(surface: surface)
            }
            .onAppear {
                appState.snackbarPresentation.register(surface, depth: parentDepth + 1)
            }
            .onDisappear {
                appState.snackbarPresentation.unregister(surface)
            }
    }
}

extension View {
    func snackbarPresentationSurface() -> some View {
        modifier(SnackbarPresentationSurfaceModifier())
    }
}

private extension EnvironmentValues {
    @Entry var snackbarSurfaceDepth = 0
}
