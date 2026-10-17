import SwiftUI

@available(iOS 27.1, *)
struct ReaderVerticalSafeAreaModifier: ViewModifier {
    @Environment(\.toolbarVerticalEdge)
    private var toolbarVerticalEdge

    func body(content: Content) -> some View {
        content.ignoresSafeArea(.container, edges: toolbarVerticalEdge == nil ? [] : .top)
    }
}
