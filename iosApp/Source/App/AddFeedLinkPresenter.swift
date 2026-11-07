import SwiftUI

/// Opens the Add feed sheet for an incoming add-feed link. SwiftUI cannot present a sheet over another one, so
/// it first closes the open sheets, waits for them to finish dismissing, then presents Add feed pre-filled.
struct AddFeedLinkPresenter: ViewModifier {
    @Environment(AppState.self)
    private var appState

    @Binding var addFeedSheet: AddFeedSheetRequest?
    let closeOtherSheets: () -> Void

    func body(content: Content) -> some View {
        content
            .task(id: appState.addFeedLinkRevision) {
                guard appState.pendingAddFeedUrl != nil else { return }
                addFeedSheet = nil
                closeOtherSheets()
                try? await Task.sleep(for: .milliseconds(600))
                guard !Task.isCancelled, let feedUrl = appState.pendingAddFeedUrl else { return }
                appState.pendingAddFeedUrl = nil
                addFeedSheet = AddFeedSheetRequest(initialUrl: feedUrl)
            }
    }
}
