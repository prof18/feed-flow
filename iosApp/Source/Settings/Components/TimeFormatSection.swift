import FeedFlowKit
import SwiftUI

struct TimeFormatSection: View {
    @Binding var timeFormat: TimeFormat

    var body: some View {
        Picker(selection: $timeFormat) {
            Text(feedFlowStrings.timeFormatHours24)
                .tag(TimeFormat.hours24)
            Text(feedFlowStrings.timeFormatHours12)
                .tag(TimeFormat.hours12)
        } label: {
            Label(feedFlowStrings.timeFormatTitle, systemImage: "clock")
        }
    }
}
