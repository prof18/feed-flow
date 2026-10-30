import SwiftUI

/// Kept in its own view so its body only observes `selectedPlaybackSpeed`. When it lived in the
/// player body, every one-second `position` tick re-evaluated the `Menu` and SwiftUI rebuilt the
/// visible context menu, cancelling a tap that landed on an item during the rebuild.
struct AudioPlaybackSpeedMenu: View {
    @Environment(AudioPlaybackController.self)
    private var playback

    var body: some View {
        Menu {
            ForEach(AudioPlaybackController.supportedPlaybackSpeeds, id: \.self) { speed in
                Button {
                    playback.setPlaybackSpeed(speed)
                } label: {
                    if playback.selectedPlaybackSpeed == speed {
                        Label(speedValueLabel(speed), systemImage: "checkmark")
                    } else {
                        Text(speedValueLabel(speed))
                    }
                }
                .accessibilityIdentifier(AudioPlayerAccessibilityIdentifiers.speedOption(speed))
            }
        } label: {
            Text(speedValueLabel(playback.selectedPlaybackSpeed))
                .font(.caption.weight(.semibold))
                .frame(minWidth: 44, minHeight: 44)
                .contentShape(Rectangle())
        }
        .accessibilityLabel(feedFlowStrings.audioPlaybackSpeed)
        .accessibilityValue(speedValueLabel(playback.selectedPlaybackSpeed))
        .accessibilityIdentifier(AudioPlayerAccessibilityIdentifiers.speed)
    }

    private func speedValueLabel(_ speed: Double) -> String {
        "\(speed.formatted(.number.precision(.fractionLength(0 ... 1))))×"
    }
}
