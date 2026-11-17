import FeedFlowKit
import SwiftUI

struct AutoDeletePeriodSelectionScreen: View {
    @Environment(\.dismiss) private var dismiss

    @Binding var autoDeletePeriod: AutoDeletePeriod

    private let feedFlowStrings = Deps.shared.getStrings()

    private let periods: [AutoDeletePeriod] = [.disabled, .oneDay, .oneWeek, .twoWeeks, .oneMonth]

    var body: some View {
        Form {
            Section {
                ForEach(periods, id: \.self) { period in
                    selectionRow(period)
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
        .navigationTitle(Text(feedFlowStrings.settingsAutoDelete))
        .navigationBarTitleDisplayMode(.inline)
    }

    static func label(for period: AutoDeletePeriod, strings: FeedFlowStrings) -> String {
        switch period {
        case .oneDay:
            strings.settingsAutoDeletePeriodOneDay
        case .oneWeek:
            strings.settingsAutoDeletePeriodOneWeek
        case .twoWeeks:
            strings.settingsAutoDeletePeriodTwoWeeks
        case .oneMonth:
            strings.settingsAutoDeletePeriodOneMonth
        default:
            strings.settingsAutoDeletePeriodDisabled
        }
    }

    private func selectionRow(_ period: AutoDeletePeriod) -> some View {
        let isSelected = autoDeletePeriod == period
        return Button {
            autoDeletePeriod = period
            dismiss()
        } label: {
            HStack {
                Text(Self.label(for: period, strings: feedFlowStrings))

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
