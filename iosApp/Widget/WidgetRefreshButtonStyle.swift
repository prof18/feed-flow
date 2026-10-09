import SwiftUI

struct WidgetRefreshButtonStyle: ToggleStyle {
    let refreshingLabel: String

    func makeBody(configuration: Configuration) -> some View {
        Button {
            configuration.isOn.toggle()
        } label: {
            ZStack(alignment: .trailing) {
                HStack(spacing: 4) {
                    Text(refreshingLabel)
                        .font(.caption)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                    Image(systemName: "hourglass")
                        .font(.subheadline)
                }
                .opacity(configuration.isOn ? 1 : 0)
                .accessibilityHidden(!configuration.isOn)

                configuration.label
                    .opacity(configuration.isOn ? 0 : 1)
                    .accessibilityHidden(configuration.isOn)
            }
            .frame(minWidth: 32, minHeight: 32)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(configuration.isOn)
    }
}
