import SwiftUI

@available(iOS 27.1, *)
struct HomeAdaptiveCompactToolbarModifier<Horizontal: ToolbarContent, Vertical: ToolbarContent>: ViewModifier {
    @Environment(\.toolbarVerticalEdge)
    private var toolbarVerticalEdge

    let title: String
    let horizontalToolbar: Horizontal
    let verticalToolbar: Vertical

    func body(content: Content) -> some View {
        content
            .navigationTitle(toolbarVerticalEdge == nil ? "" : title)
            .toolbar {
                if toolbarVerticalEdge == nil {
                    horizontalToolbar
                } else {
                    verticalToolbar
                }
            }
    }
}
