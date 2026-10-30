import SwiftUI

@available(iOS 27.1, *)
struct HomeAdaptiveToolbar<Search: View, More: View>: ToolbarContent {
    @Environment(\.toolbarVerticalEdge)
    private var toolbarVerticalEdge

    let search: Search
    let more: More

    var body: some ToolbarContent {
        if toolbarVerticalEdge != nil {
            ToolbarItemGroup(placement: .bottomBar) {
                search
                more
            }
        } else {
            ToolbarSpacer(.fixed)
            ToolbarItem {
                search
            }
            ToolbarSpacer(.fixed)
            ToolbarItem(placement: .primaryAction) {
                more
            }
        }
    }
}
