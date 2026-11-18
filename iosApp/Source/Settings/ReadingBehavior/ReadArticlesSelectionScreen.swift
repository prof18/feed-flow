import FeedFlowKit
import SwiftUI

struct ReadArticlesSelectionScreen: View {
    @Environment(\.dismiss) private var dismiss

    @Binding var readArticlesVisibility: ReadArticlesVisibility
    let isMarkReadWhenScrollingEnabled: Bool

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        Form {
            Section {
                selectionRow(
                    title: feedFlowStrings.settingsReadArticlesKeep,
                    subtitle: nil,
                    visibility: .keepInList,
                    accessibilityIdentifier: ReadingBehaviorAccessibilityIdentifiers.readArticlesKeepOption
                )
                selectionRow(
                    title: feedFlowStrings.settingsReadArticlesHideOnRefresh,
                    subtitle: nil,
                    visibility: .hideOnRefresh,
                    accessibilityIdentifier: ReadingBehaviorAccessibilityIdentifiers.readArticlesHideOnRefreshOption
                )
                selectionRow(
                    title: feedFlowStrings.settingsReadArticlesHideRightAway,
                    subtitle: isMarkReadWhenScrollingEnabled
                        ? feedFlowStrings.settingsReadArticlesRightAwayDisabled
                        : nil,
                    visibility: .hideRightAway,
                    accessibilityIdentifier: ReadingBehaviorAccessibilityIdentifiers.readArticlesHideRightAwayOption
                )
                .disabled(isMarkReadWhenScrollingEnabled)
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
        .navigationTitle(Text(feedFlowStrings.settingsReadArticlesTitle))
        .navigationBarTitleDisplayMode(.inline)
    }

    private func selectionRow(
        title: String,
        subtitle: String?,
        visibility: ReadArticlesVisibility,
        accessibilityIdentifier: String
    ) -> some View {
        let isSelected = readArticlesVisibility == visibility
        return Button {
            readArticlesVisibility = visibility
            dismiss()
        } label: {
            ReadArticlesSelectionRowLabel(title: title, subtitle: subtitle, isSelected: isSelected)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(accessibilityIdentifier)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}
