import FeedFlowKit
import SwiftUI

struct DateFormatSection: View {
    @Binding var dateFormat: DateFormat

    var body: some View {
        Picker(selection: $dateFormat) {
            Text(feedFlowStrings.dateFormatNormal)
                .tag(DateFormat.normal)
            Text(feedFlowStrings.dateFormatAmerican)
                .tag(DateFormat.american)
        } label: {
            Label(feedFlowStrings.dateFormatTitle, systemImage: "calendar")
        }
    }
}
