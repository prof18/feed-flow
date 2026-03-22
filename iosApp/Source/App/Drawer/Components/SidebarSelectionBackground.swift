import SwiftUI

@ViewBuilder
func sidebarSelectionBackground(isSelected: Bool, isCompact: Bool = false) -> some View {
    if isSelected && isCompact {
        Color(.systemGray4)
    } else if isSelected {
        RoundedRectangle(cornerRadius: 28)
            .fill(Color(.systemGray4))
    } else if isCompact {
        Color(.secondarySystemGroupedBackground)
    } else {
        Color.clear
    }
}
