import SwiftUI

struct SnackbarOverlay: View {
    @Environment(AppState.self)
    private var appState

    let surface: UUID?

    var body: some View {
        let presentation = appState.snackbarPresentation
        if presentation.frontmostSurface == surface,
           let message = appState.snackbarQueue.first,
           presentation.messageID == message.id {
            Snackbar(message: message, isExiting: presentation.isExiting) {
                _ = presentation.beginDismissal(message.id)
            }
            .id(message.id)
        }
    }
}
