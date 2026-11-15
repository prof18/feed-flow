import FeedFlowKit
import SwiftUI

struct LinksBrowserSelectionScreen: View {
    @Environment(\.dismiss)
    private var dismiss

    @Bindable var browserSelector: BrowserSelector

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        Form {
            Section {
                ForEach(browserSelector.browsers, id: \.id) { browser in
                    browserRow(browser)
                }
            } footer: {
                Text(feedFlowStrings.linksBrowserSubtitle)
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
        .navigationTitle(Text(feedFlowStrings.linksBrowserTitle))
        .navigationBarTitleDisplayMode(.inline)
    }

    private func browserRow(_ browser: Browser) -> some View {
        let isSelected = browserSelector.selectedBrowser?.id == browser.id
        return Button {
            browserSelector.selectedBrowser = browser
            dismiss()
        } label: {
            HStack {
                Text(browser.name)

                Spacer()

                if isSelected {
                    Image(systemName: "checkmark")
                }
            }
            .contentShape(.rect)
            .foregroundStyle(.primary)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}
