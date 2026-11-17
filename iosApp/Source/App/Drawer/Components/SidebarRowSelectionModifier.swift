import SwiftUI

struct SidebarRowSelectionModifier: ViewModifier {
    let isSelected: Bool
    let isCompact: Bool

    func body(content: Content) -> some View {
        content
            // Native List selection takes the tap from the row buttons that apply the filter, and NavigationStack
            // clears it when returning to the phone drawer, so the selection is drawn here.
            .listRowBackground(rowBackground)
            // The phone drawer's separators would cut through the selection capsule.
            .listRowSeparator(isCompact ? .hidden : .automatic)
            .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    @ViewBuilder private var rowBackground: some View {
        if isSelected {
            Capsule()
                .fill(Color.sidebarSelection)
                .overlay(Capsule().strokeBorder(Color.sidebarSelectionBorder, lineWidth: 1))
        }
    }
}
