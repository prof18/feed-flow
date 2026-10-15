import SwiftUI

struct SidebarRowSelectionModifier: ViewModifier {
    let isSelected: Bool
    let isCompact: Bool

    func body(content: Content) -> some View {
        content
            // NavigationStack clears native row selection when returning to the phone drawer.
            .listRowBackground(isCompact && isSelected ? Color(uiColor: .systemGray4) : nil)
            .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}
